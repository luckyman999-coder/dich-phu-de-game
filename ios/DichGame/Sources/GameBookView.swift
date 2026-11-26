import SwiftUI

struct GameBookView: View {
    @ObservedObject var book = GameBook.shared
    @Environment(\.dismiss) private var dismiss
    @State private var context = ""
    @State private var showLibrary = false
    @State private var showNewGame = false
    @State private var newGameName = ""
    @State private var editing: EntryEdit?
    @State private var confirmDelete = false

    struct EntryEdit: Identifiable { let id = UUID(); var index: Int?; var entry: BookEntry }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    Text("AI đọc sổ tay này mỗi lần dịch để giữ đúng tên, giới tính và cách xưng hô.")
                        .font(.subheadline).foregroundColor(.dgMuted)

                    section("ĐANG DÙNG")
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            chip("Chung", selected: book.currentId == nil) { book.select(nil) }
                            ForEach(book.games) { g in chip(g.name, selected: book.currentId == g.id) { book.select(g.id) } }
                            chip("＋ Thêm", selected: false, outline: true) { showNewGame = true }
                        }
                    }

                    if let cur = book.current {
                        gameDetail(cur)
                    } else {
                        VStack(alignment: .leading, spacing: 10) {
                            Text("Đang dịch chế độ chung").font(.headline).foregroundColor(.dgText)
                            Text("AI tự đoán văn phong. Chọn hoặc thêm game để bản dịch chuẩn hơn, nhất là tên nhân vật và xưng hô.")
                                .font(.subheadline).foregroundColor(.dgMuted)
                            accentButton("📚  Chọn từ thư viện \(GamePresets.all.count) game") { showLibrary = true }
                        }
                        .padding(16).frame(maxWidth: .infinity, alignment: .leading).cardStyle().padding(.top, 16)
                    }
                }
                .padding(20)
            }
            .background(Color.dgBg.ignoresSafeArea())
            .navigationTitle("Sổ tay game")
            .toolbar { ToolbarItem(placement: .confirmationAction) { Button("Xong") { dismiss() } } }
            .confirmationDialog("Thư viện game", isPresented: $showLibrary, titleVisibility: .visible) {
                ForEach(GamePresets.all) { p in Button(p.name) { book.addPreset(p); context = book.current?.context ?? "" } }
            }
            .confirmationDialog("Thêm game", isPresented: $showNewGame, titleVisibility: .visible) {
                Button("📚  Chọn từ thư viện") { showLibrary = true }
                Button("✏️  Tự nhập tên game") { newGameName = ""; promptNewGame = true }
            }
            .alert("Tên game", isPresented: $promptNewGame) {
                TextField("Ví dụ: Stellar Blade", text: $newGameName)
                Button("Thêm") {
                    let n = newGameName.trimmingCharacters(in: .whitespaces)
                    if !n.isEmpty { book.create(n); context = "" }
                }
                Button("Huỷ", role: .cancel) {}
            }
            .sheet(item: $editing) { e in
                EntryEditor(edit: e) { result in
                    guard var cur = book.current else { return }
                    switch result {
                    case .save(let entry):
                        if let i = e.index { cur.entries[i] = entry } else { cur.entries.append(entry) }
                    case .delete:
                        if let i = e.index { cur.entries.remove(at: i) }
                    }
                    book.upsert(cur)
                }
            }
            .onAppear { context = book.current?.context ?? "" }
            .onChange(of: book.currentId) { _ in context = book.current?.context ?? "" }
        }
        .preferredColorScheme(.dark)
    }

    @State private var promptNewGame = false

    @ViewBuilder
    private func gameDetail(_ cur: GameProfile) -> some View {
        section("VĂN PHONG")
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(Genre.allCases) { g in
                    chip(g.label, selected: cur.genre == g) { var c = cur; c.genre = g; book.upsert(c) }
                }
            }
        }

        section("BỐI CẢNH CÂU CHUYỆN")
        VStack(spacing: 10) {
            TextField("Cốt truyện, quan hệ giữa các nhân vật, thời đại…", text: $context, axis: .vertical)
                .lineLimit(3...10).foregroundColor(.dgText)
            accentButton("Lưu bối cảnh") { var c = cur; c.context = context; book.upsert(c) }
        }
        .padding(14).cardStyle()

        HStack {
            section("TỪ ĐIỂN (\(cur.entries.count))")
            Spacer()
            Button("＋ Thêm mục") {
                editing = EntryEdit(index: nil, entry: BookEntry(term: "", kind: .character, gender: .unknown, note: ""))
            }
            .foregroundColor(.dgAccent).padding(.top, 16)
        }
        VStack(alignment: .leading, spacing: 0) {
            if cur.entries.isEmpty {
                Text("Chưa có mục nào. Thêm tên nhân vật kèm giới tính để giọng đọc nam/nữ đúng người.")
                    .font(.subheadline).foregroundColor(.dgMuted).padding(16)
            }
            ForEach(Array(cur.entries.enumerated()), id: \.element.id) { i, e in
                Button { editing = EntryEdit(index: i, entry: e) } label: {
                    VStack(alignment: .leading, spacing: 3) {
                        Text(e.term).font(.headline).foregroundColor(.dgText)
                        Text(e.kind.label + (e.gender == .male ? " · ♂ nam" : e.gender == .female ? " · ♀ nữ" : ""))
                            .font(.caption).foregroundColor(.dgAccent)
                        if !e.note.isEmpty { Text(e.note).font(.subheadline).foregroundColor(.dgMuted).multilineTextAlignment(.leading) }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading).padding(.horizontal, 16).padding(.vertical, 12)
                }
                if i < cur.entries.count - 1 { Divider().background(Color.dgStroke) }
            }
        }
        .cardStyle()

        section("AI SẼ NHẬN ĐƯỢC")
        Text(cur.rendered.isEmpty ? "(trống)" : cur.rendered)
            .font(.system(.footnote, design: .monospaced)).foregroundColor(.dgMuted)
            .frame(maxWidth: .infinity, alignment: .leading).padding(14).cardStyle()

        Button("Xoá game này khỏi sổ tay") { confirmDelete = true }
            .foregroundColor(.red).frame(maxWidth: .infinity).padding(.top, 24)
            .confirmationDialog("Xoá \"\(cur.name)\" và toàn bộ ghi chú?", isPresented: $confirmDelete, titleVisibility: .visible) {
                Button("Xoá", role: .destructive) { book.delete(cur.id) }
            }
    }

    private func section(_ t: String) -> some View {
        Text(t).font(.footnote).foregroundColor(.dgMuted).padding(.top, 24).padding(.bottom, 8).padding(.leading, 4)
    }

    private func chip(_ label: String, selected: Bool, outline: Bool = false, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label).font(.subheadline)
                .foregroundColor(selected ? .dgBg : outline ? .dgAccent : .dgText)
                .padding(.horizontal, 14).padding(.vertical, 8)
                .background(Capsule().fill(selected ? Color.dgAccent : Color.dgCard))
                .overlay(Capsule().stroke(outline ? Color.dgAccent : Color.dgStroke))
        }
    }
}

func accentButton(_ label: String, action: @escaping () -> Void) -> some View {
    Button(action: action) {
        Text(label).font(.headline).foregroundColor(.dgBg)
            .frame(maxWidth: .infinity).padding(.vertical, 12)
            .background(RoundedRectangle(cornerRadius: 12).fill(Color.dgAccent))
    }
}

extension View {
    func cardStyle() -> some View {
        self.background(RoundedRectangle(cornerRadius: 18).fill(Color.dgCard))
            .overlay(RoundedRectangle(cornerRadius: 18).stroke(Color.dgStroke))
    }
}

/// Thêm / sửa một mục từ điển
struct EntryEditor: View {
    enum Result { case save(BookEntry), delete }
    let edit: GameBookView.EntryEdit
    let onDone: (Result) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var entry: BookEntry

    init(edit: GameBookView.EntryEdit, onDone: @escaping (Result) -> Void) {
        self.edit = edit
        self.onDone = onDone
        _entry = State(initialValue: edit.entry)
    }

    var body: some View {
        NavigationStack {
            Form {
                TextField("Tên / từ, ví dụ: Atreus", text: $entry.term)
                Picker("Loại", selection: $entry.kind) {
                    ForEach(EntryKind.allCases) { Text($0.label).tag($0) }
                }
                Picker("Giới tính", selection: $entry.gender) {
                    ForEach(BookGender.allCases, id: \.self) { Text($0.label).tag($0) }
                }
                .pickerStyle(.segmented)
                TextField("Cách dịch / vai trò / xưng hô, ví dụ: con trai Kratos, xưng \"con\", gọi Kratos là \"cha\"",
                          text: $entry.note, axis: .vertical)
                    .lineLimit(2...6)
                if edit.index != nil {
                    Button("Xoá mục này", role: .destructive) { onDone(.delete); dismiss() }
                }
            }
            .navigationTitle(edit.index == nil ? "Thêm vào từ điển" : "Sửa mục")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Huỷ") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Lưu") {
                        entry.term = entry.term.trimmingCharacters(in: .whitespaces)
                        guard !entry.term.isEmpty else { return }
                        onDone(.save(entry)); dismiss()
                    }
                }
            }
        }
        .preferredColorScheme(.dark)
    }
}
