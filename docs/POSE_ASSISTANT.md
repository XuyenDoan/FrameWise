# Pose Assistant (MediaPipe Pose Landmarker)

## Tóm tắt

Đã thêm gợi ý tư thế cơ bản khi chụp chân dung, dùng MediaPipe Tasks
Pose Landmarker (mô hình "lite", chạy on-device).

**Phạm vi cố ý thu hẹp** so với ý tưởng "Pose Assistant" đầy đủ trong bản
đặc tả gốc (xoay người, quay vai, nhìn trái/phải, bước lên/lùi...): chỉ có
2 gợi ý thực sự tính toán được đáng tin cậy chỉ từ landmark vai + mũi, mà
không cần biết hướng camera/độ sâu 3D chính xác:

- **Thả lỏng vai** — khi 2 vai lệch cao thấp rõ rệt (so với chiều rộng
  vai) → có thể đang gù/lệch vai.
- **Ngẩng cằm lên** — khi khoảng cách dọc từ mũi đến đường vai quá ngắn so
  với chiều rộng vai → ước lượng thô cho việc "cúi đầu quá nhiều".

Cả 2 đều là **heuristic ngưỡng tự chọn, chưa hiệu chỉnh trên ảnh thật** —
tương tự tinh thần trung thực đã áp dụng với `SceneClassifier`. Đã viết
unit test cho logic hình học thuần (`AnalyzePoseUseCaseTest`), nhưng độ
chính xác thực tế trên người thật/góc chụp thật thì tôi không kiểm chứng
được trong môi trường này.

## Giới hạn kỹ thuật quan trọng — đọc trước khi test

1. **Cần mạng ở lần chạy đầu tiên.** MediaPipe Tasks không có model nhúng
   sẵn trong app như ML Kit — `PoseModelProvider` tự tải file
   `pose_landmarker_lite.task` (~5-9MB) từ server công khai của Google về
   bộ nhớ riêng của app khi Pose Assistant chạy lần đầu. Nếu máy offline
   hoặc tải lỗi, tính năng này **tự tắt lặng lẽ cho phiên đó** (không có
   pose suggestion nào hiện ra), không crash app, không chặn các tính năng
   khác. Tôi **không kiểm chứng được URL tải model** trong sandbox này
   (mạng bị chặn), nên có rủi ro URL sai/đổi mà tôi không phát hiện được —
   nếu sau khi cài APK, mục Pose không bao giờ xuất hiện dù chụp chân
   dung rõ ràng, khả năng cao là bước tải model bị lỗi (cần xem Logcat tag
   `PoseModelProvider`/`PoseFrameProcessor`).
2. **Chạy throttled mỗi 5 frame** (nặng hơn ML Kit khá nhiều), và dùng chế
   độ `RunningMode.VIDEO` (gọi đồng bộ `detectForVideo`) thay vì
   `LIVE_STREAM` — lý do: pipeline phân tích frame hiện tại vốn đã tuần tự
   (face → object → scene → pose) trên 1 executor riêng, dùng API đồng bộ
   khớp với cấu trúc có sẵn mà không phải điều phối thêm 1 luồng callback
   bất đồng bộ tranh chấp vòng đời `ImageProxy`.
3. Yêu cầu quyền `INTERNET` (đã thêm vào `data:vision`'s AndroidManifest,
   chỉ dùng để tải model, không gửi ảnh/dữ liệu người dùng đi đâu cả).

## Hướng dẫn kiểm thử

1. `./gradlew :domain:test` — có thêm 5 test case cho `AnalyzePoseUseCase`.
2. Cài APK, mở camera, hướng vào một người (chân dung, thấy rõ vai).
3. Đợi vài giây (model cần tải xong lần đầu + throttle mỗi 5 frame) — nếu
   có mạng, sẽ thấy dòng gợi ý màu vàng nhạt phía trên nút chụp khi vai
   lệch hoặc cằm cúi thấp.
4. Thử nghiêng vai rõ rệt / cúi đầu mạnh để xem gợi ý có phản ứng đúng
   không — nhớ đây là heuristic ngưỡng cố định, dễ có false positive/
   negative với các tư thế không điển hình.
