# Semantic Segmentation — tách nền/chủ thể (một phần)

## Tóm tắt

Dùng MediaPipe **Selfie Segmentation** để ước lượng độ "rối" của hậu cảnh
khi chụp chân dung, sinh guidance `BUSY_BACKGROUND` ("Hậu cảnh hơi rối, thử
đổi góc chụp") khi phù hợp.

**Đây là proxy gián tiếp, không phải phép đo độ rối hình ảnh trực tiếp.**
`BackgroundClutterScorer` chỉ nhìn vào **hình dạng của mask tách nền/chủ
thể** do MediaPipe trả về — đếm số lần chuyển đổi nền↔chủ thể trên mỗi
hàng của mask (mask gọn gàng, chủ thể là 1 khối liền mạch → ít lần chuyển
đổi → `CLEAN`; mask bị vụn/nhiều đốm rải rác → nhiều lần chuyển đổi →
`BUSY`). Cách tiếp cận này được chọn **thay vì** so khớp mask với dữ liệu
độ sáng thô (luma plane) theo từng pixel, để tránh lặp lại rủi ro lệch hệ
toạ độ xoay ảnh đã ghi nhận ở `HorizonLineDetector` (mask của MediaPipe ở
hệ toạ độ đã hiệu chỉnh xoay, còn luma buffer thô thì chưa) — xem chi tiết
trong KDoc của `BackgroundSegmentationProcessor.kt`.

## Giới hạn kỹ thuật quan trọng — đọc trước khi test

1. **Cần mạng ở lần chạy đầu tiên** (giống Pose Assistant) — model
   `selfie_segmenter.tflite` được tải về khi cần, không nhúng sẵn trong
   app. Nếu tải lỗi, tính năng tự tắt lặng lẽ cho phiên đó (`BackgroundState.UNKNOWN`),
   không crash, không chặn tính năng khác.
2. **Chạy throttled mỗi 8 frame**, chỉ khi đã có chủ thể chính được phát
   hiện (không chạy khi khung hình trống — không có gì để tách nền).
3. **Proxy gián tiếp** (đã nêu ở trên) — độ chính xác trên ảnh thật (mask
   "gồ ghề" có thực sự tương ứng với hậu cảnh rối về mặt thị giác hay
   không) **chưa được kiểm chứng trên thiết bị thật**. Ngưỡng
   `BUSY_AVG_TRANSITIONS_PER_ROW = 6f` là số tự chọn, chưa hiệu chỉnh.
4. Cần quyền `INTERNET` — đã có sẵn từ Pose Assistant (khai báo chung ở
   `data:vision`'s AndroidManifest), không cần thêm gì mới.

## Hướng dẫn kiểm thử

1. `./gradlew :domain:test` — có thêm 2 test case cho `BUSY_BACKGROUND`
   trong `AnalyzeCompositionUseCaseTest`.
2. Cài APK, mở camera, hướng vào người ở hậu cảnh lộn xộn (nhiều đồ vật,
   nhiều màu sắc/hoa văn) so với hậu cảnh trơn (tường phẳng, bầu trời) —
   xem guidance "Hậu cảnh hơi rối" có xuất hiện đúng lúc không.
3. Vì đây là proxy gián tiếp, đừng ngạc nhiên nếu có false positive/
   negative — báo lại các trường hợp sai để tinh chỉnh ngưỡng.
