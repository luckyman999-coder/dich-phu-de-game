import SwiftUI

struct SettingsView: View {
    @ObservedObject var prefs = Prefs.shared
    @Environment(\.dismiss) private var dismiss
    @State private var k1 = ""
    @State private var k2 = ""
    @State private var k3 = ""
    @State private var testResult = ""

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    SecureField("Mã Gemini 1", text: $k1)
                    SecureField("Mã Gemini 2", text: $k2)
                    SecureField("Mã Gemini 3", text: $k3)
                    Link("Lấy mã Gemini (Google AI Studio)", destination: URL(string: "https://aistudio.google.com/apikey")!)
                    SecureField("Mã Groq (dự phòng)", text: $prefs.groqKey)
                    Link("Lấy mã Groq", destination: URL(string: "https://console.groq.com/keys")!)
                    Button("Lưu và dịch thử") { save(); test() }
                    if !testResult.isEmpty { Text(testResult).font(.footnote) }
                } header: {
                    Text("1. Mã dịch miễn phí")
                } footer: {
                    Text("Gemini tối đa 3 mã, hết lượt mã này tự chuyển mã khác. Groq dùng khi cả 3 mã Gemini hết lượt.")
                }

                Section("2. Game đang chơi") {
                    Menu("Chọn game có sẵn ghi chú") {
                        ForEach(GamePresets.all) { p in
                            Button(p.name) { prefs.gameName = p.name; prefs.gameNotes = p.notes }
                        }
                    }
                    TextField("Tên game", text: $prefs.gameName)
                    TextField("Ghi chú: tên nhân vật, cách xưng hô…", text: $prefs.gameNotes, axis: .vertical)
                        .lineLimit(3...8)
                }

                Section("3. Hiển thị và giọng đọc") {
                    Toggle("Đọc to phụ đề tiếng Việt", isOn: $prefs.speak)
                    Toggle("Hiện cả câu tiếng Anh gốc", isOn: $prefs.showOriginal)
                    VStack(alignment: .leading) {
                        Text("Tốc độ đọc")
                        Slider(value: $prefs.speechRate, in: 0.35...0.65)
                    }
                }

                Section("Nâng cao") {
                    TextField("Model Gemini", text: $prefs.geminiModel)
                        .autocorrectionDisabled().textInputAutocapitalization(.never)
                    TextField("Model Groq", text: $prefs.groqModel)
                        .autocorrectionDisabled().textInputAutocapitalization(.never)
                }
            }
            .navigationTitle("Cài đặt dịch")
            .toolbar {
                ToolbarItem(placement: .confirmationAction) { Button("Xong") { save(); dismiss() } }
            }
            .onAppear {
                let ks = prefs.geminiKeys
                k1 = ks.count > 0 ? ks[0] : ""
                k2 = ks.count > 1 ? ks[1] : ""
                k3 = ks.count > 2 ? ks[2] : ""
            }
        }
    }

    private func save() {
        prefs.geminiKeys = [k1, k2, k3].map { $0.trimmingCharacters(in: .whitespaces) }.filter { !$0.isEmpty }
    }

    private func test() {
        guard prefs.hasAnyKey else { testResult = "Chưa nhập mã nào."; return }
        testResult = "Đang dịch thử…"
        let tr = Translator(prefs: prefs)
        Task {
            let t = await tr.translate("We'll find a way. Together.")
            let err = await tr.lastError
            testResult = t.map { "✅ Dịch được: \($0.vietnamese)" } ?? "❌ Lỗi: \(err ?? "không rõ") – kiểm tra lại mã."
        }
    }
}
