import Foundation

enum Gender { case male, female, unknown }

struct Translation: Equatable {
    let original: String
    let vietnamese: String
    let speaker: String
    let gender: Gender
}

/// Dịch bằng Gemini (xoay vòng nhiều mã), Groq dự phòng. Cùng prompt với bản Android.
actor Translator {
    private let prefs: Prefs
    private var cache: [String: Translation] = [:]
    private var history: [(String, String)] = []
    private var blockedUntil: [String: Date] = [:]
    private var keyIndex = 0
    private(set) var lastError: String?

    init(prefs: Prefs) { self.prefs = prefs }

    func translate(_ text: String) async -> Translation? {
        if let c = cache[text] { return c }
        let snapshot = await MainActor.run { (prefs.cleanGeminiKeys, prefs.groqKey, prefs.geminiModel, prefs.groqModel, prefs.gameName, prefs.gameNotes) }
        let prompt = buildPrompt(text, game: snapshot.4, notes: snapshot.5)
        var raw = await tryGemini(prompt, keys: snapshot.0, model: snapshot.2)
        if raw == nil { raw = await tryGroq(prompt, key: snapshot.1, model: snapshot.3) }
        guard let r = raw, let t = parse(text, r) else { return nil }
        cache[text] = t
        history.append((text, t.vietnamese))
        if history.count > 6 { history.removeFirst() }
        lastError = nil
        return t
    }

    private func buildPrompt(_ text: String, game: String, notes: String) -> String {
        var s = "Bạn là dịch giả phụ đề game chuyên nghiệp. Dịch câu thoại tiếng Anh sau sang tiếng Việt tự nhiên, đúng giọng nhân vật và văn phong của game, ngắn gọn như phụ đề. "
        s += "Giữ nguyên tên riêng (nhân vật, địa danh, kỹ năng) trừ khi ghi chú yêu cầu khác. "
        s += "Chọn cách xưng hô tiếng Việt (anh/em, tôi/cậu, ta/ngươi...) phù hợp quan hệ nhân vật và giữ nhất quán với các câu trước.\n"
        if !game.isEmpty { s += "Game: \(game)\n" }
        if !notes.isEmpty { s += "Ghi chú về game (bắt buộc tuân theo):\n\(notes)\n" }
        if !history.isEmpty {
            s += "Các câu vừa dịch trước đó (để giữ mạch hội thoại):\n"
            for (en, vi) in history { s += "- \(en) => \(vi)\n" }
        }
        s += "Văn bản do OCR đọc từ màn hình nên có thể sai vài ký tự; hãy hiểu theo nghĩa hợp lý nhất. "
        s += "Nếu dòng đầu có dạng \"TÊN: lời thoại\" hoặc dòng riêng chỉ có TÊN viết hoa thì đó là người nói, không dịch tên đó.\n"
        s += "Trả về DUY NHẤT một JSON: {\"speaker\": \"tên người nói hoặc rỗng\", \"gender\": \"male\" | \"female\" | \"unknown\", \"vi\": \"bản dịch tiếng Việt\"}\n\n"
        s += "Văn bản:\n\(text)"
        return s
    }

    private func tryGemini(_ prompt: String, keys: [String], model: String) async -> String? {
        guard !keys.isEmpty else { return nil }
        let now = Date()
        for i in 0..<keys.count {
            let idx = (keyIndex + i) % keys.count
            let key = keys[idx]
            if let until = blockedUntil[key], until > now { continue }
            let body: [String: Any] = [
                "contents": [["role": "user", "parts": [["text": prompt]]]],
                "generationConfig": ["temperature": 0.3, "responseMimeType": "application/json"]
            ]
            let m = model.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? model
            guard let url = URL(string: "https://generativelanguage.googleapis.com/v1beta/models/\(m):generateContent") else { return nil }
            do {
                let json = try await post(url, body: body, headers: ["x-goog-api-key": key])
                keyIndex = idx
                if let cands = json["candidates"] as? [[String: Any]],
                   let content = cands.first?["content"] as? [String: Any],
                   let parts = content["parts"] as? [[String: Any]],
                   let text = parts.first?["text"] as? String {
                    return text
                }
            } catch let HttpError.status(code, _) {
                lastError = "Gemini lỗi \(code)" + (code == 429 ? " (hết lượt)" : "")
                if [400, 401, 403, 429].contains(code) { blockedUntil[key] = now.addingTimeInterval(60) }
            } catch {
                lastError = "Gemini: \(error.localizedDescription)"
            }
        }
        return nil
    }

    private func tryGroq(_ prompt: String, key: String, model: String) async -> String? {
        let k = key.trimmingCharacters(in: .whitespaces)
        guard !k.isEmpty, let url = URL(string: "https://api.groq.com/openai/v1/chat/completions") else { return nil }
        var body: [String: Any] = [
            "model": model,
            "temperature": 0.3,
            "response_format": ["type": "json_object"],
            "messages": [["role": "user", "content": prompt]]
        ]
        if model.hasPrefix("openai/gpt-oss") { body["reasoning_effort"] = "low" }
        do {
            let json = try await post(url, body: body, headers: ["Authorization": "Bearer \(k)"])
            if let choices = json["choices"] as? [[String: Any]],
               let msg = choices.first?["message"] as? [String: Any],
               let content = msg["content"] as? String {
                return content
            }
        } catch let HttpError.status(code, _) {
            lastError = "Groq lỗi \(code)"
        } catch {
            lastError = "Groq: \(error.localizedDescription)"
        }
        return nil
    }

    private func parse(_ original: String, _ raw: String) -> Translation? {
        if let s = raw.firstIndex(of: "{"), let e = raw.lastIndex(of: "}"),
           let data = String(raw[s...e]).data(using: .utf8),
           let o = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
            let vi = (o["vi"] as? String ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
            guard !vi.isEmpty else { return nil }
            let g: Gender
            switch (o["gender"] as? String ?? "").lowercased() {
            case "male": g = .male
            case "female": g = .female
            default: g = .unknown
            }
            return Translation(original: original, vietnamese: vi,
                               speaker: (o["speaker"] as? String ?? "").trimmingCharacters(in: .whitespaces), gender: g)
        }
        let t = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        return t.isEmpty ? nil : Translation(original: original, vietnamese: t, speaker: "", gender: .unknown)
    }

    enum HttpError: Error { case status(Int, String) }

    private func post(_ url: URL, body: [String: Any], headers: [String: String]) async throws -> [String: Any] {
        var req = URLRequest(url: url, timeoutInterval: 15)
        req.httpMethod = "POST"
        req.setValue("application/json; charset=utf-8", forHTTPHeaderField: "Content-Type")
        for (k, v) in headers { req.setValue(v, forHTTPHeaderField: k) }
        req.httpBody = try JSONSerialization.data(withJSONObject: body)
        let (data, resp) = try await URLSession.shared.data(for: req)
        let code = (resp as? HTTPURLResponse)?.statusCode ?? 0
        guard (200..<300).contains(code) else {
            throw HttpError.status(code, String(data: data, encoding: .utf8)?.prefix(300).description ?? "")
        }
        return (try JSONSerialization.jsonObject(with: data) as? [String: Any]) ?? [:]
    }
}
