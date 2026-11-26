import SwiftUI

/// Khung xanh kéo/thả để chọn vùng phụ đề (toạ độ 0..1)
struct RegionSelector: View {
    @Binding var region: CGRect
    var editing: Bool
    private let green = Color(red: 0.31, green: 0.86, blue: 0.47)
    private let minSize: CGFloat = 0.04

    @State private var start: CGRect?

    var body: some View {
        GeometryReader { geo in
            let W = geo.size.width, H = geo.size.height
            let r = CGRect(x: region.minX * W, y: region.minY * H, width: region.width * W, height: region.height * H)
            if editing {
                ZStack(alignment: .topLeading) {
                    Path { p in
                        p.addRect(CGRect(origin: .zero, size: geo.size))
                        p.addRect(r)
                    }
                    .fill(Color.black.opacity(0.45), style: FillStyle(eoFill: true))
                    Rectangle().stroke(green, lineWidth: 2)
                        .frame(width: r.width, height: r.height)
                        .contentShape(Rectangle())
                        .offset(x: r.minX, y: r.minY)
                        .gesture(DragGesture().onChanged { v in
                            let s = start ?? region; start = s
                            let dx = v.translation.width / W, dy = v.translation.height / H
                            var n = s.offsetBy(dx: dx, dy: dy)
                            n.origin.x = min(max(0, n.minX), 1 - n.width)
                            n.origin.y = min(max(0, n.minY), 1 - n.height)
                            region = n
                        }.onEnded { _ in start = nil })
                    Text("Kéo khung trùm lên chỗ phụ đề").font(.caption).foregroundColor(.white)
                        .offset(x: r.minX, y: max(0, r.minY - 20))
                    ForEach(0..<4, id: \.self) { corner in
                        let px = corner % 2 == 0 ? r.minX : r.maxX
                        let py = corner < 2 ? r.minY : r.maxY
                        Circle().fill(green).frame(width: 22, height: 22)
                            .offset(x: px - 11, y: py - 11)
                            .gesture(DragGesture().onChanged { v in
                                let s = start ?? region; start = s
                                let dx = v.translation.width / W, dy = v.translation.height / H
                                var l = s.minX, t = s.minY, rr = s.maxX, b = s.maxY
                                if corner % 2 == 0 { l = min(max(0, l + dx), rr - minSize) } else { rr = max(min(1, rr + dx), l + minSize) }
                                if corner < 2 { t = min(max(0, t + dy), b - minSize) } else { b = max(min(1, b + dy), t + minSize) }
                                region = CGRect(x: l, y: t, width: rr - l, height: b - t)
                            }.onEnded { _ in start = nil })
                    }
                }
            }
        }
        .allowsHitTesting(editing)
    }
}
