# Dịch Game PS5 – bản Android (giai đoạn 1)

Fork từ [chiaki-ng](https://github.com/streetpea/chiaki-ng) (giấy phép AGPL-3.0, nên mã nguồn bản sửa cũng phải công khai nếu phát hành).

## Làm được gì
- Điện thoại kết nối PS5 bằng Chơi từ xa (Remote Play), bạn vẫn chơi trên TV như thường.
- Đọc phụ đề tiếng Anh trong khung bạn kéo (OCR ML Kit chạy ngay trên máy).
- Dịch sang tiếng Việt bằng Gemini (xoay vòng 3 mã), Groq dự phòng; có ghi chú theo từng game.
- Hiện phụ đề Việt đè đúng chỗ trên màn hình điện thoại, đọc to bằng giọng Việt (nam trầm / nữ cao, mỗi nhân vật một giọng).
- Game đã có phụ đề Việt → chỉ đọc, không tốn lượt dịch.
- Tự tắt tiếng game trên điện thoại (TV đã có tiếng).

## Mã nguồn thêm vào
- `android/app/src/main/java/com/metallic/chiaki/dich/` – toàn bộ phần dịch
- `StreamActivity.kt`, `activity_stream.xml` – gắn phụ đề, khung chọn vùng, nút điều khiển
- `RegistActivity.kt` – nút đăng nhập PSN tự điền Account ID
- `cpp/audio-output.cpp`, `chiaki-jni.c`, `Chiaki.kt` – tắt tiếng game

## Build
**Cách 1 – GitHub Actions (không cần cài gì):** đẩy mã lên GitHub, workflow
`.github/workflows/build-android-dichgame.yml` tự build, tải APK ở tab Actions → Artifacts.

**Cách 2 – Android Studio:** `git submodule update --init --recursive`, cài NDK 28.2.13676358 + CMake 3.22.1,
`pip install protobuf grpcio-tools`, mở thư mục `android/`, Build → Build APK.

## Dùng
1. Mở app → Cài đặt dịch → dán mã Gemini/Groq → "Lưu và dịch thử".
2. Màn hình chính → nút + → Đăng ký máy → PS5 → "Đăng nhập PSN để tự lấy Account ID" → nhập PIN lấy trên PS5
   (Cài đặt → Hệ thống → Chơi từ xa → Liên kết thiết bị).
3. Bấm vào PS5 để kết nối. Vuốt mép màn hình để hiện thanh điều khiển → "Chỉnh khung phụ đề" → kéo khung trùm lên phụ đề → Xong.
