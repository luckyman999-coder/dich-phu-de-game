import SwiftUI

extension Color {
    // Bảng màu riêng của Dịch Game: nền tối, nhấn xanh ngọc
    static let dgBg = Color(red: 14/255, green: 17/255, blue: 22/255)
    static let dgCard = Color(red: 24/255, green: 28/255, blue: 36/255)
    static let dgStroke = Color(red: 40/255, green: 46/255, blue: 58/255)
    static let dgText = Color(red: 236/255, green: 239/255, blue: 244/255)
    static let dgMuted = Color(red: 140/255, green: 148/255, blue: 162/255)
    static let dgAccent = Color(red: 45/255, green: 212/255, blue: 167/255)
    static let dgAmber = Color(red: 245/255, green: 180/255, blue: 40/255)
    static let accentPurple = dgAccent
}

@main
struct DichGameApp: App {
    var body: some Scene {
        WindowGroup { HomeView().preferredColorScheme(.dark) }
    }
}

/// Màn hình chính: thẻ máy chơi game + game đang chơi + danh sách chuẩn bị
struct HomeView: View {
    @ObservedObject var prefs = Prefs.shared
    @State private var showSettings = false
    @State private var showDemo = false
    @State private var showGameBook = false
    @State private var showKeyGuide = false
    @ObservedObject var book = GameBook.shared
    @State private var alert: HomeAlert?

    enum HomeAlert: Identifiable {
        case needKey, remotePlaySoon
        var id: Int { self == .needKey ? 0 : 1 }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                HStack(spacing: 6) {
                    Text("Dịch").font(.system(size: 24, weight: .bold)).foregroundColor(.dgText)
                    Text("Game").font(.system(size: 24, weight: .bold)).foregroundColor(.dgBg)
                        .padding(.horizontal, 8).background(RoundedRectangle(cornerRadius: 8).fill(Color.dgAccent))
                    Spacer()
                    Button { showSettings = true } label: {
                        Image(systemName: "gearshape").font(.title2).foregroundColor(.dgText)
                    }
                    .accessibilityLabel("Cài đặt")
                }
                Text("Phụ đề và giọng đọc tiếng Việt cho game PS5")
                    .font(.subheadline).foregroundColor(.dgMuted).padding(.top, 4)

                // thẻ máy chơi game
                VStack(spacing: 0) {
                    HStack(spacing: 14) {
                        Text("PS5").font(.subheadline.bold()).foregroundColor(.dgText)
                            .frame(width: 52, height: 52)
                            .background(RoundedRectangle(cornerRadius: 12).fill(Color.dgStroke))
                        VStack(alignment: .leading, spacing: 4) {
                            Text("Máy chơi game của bạn").font(.headline).foregroundColor(.dgText)
                            HStack(spacing: 6) {
                                Circle().fill(Color.dgMuted).frame(width: 8, height: 8)
                                Text("Chưa thấy máy chơi game").font(.footnote).foregroundColor(.dgMuted)
                            }
                        }
                        Spacer()
                    }
                    Button(action: start) {
                        VStack(spacing: 2) {
                            Text("▶  CHƠI VÀ DỊCH").font(.system(size: 18, weight: .bold))
                            Text("kết nối & dịch").font(.footnote).opacity(0.7)
                        }
                        .foregroundColor(.dgBg)
                        .frame(maxWidth: .infinity).padding(.vertical, 14)
                        .background(RoundedRectangle(cornerRadius: 14).fill(Color.dgAccent))
                    }
                    .padding(.top, 18)
                    Button("Chưa có máy? Xem thử bằng cảnh mẫu") { showDemo = true }
                        .font(.subheadline).foregroundColor(.dgAccent).padding(.top, 14)
                }
                .padding(18)
                .background(RoundedRectangle(cornerRadius: 20).fill(Color.dgCard))
                .overlay(RoundedRectangle(cornerRadius: 20).stroke(Color.dgStroke))
                .padding(.top, 20)

                section("GAME ĐANG CHƠI")
                Button { showGameBook = true } label: {
                    VStack(alignment: .leading, spacing: 4) {
                        HStack {
                            Text("🎮  " + (book.current.map { "\($0.name) · \($0.entries.count) mục" } ?? "Chung · tự nhận diện")).foregroundColor(.dgText)
                            Spacer()
                            Text("Sổ tay ›").foregroundColor(.dgAccent)
                        }
                        Text("Chọn đúng game để AI giữ tên nhân vật và cách xưng hô")
                            .font(.footnote).foregroundColor(.dgMuted).multilineTextAlignment(.leading)
                    }
                    .padding(16)
                    .background(RoundedRectangle(cornerRadius: 20).fill(Color.dgCard))
                    .overlay(RoundedRectangle(cornerRadius: 20).stroke(Color.dgStroke))
                }

                section("CHUẨN BỊ")
                VStack(spacing: 0) {
                    checkRow("Mã dịch AI", ok: prefs.hasAnyKey,
                             sub: prefs.hasAnyKey ? "\(prefs.cleanGeminiKeys.count) mã Gemini" : "Lấy mã miễn phí – 2 phút") {
                        if prefs.hasAnyKey { showSettings = true } else { showKeyGuide = true }
                    }
                    checkRow("Tài khoản PSN", ok: !prefs.psnAccountId.isEmpty,
                             sub: prefs.psnAccountId.isEmpty ? "Đăng nhập để ghép đôi" : "Đã lấy Account ID") { start() }
                    checkRow("Ghép đôi máy chơi game", ok: false, sub: "Bấm Chơi để tự tìm và ghép đôi") { start() }
                }
                .background(RoundedRectangle(cornerRadius: 20).fill(Color.dgCard))
                .overlay(RoundedRectangle(cornerRadius: 20).stroke(Color.dgStroke))
            }
            .padding(20)
        }
        .background(Color.dgBg.ignoresSafeArea())
        .sheet(isPresented: $showSettings) { SettingsView() }
        .fullScreenCover(isPresented: $showDemo) { DemoView() }
        .sheet(isPresented: $showGameBook) { GameBookView() }
        .sheet(isPresented: $showKeyGuide) { KeyGuideView() }
        .alert(item: $alert) { a in
            switch a {
            case .needKey:
                return Alert(title: Text("Thêm mã dịch miễn phí"),
                             message: Text("Cần ít nhất 1 mã Gemini (miễn phí) để dịch. Có hướng dẫn lấy mã trong Cài đặt.\n\nMuốn xem thử trước thì bấm \"Xem thử\"."),
                             primaryButton: .default(Text("Lấy mã ngay")) { showKeyGuide = true },
                             secondaryButton: .default(Text("Xem thử")) { showDemo = true })
            case .remotePlaySoon:
                return Alert(title: Text("Sắp có"),
                             message: Text("Kết nối máy chơi game trên iPhone đang được hoàn thiện. Trong lúc chờ, bấm \"Xem thử\" để trải nghiệm dịch phụ đề."),
                             dismissButton: .default(Text("Đã hiểu")))
            }
        }
    }

    private func section(_ t: String) -> some View {
        Text(t).font(.footnote).foregroundColor(.dgMuted).padding(.top, 24).padding(.bottom, 8).padding(.leading, 4)
    }

    private func checkRow(_ title: String, ok: Bool, sub: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 12) {
                Text(ok ? "✓" : "!").font(.subheadline.bold()).foregroundColor(ok ? .dgBg : .black)
                    .frame(width: 26, height: 26).background(Circle().fill(ok ? Color.dgAccent : Color.dgAmber))
                VStack(alignment: .leading, spacing: 2) {
                    Text(title).foregroundColor(.dgText)
                    Text(sub).font(.footnote).foregroundColor(.dgMuted)
                }
                Spacer()
                Image(systemName: "chevron.right").foregroundColor(.dgMuted)
            }
            .padding(.horizontal, 14).padding(.vertical, 12)
        }
    }

    private func start() {
        if !prefs.hasAnyKey { alert = .needKey; return }
        if RemotePlay.isAvailable {
            RemotePlay.start()
        } else {
            alert = .remotePlaySoon
        }
    }
}

/// Kết nối Chơi từ xa trên iOS: giai đoạn 2 (biên dịch lõi chiaki-ng cho iOS).
enum RemotePlay {
    static let isAvailable = false
    static func start() {}
}
