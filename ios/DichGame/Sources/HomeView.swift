import SwiftUI

extension Color {
    static let accentPurple = Color(red: 0.42, green: 0.39, blue: 1.0)
    static let accentPurple2 = Color(red: 0.55, green: 0.36, blue: 0.96)
    static let homeBg = Color(red: 0.93, green: 0.94, blue: 1.0)
}

@main
struct DichGameApp: App {
    var body: some Scene {
        WindowGroup { HomeView() }
    }
}

/// Màn hình chính một chạm (giống HomeActivity bên Android)
struct HomeView: View {
    @ObservedObject var prefs = Prefs.shared
    @State private var showSettings = false
    @State private var showDemo = false
    @State private var showGamePicker = false
    @State private var alert: HomeAlert?

    enum HomeAlert: Identifiable {
        case needKey, remotePlaySoon
        var id: Int { self == .needKey ? 0 : 1 }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                HStack {
                    Text("Dịch Phụ Đề Game").font(.system(size: 26, weight: .bold)).foregroundColor(Color(white: 0.07))
                    Spacer()
                    Circle().fill(Color.gray).frame(width: 12, height: 12).padding(.trailing, 10)
                    Button { showSettings = true } label: {
                        Image(systemName: "gearshape.fill").font(.title2).foregroundColor(Color(white: 0.07))
                    }
                    .accessibilityLabel("Cài đặt")
                }
                Button { showGamePicker = true } label: {
                    Text("🎮 \(prefs.gameName.isEmpty ? "Chung · Tự động" : prefs.gameName)  ›")
                        .font(.subheadline).foregroundColor(Color(white: 0.35))
                }
                .padding(.top, 4)

                Button(action: start) {
                    VStack(spacing: 4) {
                        Text("BẮT ĐẦU CHƠI").font(.system(size: 26, weight: .bold))
                        Text("kết nối & dịch").font(.subheadline).opacity(0.85)
                    }
                    .foregroundColor(.white)
                    .frame(width: 230, height: 230)
                    .background(Circle().fill(LinearGradient(colors: [.accentPurple, .accentPurple2], startPoint: .topLeading, endPoint: .bottomTrailing)))
                    .shadow(color: .accentPurple.opacity(0.35), radius: 16, y: 8)
                }
                .frame(maxWidth: .infinity)
                .padding(.top, 40).padding(.bottom, 28)

                Button { showDemo = true } label: {
                    Label("Xem thử – không cần máy chơi game", systemImage: "play.circle.fill")
                        .font(.body.weight(.medium)).foregroundColor(.accentPurple)
                        .frame(maxWidth: .infinity).padding(.vertical, 12)
                        .background(Capsule().fill(Color.white))
                }

                VStack(alignment: .leading, spacing: 8) {
                    ForEach([
                        "Lần đầu: bấm BẮT ĐẦU CHƠI → đăng nhập PSN → nhập mã ghép đôi hiện trên máy chơi game",
                        "iPhone và máy chơi game nên cùng Wi-Fi 5GHz cho hình mượt",
                        "Trong game: bấm nút tròn → \"Chọn khung dịch\" để kéo khung trùm phụ đề",
                        "Cắm sạc khi chơi lâu"
                    ], id: \.self) { tip in
                        Text("💡  \(tip)").font(.subheadline).foregroundColor(Color(white: 0.3))
                    }
                }
                .padding(16)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(RoundedRectangle(cornerRadius: 16).fill(Color.white.opacity(0.8)))
                .padding(.top, 20)
            }
            .padding(20)
        }
        .background(Color.homeBg.ignoresSafeArea())
        .sheet(isPresented: $showSettings) { SettingsView() }
        .fullScreenCover(isPresented: $showDemo) { DemoView() }
        .confirmationDialog("Đang chơi game nào?", isPresented: $showGamePicker, titleVisibility: .visible) {
            Button("Chung · Tự động") { prefs.gameName = ""; prefs.gameNotes = "" }
            ForEach(GamePresets.all) { p in
                Button(p.name) { prefs.gameName = p.name; prefs.gameNotes = p.notes }
            }
        }
        .alert(item: $alert) { a in
            switch a {
            case .needKey:
                return Alert(title: Text("Thêm mã dịch miễn phí"),
                             message: Text("Cần ít nhất 1 mã Gemini (miễn phí) để dịch. Có hướng dẫn lấy mã trong Cài đặt.\n\nMuốn xem thử trước thì bấm \"Xem thử\"."),
                             primaryButton: .default(Text("Vào cài đặt")) { showSettings = true },
                             secondaryButton: .default(Text("Xem thử")) { showDemo = true })
            case .remotePlaySoon:
                return Alert(title: Text("Sắp có"),
                             message: Text("Kết nối máy chơi game trên iPhone đang được hoàn thiện. Trong lúc chờ, bấm \"Xem thử\" để trải nghiệm dịch phụ đề."),
                             dismissButton: .default(Text("Đã hiểu")))
            }
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
