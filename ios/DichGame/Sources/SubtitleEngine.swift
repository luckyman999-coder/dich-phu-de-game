import Foundation
import Vision
import CoreGraphics

/// Chụp khung hình → OCR vùng phụ đề (Apple Vision, chạy trên máy) → dịch → hiện + đọc.
/// Logic giống SubtitlePipeline bên Android.
@MainActor
final class SubtitleEngine: ObservableObject {
    @Published var current: Translation?
    @Published var status: String?

    private let prefs: Prefs
    private let translator: Translator
    private let speaker: Speaker
    private let frameSource: () async -> CGImage?
    private let region: () -> CGRect
    private let fallback: ((String) -> Translation?)?

    private var loop: Task<Void, Never>?
    private var translateTask: Task<Void, Never>?
    private var pendingText = ""
    private var pendingCount = 0
    private var shownText = ""
    private var emptyCount = 0

    private static let viChars = Set("ăâđêôơưạảấầẩẫậắằẳẵặẹẻẽếềểễệỉịọỏốồổỗộớờởỡợụủứừửữựỳỵỷỹ")

    init(prefs: Prefs, translator: Translator, speaker: Speaker,
         region: @escaping () -> CGRect,
         fallback: ((String) -> Translation?)? = nil,
         frameSource: @escaping () async -> CGImage?) {
        self.prefs = prefs
        self.translator = translator
        self.speaker = speaker
        self.region = region
        self.fallback = fallback
        self.frameSource = frameSource
    }

    func start() {
        guard loop == nil else { return }
        loop = Task { [weak self] in
            while !Task.isCancelled {
                await self?.tick()
                try? await Task.sleep(nanoseconds: 300_000_000)
            }
        }
    }

    func stop() {
        loop?.cancel(); loop = nil
        translateTask?.cancel()
        reset()
        speaker.stop()
    }

    func reset() {
        pendingText = ""; pendingCount = 0; shownText = ""; emptyCount = 0
    }

    private func tick() async {
        guard let frame = await frameSource() else { return }
        let r = region()
        let w = CGFloat(frame.width), h = CGFloat(frame.height)
        let rect = CGRect(x: r.minX * w, y: r.minY * h, width: r.width * w, height: r.height * h).integral
        guard rect.width >= 8, rect.height >= 8, let crop = frame.cropping(to: rect) else { return }
        let text = await Self.ocr(crop)
        handle(text)
    }

    nonisolated private static func ocr(_ image: CGImage) async -> String {
        await withCheckedContinuation { cont in
            let req = VNRecognizeTextRequest { req, _ in
                let obs = (req.results as? [VNRecognizedTextObservation]) ?? []
                let lines = obs
                    .sorted { $0.boundingBox.maxY > $1.boundingBox.maxY } // Vision: gốc toạ độ ở dưới
                    .compactMap { $0.topCandidates(1).first?.string.trimmingCharacters(in: .whitespaces) }
                    .filter { !$0.isEmpty }
                cont.resume(returning: lines.joined(separator: "\n"))
            }
            req.recognitionLevel = .accurate
            req.usesLanguageCorrection = true
            req.recognitionLanguages = ["en-US", "vi-VT"]
            do {
                try VNImageRequestHandler(cgImage: image, options: [:]).perform([req])
            } catch {
                cont.resume(returning: "")
            }
        }
    }

    private func handle(_ raw: String) {
        let text = clean(raw)
        if text.isEmpty {
            emptyCount += 1
            pendingCount = 0; pendingText = ""
            if emptyCount == 3 && !shownText.isEmpty {
                shownText = ""
                translateTask?.cancel()
                current = nil
            }
            return
        }
        emptyCount = 0
        if similar(text, pendingText) { pendingCount += 1 } else { pendingText = text; pendingCount = 1 }
        guard pendingCount == 2, !similar(text, shownText) else { return }
        shownText = text
        process(text)
    }

    private func process(_ text: String) {
        translateTask?.cancel()
        translateTask = Task { [weak self] in
            guard let self else { return }
            var t: Translation?
            if Self.isVietnamese(text) {
                t = Translation(original: text, vietnamese: text, speaker: "", gender: .unknown)
            } else if !prefs.hasAnyKey {
                t = fallback?(text)
                if t == nil { status = "Chưa có mã dịch – vào Cài đặt để thêm mã Gemini/Groq"; return }
            } else {
                t = await translator.translate(text)
                if t == nil { t = fallback?(text) }
            }
            if Task.isCancelled { return }
            if let tt = t, let g = GameBook.shared.gender(forSpeaker: tt.speaker) {
                t = Translation(original: tt.original, vietnamese: tt.vietnamese, speaker: tt.speaker, gender: g)
            }
            if let t {
                status = nil
                current = t
                speaker.speak(t)
            } else {
                let err = await translator.lastError
                status = err ?? "Dịch thất bại"
            }
        }
    }

    private func clean(_ raw: String) -> String {
        let lines = raw.split(separator: "\n")
            .map { $0.split(whereSeparator: { $0.isWhitespace }).joined(separator: " ") }
            .filter { $0.filter(\.isLetter).count >= 2 }
        let t = lines.joined(separator: "\n")
        return t.filter(\.isLetter).count < 3 ? "" : t
    }

    private func key(_ s: String) -> [Character] { Array(s.lowercased().filter { $0.isLetter || $0.isNumber }) }

    private func similar(_ a: String, _ b: String) -> Bool {
        let ka = key(a), kb = key(b)
        if ka == kb { return true }
        if ka.isEmpty || kb.isEmpty { return false }
        let longer = max(ka.count, kb.count)
        if abs(ka.count - kb.count) > longer / 8 + 1 { return false }
        return 1 - Double(levenshtein(ka, kb)) / Double(longer) >= 0.9
    }

    private func levenshtein(_ a: [Character], _ b: [Character]) -> Int {
        var prev = Array(0...b.count)
        var cur = Array(repeating: 0, count: b.count + 1)
        for i in 1...a.count {
            cur[0] = i
            for j in 1...b.count {
                cur[j] = min(cur[j - 1] + 1, prev[j] + 1, prev[j - 1] + (a[i - 1] == b[j - 1] ? 0 : 1))
            }
            swap(&prev, &cur)
        }
        return prev[b.count]
    }

    private static func isVietnamese(_ text: String) -> Bool {
        let letters = text.filter(\.isLetter).count
        guard letters > 0 else { return false }
        let vi = text.lowercased().filter { viChars.contains($0) }.count
        return Double(vi) / Double(letters) > 0.06
    }
}
