import SwiftUI

/// Hướng dẫn (giống GuideActivity bên Android)
struct GuideView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var showKeyGuide = false
    @State private var showBook = false
    @State private var showDemo = false

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 10) {
                    Text("4 bước là chơi được. Làm một lần, những lần sau chỉ cần bấm Chơi.")
                        .font(.subheadline).foregroundColor(.dgMuted)

                    infoSection("BẮT ĐẦU NHANH")
                    step(1, "Lấy mã dịch miễn phí", "Mã Gemini (Google) để AI dịch. Mất khoảng 2 phút, không cần thẻ thanh toán.", "Lấy mã ngay ›") { showKeyGuide = true }
                    step(2, "Chuẩn bị máy PS5", "Trên PS5: Cài đặt → Hệ thống → Chơi từ xa → Bật Chơi từ xa.\nMuốn bật máy từ xa: Cài đặt → Hệ thống → Tiết kiệm năng lượng → Tính năng khi ở chế độ nghỉ → bật \"Duy trì kết nối Internet\" và \"Cho phép bật PS5 từ mạng\".", nil) {}
                    step(3, "Ghép đôi lần đầu", "Bấm CHƠI VÀ DỊCH. App tự tìm PS5, mời đăng nhập PSN (nên dùng tài khoản PSN thứ 2), rồi hỏi mã 8 số.\nLấy mã trên PS5: Cài đặt → Hệ thống → Chơi từ xa → Liên kết thiết bị.", nil) {}
                    step(4, "Chọn game và chỉnh khung phụ đề", "Chọn game trong Sổ tay để AI giữ đúng tên nhân vật và xưng hô. Trong game: bấm nút chỉnh khung → kéo khung xanh trùm lên chỗ phụ đề → Xong.", "Mở Sổ tay game ›") { showBook = true }

                    infoSection("MẸO")
                    VStack(alignment: .leading, spacing: 6) {
                        ForEach([
                            "iPhone và PS5 nên cùng Wi-Fi 5GHz, hoặc cắm dây mạng cho PS5.",
                            "Bật hiện tên người nói trong cài đặt phụ đề của game: AI chọn xưng hô và giọng nam/nữ chuẩn hơn.",
                            "Phụ đề hiện chậm hơn lời thoại khoảng 1–2 giây, vì phải đọc chữ và hỏi AI.",
                            "Thêm 2–3 mã Gemini (tạo ở các dự án khác nhau) để ít bị hết lượt.",
                            "Cắm sạc khi chơi lâu."
                        ], id: \.self) { Text("•  \($0)").font(.subheadline).foregroundColor(.dgText) }
                    }
                    .padding(16).frame(maxWidth: .infinity, alignment: .leading).cardStyle()

                    infoSection("GẶP LỖI THƯỜNG GẶP")
                    faq("Báo \"Gemini lỗi 429 (hết lượt)\"", "Mã miễn phí đã hết lượt trong phút/ngày. Thêm mã Gemini khác hoặc mã Groq dự phòng, app tự chuyển.")
                    faq("Không tìm thấy máy chơi game", "Kiểm tra cùng Wi-Fi, đã bật Chơi từ xa trên PS5.")
                    faq("Dịch sai tên hoặc xưng hô", "Thêm nhân vật vào Sổ tay game kèm giới tính và cách xưng hô.")

                    Button("Xem thử bằng cảnh mẫu ›") { showDemo = true }
                        .font(.headline).foregroundColor(.dgAccent).frame(maxWidth: .infinity).padding(.top, 16)
                }
                .padding(20)
            }
            .background(Color.dgBg.ignoresSafeArea())
            .navigationTitle("Hướng dẫn")
            .toolbar { ToolbarItem(placement: .confirmationAction) { Button("Xong") { dismiss() } } }
            .sheet(isPresented: $showKeyGuide) { KeyGuideView() }
            .sheet(isPresented: $showBook) { GameBookView() }
            .fullScreenCover(isPresented: $showDemo) { DemoView() }
        }
        .preferredColorScheme(.dark)
    }

    private func step(_ n: Int, _ title: String, _ desc: String, _ action: String?, _ onTap: @escaping () -> Void) -> some View {
        HStack(alignment: .top, spacing: 12) {
            Text("\(n)").font(.subheadline.bold()).foregroundColor(.dgBg)
                .frame(width: 28, height: 28).background(Circle().fill(Color.dgAccent))
            VStack(alignment: .leading, spacing: 6) {
                Text(title).font(.headline).foregroundColor(.dgText)
                Text(desc).font(.subheadline).foregroundColor(.dgMuted)
                if let action { Button(action, action: onTap).font(.subheadline.bold()).foregroundColor(.dgAccent) }
            }
            Spacer(minLength: 0)
        }
        .padding(14).cardStyle()
    }

    private func faq(_ q: String, _ a: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(q).font(.subheadline.bold()).foregroundColor(.dgText)
            Text(a).font(.subheadline).foregroundColor(.dgMuted)
        }
        .padding(14).frame(maxWidth: .infinity, alignment: .leading).cardStyle()
    }
}

/// Giới thiệu & quyền riêng tư (giống AboutActivity bên Android)
struct AboutView: View {
    @Environment(\.dismiss) private var dismiss
    private var version: String { Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "?" }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 10) {
                    VStack(alignment: .leading, spacing: 8) {
                        HStack(spacing: 6) {
                            Text("Dịch").font(.system(size: 24, weight: .bold)).foregroundColor(.dgText)
                            Text("Game").font(.system(size: 24, weight: .bold)).foregroundColor(.dgBg)
                                .padding(.horizontal, 8).background(RoundedRectangle(cornerRadius: 8).fill(Color.dgAccent))
                        }
                        Text("Phiên bản iOS \(version)").font(.footnote).foregroundColor(.dgMuted)
                        Text("Dịch Game đọc phụ đề tiếng Anh trong game PS5, dịch sang tiếng Việt bằng AI theo đúng văn phong từng game, hiện chữ và đọc to bằng giọng Việt.")
                            .font(.subheadline).foregroundColor(.dgText)
                    }
                    .padding(16).frame(maxWidth: .infinity, alignment: .leading).cardStyle()

                    infoSection("DỮ LIỆU VÀ QUYỀN RIÊNG TƯ")
                    VStack(alignment: .leading, spacing: 6) {
                        ForEach([
                            "Chữ phụ đề đọc được trên màn hình được gửi tới Google Gemini hoặc Groq bằng chính mã dịch của bạn để dịch. Hình ảnh game không được gửi đi.",
                            "Đăng nhập PSN chỉ để lấy Account ID phục vụ ghép đôi máy. App không thấy và không lưu mật khẩu PSN.",
                            "Mã dịch, sổ tay game và cài đặt chỉ lưu trên máy của bạn.",
                            "Kết nối tới máy chơi game diễn ra trong mạng Wi-Fi nhà bạn."
                        ], id: \.self) { Text("•  \($0)").font(.subheadline).foregroundColor(.dgText) }
                    }
                    .padding(16).frame(maxWidth: .infinity, alignment: .leading).cardStyle()

                    infoSection("MÃ NGUỒN MỞ")
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Phần kết nối máy chơi game dựa trên chiaki-ng, phát hành theo giấy phép GNU AGPL-3.0. Toàn bộ mã nguồn của Dịch Game được công khai theo cùng giấy phép.")
                            .font(.subheadline).foregroundColor(.dgText)
                        Link("Mã nguồn Dịch Game ›", destination: URL(string: "https://github.com/luckyman999-coder/dich-phu-de-game")!)
                        Link("Dự án chiaki-ng ›", destination: URL(string: "https://github.com/streetpea/chiaki-ng")!)
                        Link("Giấy phép AGPL-3.0 ›", destination: URL(string: "https://www.gnu.org/licenses/agpl-3.0.html")!)
                    }
                    .tint(.dgAccent)
                    .padding(16).frame(maxWidth: .infinity, alignment: .leading).cardStyle()

                    infoSection("LƯU Ý")
                    Text("Dịch Game là ứng dụng độc lập, không liên kết với Sony Interactive Entertainment. PlayStation, PS5 và PSN là thương hiệu của Sony Interactive Entertainment. Gemini là thương hiệu của Google; Groq là thương hiệu của Groq, Inc.")
                        .font(.footnote).foregroundColor(.dgMuted)
                        .padding(16).frame(maxWidth: .infinity, alignment: .leading).cardStyle()
                }
                .padding(20)
            }
            .background(Color.dgBg.ignoresSafeArea())
            .navigationTitle("Giới thiệu")
            .toolbar { ToolbarItem(placement: .confirmationAction) { Button("Xong") { dismiss() } } }
        }
        .preferredColorScheme(.dark)
    }
}

func infoSection(_ t: String) -> some View {
    Text(t).font(.footnote).foregroundColor(.dgMuted).padding(.top, 16).padding(.leading, 4)
}
