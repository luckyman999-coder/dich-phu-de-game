import AVFoundation

/// Giọng đọc tiếng Việt: nam giọng trầm, nữ giọng cao, mỗi nhân vật một giọng.
@MainActor
final class Speaker {
    private let synth = AVSpeechSynthesizer()
    private let prefs: Prefs
    private var perSpeaker: [String: (AVSpeechSynthesisVoice?, Float)] = [:]
    private lazy var viVoices: [AVSpeechSynthesisVoice] =
        AVSpeechSynthesisVoice.speechVoices().filter { $0.language.hasPrefix("vi") }.sorted { $0.identifier < $1.identifier }

    init(prefs: Prefs) {
        self.prefs = prefs
        // Phát cùng tiếng game, không tắt nhạc ứng dụng khác
        try? AVAudioSession.sharedInstance().setCategory(.playback, mode: .spokenAudio, options: [.mixWithOthers, .duckOthers])
        try? AVAudioSession.sharedInstance().setActive(true)
    }

    var hasVietnamese: Bool { !viVoices.isEmpty }

    func speak(_ t: Translation) {
        guard prefs.speak else { return }
        let (voice, pitch) = voiceFor(t.speaker, t.gender)
        let u = AVSpeechUtterance(string: t.vietnamese)
        u.voice = voice ?? AVSpeechSynthesisVoice(language: "vi-VN")
        u.pitchMultiplier = pitch
        u.rate = Float(prefs.speechRate)
        if synth.isSpeaking { synth.stopSpeaking(at: .immediate) }
        synth.speak(u)
    }

    func stop() { synth.stopSpeaking(at: .immediate) }

    private func voiceFor(_ speaker: String, _ gender: Gender) -> (AVSpeechSynthesisVoice?, Float) {
        let key = speaker.isEmpty ? "_\(gender)" : speaker.lowercased()
        if let v = perSpeaker[key] { return v }
        var h = 0
        for u in key.unicodeScalars { h = (h &* 31 &+ Int(u.value)) & 0x7fffffff }
        let voice = viVoices.isEmpty ? nil : viVoices[h % viVoices.count]
        let jitter = Float((h / 7) % 5) * 0.03
        let pitch: Float
        switch gender {
        case .male: pitch = 0.8 + jitter
        case .female: pitch = 1.15 + jitter
        case .unknown: pitch = 1.0
        }
        perSpeaker[key] = (voice, pitch)
        return (voice, pitch)
    }
}
