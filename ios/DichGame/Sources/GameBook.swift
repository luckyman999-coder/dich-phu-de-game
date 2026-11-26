import Foundation

/// Sổ tay game (giống GameBook.kt bên Android)
enum Genre: String, Codable, CaseIterable, Identifiable {
    case auto, action, myth, wuxia, horror, scifi, modern, fantasy, comedy
    var id: String { rawValue }

    var label: String {
        switch self {
        case .auto: return "Tự nhận diện"
        case .action: return "Hành động"
        case .myth: return "Thần thoại / sử thi"
        case .wuxia: return "Kiếm hiệp / cổ trang"
        case .horror: return "Kinh dị"
        case .scifi: return "Khoa học viễn tưởng"
        case .modern: return "Đời thường / thành phố"
        case .fantasy: return "Giả tưởng / JRPG"
        case .comedy: return "Hài hước"
        }
    }

    var style: String {
        switch self {
        case .auto: return ""
        case .action: return "Văn phong hành động: câu ngắn, dứt khoát, giàu cảm xúc."
        case .myth: return "Văn phong sử thi, trang trọng; thần linh xưng \"ta\", gọi \"ngươi\"."
        case .wuxia: return "Văn phong cổ trang, dùng từ Hán Việt; xưng hô \"tại hạ\", \"huynh\", \"muội\", \"lão phu\", \"ngươi\" theo vai vế."
        case .horror: return "Văn phong căng thẳng, u ám, câu ngắn."
        case .scifi: return "Văn phong hiện đại, thuật ngữ công nghệ giữ nguyên tiếng Anh nếu thông dụng."
        case .modern: return "Văn phong đời thường, khẩu ngữ tự nhiên, được dùng tiếng lóng nhẹ."
        case .fantasy: return "Văn phong phiêu lưu giả tưởng, tự nhiên như phim hoạt hình lồng tiếng."
        case .comedy: return "Văn phong dí dỏm, giữ được câu đùa, chơi chữ theo kiểu Việt."
        }
    }
}

enum EntryKind: String, Codable, CaseIterable, Identifiable {
    case character, name, place, item, term
    var id: String { rawValue }
    var label: String {
        switch self {
        case .character: return "Nhân vật"
        case .name: return "Tên riêng"
        case .place: return "Địa danh"
        case .item: return "Kỹ năng / vật phẩm"
        case .term: return "Thuật ngữ"
        }
    }
}

enum BookGender: String, Codable, CaseIterable {
    case unknown, male, female
    var label: String { self == .male ? "Nam" : self == .female ? "Nữ" : "Chưa rõ" }
    var gender: Gender { self == .male ? .male : self == .female ? .female : .unknown }
}

struct BookEntry: Codable, Identifiable, Equatable {
    var id = UUID()
    var term: String
    var kind: EntryKind
    var gender: BookGender
    var note: String
}

struct GameProfile: Codable, Identifiable, Equatable {
    var id = UUID()
    var name: String
    var genre: Genre = .auto
    var context: String = ""
    var entries: [BookEntry] = []

    var rendered: String {
        var s = ""
        if !genre.style.isEmpty { s += "Thể loại: \(genre.label). \(genre.style)\n" }
        let c = context.trimmingCharacters(in: .whitespacesAndNewlines)
        if !c.isEmpty { s += c + "\n" }
        if !entries.isEmpty {
            s += "Từ điển của game (bắt buộc dùng đúng):\n"
            for e in entries {
                s += "- \(e.term) [\(e.kind.label)"
                if e.gender == .male { s += ", nam" } else if e.gender == .female { s += ", nữ" }
                s += "]"
                let n = e.note.trimmingCharacters(in: .whitespacesAndNewlines)
                if !n.isEmpty { s += ": \(n)" }
                s += "\n"
            }
        }
        return s.trimmingCharacters(in: .whitespacesAndNewlines)
    }
}

@MainActor
final class GameBook: ObservableObject {
    static let shared = GameBook()
    private let d = UserDefaults.standard

    @Published private(set) var games: [GameProfile] = []
    @Published private(set) var currentId: UUID?

    private init() {
        if let data = d.data(forKey: "book_games"), let g = try? JSONDecoder().decode([GameProfile].self, from: data) { games = g }
        if let s = d.string(forKey: "book_current") { currentId = UUID(uuidString: s) }
    }

    var current: GameProfile? { games.first { $0.id == currentId } }

    func select(_ id: UUID?) {
        currentId = id
        d.set(id?.uuidString, forKey: "book_current")
        sync()
    }

    func upsert(_ p: GameProfile) {
        if let i = games.firstIndex(where: { $0.id == p.id }) { games[i] = p } else { games.insert(p, at: 0) }
        save()
        if currentId == p.id { sync() }
    }

    func delete(_ id: UUID) {
        games.removeAll { $0.id == id }
        save()
        if currentId == id { select(nil) }
    }

    @discardableResult
    func create(_ name: String, context: String = "") -> GameProfile {
        let p = GameProfile(name: name.trimmingCharacters(in: .whitespaces), context: context)
        upsert(p)
        select(p.id)
        return p
    }

    func addPreset(_ preset: GamePreset) {
        if let g = games.first(where: { $0.name.caseInsensitiveCompare(preset.name) == .orderedSame }) { select(g.id); return }
        create(preset.name, context: preset.notes)
    }

    /// Gender theo sổ tay, ưu tiên hơn AI đoán
    func gender(forSpeaker name: String) -> Gender? {
        guard let p = current, !name.isEmpty else { return nil }
        let e = p.entries.first { $0.kind == .character && $0.term.caseInsensitiveCompare(name) == .orderedSame && $0.gender != .unknown }
        return e?.gender.gender
    }

    private func save() {
        if let data = try? JSONEncoder().encode(games) { d.set(data, forKey: "book_games") }
    }

    /// Đồng bộ sang Prefs để Translator dùng
    private func sync() {
        let prefs = Prefs.shared
        prefs.gameName = current?.name ?? ""
        prefs.gameNotes = current?.rendered ?? ""
    }
}
