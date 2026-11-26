import Foundation
import UIKit

/// Cấu hình dùng chung (UserDefaults). Giống DichPrefs bên Android.
final class Prefs: ObservableObject {
    static let shared = Prefs()
    private let d = UserDefaults.standard

    static let defaultGeminiModel = "gemini-flash-lite-latest"
    static let defaultGroqModel = "openai/gpt-oss-120b"

    @Published var geminiKeys: [String] { didSet { d.set(geminiKeys, forKey: "gemini_keys") } }
    @Published var groqKey: String { didSet { d.set(groqKey, forKey: "groq_key") } }
    @Published var geminiModel: String { didSet { d.set(geminiModel, forKey: "gemini_model") } }
    @Published var groqModel: String { didSet { d.set(groqModel, forKey: "groq_model") } }
    @Published var gameName: String { didSet { d.set(gameName, forKey: "game_name") } }
    @Published var gameNotes: String { didSet { d.set(gameNotes, forKey: "game_notes") } }
    @Published var speak: Bool { didSet { d.set(speak, forKey: "speak") } }
    @Published var showOriginal: Bool { didSet { d.set(showOriginal, forKey: "show_original") } }
    @Published var speechRate: Double { didSet { d.set(speechRate, forKey: "speech_rate") } }
    @Published var psnAccountId: String { didSet { d.set(psnAccountId, forKey: "psn_account_id") } }
    /// Vùng phụ đề, toạ độ 0..1
    @Published var region: CGRect { didSet { d.set(NSCoder.string(for: region), forKey: "region") } }

    private init() {
        geminiKeys = d.stringArray(forKey: "gemini_keys") ?? []
        groqKey = d.string(forKey: "groq_key") ?? ""
        geminiModel = d.string(forKey: "gemini_model") ?? Prefs.defaultGeminiModel
        groqModel = d.string(forKey: "groq_model") ?? Prefs.defaultGroqModel
        gameName = d.string(forKey: "game_name") ?? ""
        gameNotes = d.string(forKey: "game_notes") ?? ""
        speak = d.object(forKey: "speak") as? Bool ?? true
        showOriginal = d.object(forKey: "show_original") as? Bool ?? false
        speechRate = d.object(forKey: "speech_rate") as? Double ?? 0.52
        psnAccountId = d.string(forKey: "psn_account_id") ?? ""
        if let s = d.string(forKey: "region") {
            region = NSCoder.cgRect(for: s)
        } else {
            region = CGRect(x: 0.12, y: 0.78, width: 0.76, height: 0.17)
        }
    }

    var cleanGeminiKeys: [String] { geminiKeys.map { $0.trimmingCharacters(in: .whitespaces) }.filter { !$0.isEmpty } }
    var hasAnyKey: Bool { !cleanGeminiKeys.isEmpty || !groqKey.trimmingCharacters(in: .whitespaces).isEmpty }
}
