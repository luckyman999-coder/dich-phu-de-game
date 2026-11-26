import SwiftUI
import UniformTypeIdentifiers

/// Hướng dẫn lấy mã Gemini / Groq từng bước (giống KeyGuideActivity bên Android)
struct KeyGuideView: View {
    @ObservedObject var prefs = Prefs.shared
    @Environment(\.dismiss) private var dismiss
    @State private var provider: Provider = .gemini
    @State private var manual = ""
    @State private var result: String?

    enum Provider: String, CaseIterable, Identifiable {
        case gemini, groq
        var id: String { rawValue }
        var title: String { self == .gemini ? "Gemini (Google)" : "Groq" }
        var url: URL { URL(string: self == .gemini ? "https://aistudio.google.com/apikey" : "https://console.groq.com/keys")! }
        var prefix: String { self == .gemini ? "AIza" : "gsk_" }
        var steps: [(String, String)] {
            switch self {
            case .gemini: return [
                ("Mở trang Google AI Studio", "Bấm nút bên dưới. Đăng nhập bằng tài khoản Google (Gmail) bất kỳ."),
                ("Tạo mã", "Bấm \"Create API key\" (Tạo khoá API). Lần đầu có thể phải đồng ý điều khoản."),
                ("Copy mã", "Bấm biểu tượng copy cạnh mã vừa tạo. Mã bắt đầu bằng AIza…"),
                ("Quay lại Dịch Game", "Bấm nút \"Dán\" bên dưới, app tự lưu và dịch thử."),
            ]
            case .groq: return [
                ("Mở trang Groq", "Bấm nút bên dưới, đăng nhập bằng Google hoặc email."),
                ("Tạo mã", "Bấm \"Create API Key\", đặt tên bất kỳ (ví dụ: dichgame) rồi bấm Submit."),
                ("Copy mã", "Copy mã hiện ra (chỉ hiện 1 lần). Mã bắt đầu bằng gsk_…"),
                ("Quay lại Dịch Game", "Bấm nút \"Dán\" bên dưới. Groq được dùng khi các mã Gemini hết lượt."),
            ]
            }
        }
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    Text("Khoảng 2 phút. Mã miễn phí, không cần thẻ thanh toán.").font(.subheadline).foregroundColor(.dgMuted)
                    Text("Gemini: \(prefs.cleanGeminiKeys.count)/3 mã   ·   Groq: \(prefs.groqKey.isEmpty ? "chưa có" : "đã có")")
                        .font(.subheadline).foregroundColor(prefs.hasAnyKey ? .dgAccent : .dgAmber)

                    Picker("", selection: $provider) {
                        ForEach(Provider.allCases) { Text($0.title).tag($0) }
                    }
                    .pickerStyle(.segmented)

                    ForEach(Array(provider.steps.enumerated()), id: \.offset) { i, step in
                        HStack(alignment: .top, spacing: 12) {
                            Text("\(i + 1)").font(.subheadline.bold()).foregroundColor(.dgBg)
                                .frame(width: 28, height: 28).background(Circle().fill(Color.dgAccent))
                            VStack(alignment: .leading, spacing: 4) {
                                Text(step.0).font(.headline).foregroundColor(.dgText)
                                Text(step.1).font(.subheadline).foregroundColor(.dgMuted)
                                if i == 0 {
                                    Link(destination: provider.url) {
                                        Text("Mở \(provider.title)").font(.headline).foregroundColor(.dgBg)
                                            .frame(maxWidth: .infinity).padding(.vertical, 11)
                                            .background(RoundedRectangle(cornerRadius: 12).fill(Color.dgAccent))
                                    }
                                    .padding(.top, 6)
                                }
                                if i == 3 {
                                    PasteButton(payloadType: String.self) { strings in
                                        if let s = strings.first { Task { @MainActor in save(s) } }
                                    }
                                    .padding(.top, 6)
                                }
                            }
                            Spacer(minLength: 0)
                        }
                        .padding(14).cardStyle()
                    }

                    Text("Hoặc gõ / dán mã vào đây").font(.footnote).foregroundColor(.dgMuted).padding(.top, 8)
                    HStack {
                        TextField("\(provider.prefix)…", text: $manual)
                            .textInputAutocapitalization(.never).autocorrectionDisabled()
                            .padding(10).cardStyle()
                        Button("Lưu") { save(manual) }.foregroundColor(.dgAccent)
                    }

                    if let r = result {
                        Text(r).foregroundColor(r.hasPrefix("❌") ? .dgAmber : .dgAccent).padding(.top, 8)
                    }
                    if provider == .gemini {
                        Text("Mẹo: mỗi mã nên tạo ở một dự án (project) hoặc tài khoản Google khác nhau để có thêm lượt dịch miễn phí.")
                            .font(.footnote).foregroundColor(.dgMuted).padding(.top, 8)
                    }
                }
                .padding(20)
            }
            .background(Color.dgBg.ignoresSafeArea())
            .navigationTitle("Lấy mã dịch miễn phí")
            .toolbar { ToolbarItem(placement: .confirmationAction) { Button("Xong") { dismiss() } } }
        }
        .preferredColorScheme(.dark)
    }

    private func save(_ raw: String) {
        let key = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        if key.hasPrefix("AIza") {
            if prefs.cleanGeminiKeys.contains(key) { result = "Mã này đã có rồi."; return }
            if prefs.cleanGeminiKeys.count >= 3 { result = "Đã đủ 3 mã Gemini. Xoá bớt trong Cài đặt nếu muốn thay."; return }
            prefs.geminiKeys = prefs.cleanGeminiKeys + [key]
        } else if key.hasPrefix("gsk_") {
            prefs.groqKey = key
        } else {
            result = "❌ Mã không đúng dạng (Gemini bắt đầu bằng AIza…, Groq bắt đầu bằng gsk_…)."
            return
        }
        manual = ""
        result = "Đã nhận mã, đang dịch thử…"
        let tr = Translator(prefs: prefs)
        Task {
            let t = await tr.translate("Stay close to me. The bridge won't hold for long.")
            let err = await tr.lastError
            result = t.map { "✅ Mã dùng được! Dịch thử: \"\($0.vietnamese)\"" }
                ?? "❌ Mã chưa dùng được: \(err ?? "không rõ lỗi"). Kiểm tra lại đã copy đủ mã chưa."
        }
    }
}
