import SwiftUI
import Combine

/// Kịch bản cảnh mẫu (giống DemoScript bên Android)
enum DemoScript {
    struct Line { let speaker: String; let en: String; let vi: String; let gender: Gender }

    static let lines: [Line] = [
        Line(speaker: "AERIN", en: "Stay close to me. The bridge won't hold for long.", vi: "Theo sát tôi. Cây cầu không trụ được lâu đâu.", gender: .female),
        Line(speaker: "KAEL", en: "I can see the tower from here. We're almost there.", vi: "Từ đây tôi thấy được ngọn tháp rồi. Sắp tới nơi rồi.", gender: .male),
        Line(speaker: "AERIN", en: "Don't look down. Just keep walking.", vi: "Đừng nhìn xuống. Cứ đi tiếp đi.", gender: .female),
        Line(speaker: "KAEL", en: "Something is following us in the dark.", vi: "Có thứ gì đó đang bám theo chúng ta trong bóng tối.", gender: .male),
        Line(speaker: "AERIN", en: "Then we run. Now!", vi: "Vậy thì chạy. Ngay!", gender: .female),
        Line(speaker: "KAEL", en: "We'll find a way. Together.", vi: "Chúng ta sẽ tìm ra cách. Cùng nhau.", gender: .male),
    ]

    private static func key(_ s: String) -> String { String(s.lowercased().filter(\.isLetter)) }

    /// Bản dịch dự phòng khi chưa có mã dịch: khớp chữ OCR với kịch bản
    static func fallback(_ ocr: String) -> Translation? {
        let k = key(ocr)
        guard let line = lines.first(where: { k.contains(String(key($0.en).prefix(20))) }) else { return nil }
        return Translation(original: ocr, vietnamese: line.vi, speaker: line.speaker.capitalized, gender: line.gender)
    }
}

/// Cảnh game mẫu vẽ bằng code
struct DemoScene: View {
    let lineIndex: Int

    var body: some View {
        Canvas { ctx, size in
            let w = size.width, h = size.height
            ctx.fill(Path(CGRect(origin: .zero, size: size)),
                     with: .linearGradient(Gradient(colors: [Color(red: 0.09, green: 0.08, blue: 0.24), Color(red: 0.27, green: 0.2, blue: 0.43)]),
                                           startPoint: .zero, endPoint: CGPoint(x: 0, y: h)))
            for i in 0..<40 {
                let x = CGFloat((i * 73) % 100) / 100 * w, y = CGFloat((i * 37) % 45) / 100 * h
                ctx.fill(Path(ellipseIn: CGRect(x: x, y: y, width: w * 0.005, height: w * 0.005)), with: .color(.white.opacity(0.8)))
            }
            ctx.fill(Path(ellipseIn: CGRect(x: w * 0.508, y: h * 0.32, width: w * 0.024, height: w * 0.024)), with: .color(Color(red: 1, green: 0.77, blue: 0.47)))
            var hill1 = Path()
            hill1.move(to: CGPoint(x: 0, y: h * 0.62))
            hill1.addQuadCurve(to: CGPoint(x: w * 0.6, y: h * 0.6), control: CGPoint(x: w * 0.3, y: h * 0.45))
            hill1.addQuadCurve(to: CGPoint(x: w, y: h * 0.58), control: CGPoint(x: w * 0.85, y: h * 0.5))
            hill1.addLine(to: CGPoint(x: w, y: h)); hill1.addLine(to: CGPoint(x: 0, y: h)); hill1.closeSubpath()
            ctx.fill(hill1, with: .color(Color(red: 0.18, green: 0.15, blue: 0.33)))
            var hill2 = Path()
            hill2.move(to: CGPoint(x: 0, y: h * 0.72))
            hill2.addQuadCurve(to: CGPoint(x: w, y: h * 0.74), control: CGPoint(x: w * 0.5, y: h * 0.62))
            hill2.addLine(to: CGPoint(x: w, y: h)); hill2.addLine(to: CGPoint(x: 0, y: h)); hill2.closeSubpath()
            ctx.fill(hill2, with: .color(Color(red: 0.12, green: 0.09, blue: 0.23)))
            for (x, s) in [(0.47, 1.0), (0.55, 0.85)] {
                let fh = h * 0.16 * s
                let cx = w * x
                ctx.fill(Path(ellipseIn: CGRect(x: cx - fh * 0.16, y: h * 0.6 - fh - fh * 0.16, width: fh * 0.32, height: fh * 0.32)), with: .color(.black))
                ctx.fill(Path(roundedRect: CGRect(x: cx - fh * 0.14, y: h * 0.6 - fh * 0.85, width: fh * 0.28, height: fh * 0.85), cornerRadius: fh * 0.08), with: .color(.black))
            }
            let line = DemoScript.lines[lineIndex % DemoScript.lines.count]
            ctx.draw(Text(line.speaker).font(.system(size: h * 0.045, weight: .bold)).foregroundColor(.white), at: CGPoint(x: w / 2, y: h * 0.82))
            ctx.draw(Text(line.en).font(.system(size: h * 0.058)).foregroundColor(.white), at: CGPoint(x: w / 2, y: h * 0.9))
        }
        .aspectRatio(16 / 9, contentMode: .fit)
    }

    /// Vẽ cảnh ra ảnh để OCR (giống chụp hình máy chơi game)
    @MainActor
    static func render(lineIndex: Int, width: CGFloat = 1280) -> CGImage? {
        let r = ImageRenderer(content: DemoScene(lineIndex: lineIndex).frame(width: width, height: width * 9 / 16))
        r.scale = 1
        return r.cgImage
    }
}

struct DemoView: View {
    @ObservedObject var prefs = Prefs.shared
    @Environment(\.dismiss) private var dismiss
    @State private var lineIndex = 0
    @State private var region = CGRect(x: 0.06, y: 0.78, width: 0.88, height: 0.19)
    @State private var editing = false
    @StateObject private var holder = EngineHolder()
    private let timer = Timer.publish(every: 4.5, on: .main, in: .common).autoconnect()

    var body: some View {
        VStack(spacing: 0) {
            Text("XEM THỬ · cảnh mẫu do ứng dụng tạo, không cần máy chơi game")
                .font(.footnote).foregroundColor(.white.opacity(0.8))
                .frame(maxWidth: .infinity, alignment: .leading).padding()
            ZStack {
                DemoScene(lineIndex: lineIndex)
                RegionSelector(region: $region, editing: editing)
            }
            .aspectRatio(16 / 9, contentMode: .fit)
            Button(editing ? "Xong" : "Thử kéo khung") { editing.toggle() }
                .foregroundColor(Color(red: 0.62, green: 0.88, blue: 0.8))
                .frame(maxWidth: .infinity, alignment: .leading).padding(.horizontal).padding(.top, 8)

            VStack(spacing: 12) {
                HStack {
                    Spacer()
                    Text("🇻🇳 Bản dịch tiếng Việt").font(.footnote).foregroundColor(.white)
                        .padding(.horizontal, 10).padding(.vertical, 4)
                        .background(Capsule().fill(Color.orange))
                }
                if let t = holder.engine?.current {
                    Text(t.speaker.isEmpty ? t.vietnamese : "\(t.speaker): \(t.vietnamese)")
                        .font(.title2.bold()).foregroundColor(.white).multilineTextAlignment(.center)
                    Text(t.original.replacingOccurrences(of: "\n", with: " "))
                        .font(.footnote).foregroundColor(.white.opacity(0.6)).multilineTextAlignment(.center)
                } else {
                    Text("Đang đọc phụ đề…").font(.title3).foregroundColor(.white.opacity(0.7))
                }
                if let s = holder.engine?.status {
                    Text(s).font(.footnote).foregroundColor(.orange)
                }
                Spacer()
            }
            .padding(20)
            .frame(maxHeight: .infinity)

            Button { dismiss() } label: {
                Text("BẮT ĐẦU CHƠI THẬT").font(.headline).foregroundColor(.white)
                    .frame(maxWidth: .infinity).padding(.vertical, 14)
                    .background(Capsule().fill(Color.accentPurple))
            }
            .padding(.horizontal, 20).padding(.bottom, 24)
        }
        .background(Color.black.ignoresSafeArea())
        .onReceive(timer) { _ in lineIndex += 1 }
        .onAppear {
            let engine = SubtitleEngine(
                prefs: prefs, translator: Translator(prefs: prefs), speaker: Speaker(prefs: prefs),
                region: { region },
                fallback: DemoScript.fallback,
                frameSource: { await MainActor.run { DemoScene.render(lineIndex: lineIndex) } })
            holder.engine = engine
            engine.start()
        }
        .onDisappear { holder.engine?.stop() }
    }
}

/// Giữ engine để SwiftUI cập nhật khi engine đổi trạng thái
@MainActor
final class EngineHolder: ObservableObject {
    @Published var engine: SubtitleEngine? {
        didSet { sub = engine?.objectWillChange.sink { [weak self] _ in self?.objectWillChange.send() } }
    }
    private var sub: Any?
}
