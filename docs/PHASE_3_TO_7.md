# FrameWise — Phase 3-7: Camera Controls, Overlay, Sensor, ML Kit, Composition Engine

> Tài liệu này lẽ ra phải có ngay sau `PHASE_2_CAMERAX_SETUP.md` nhưng bị bỏ
> sót lúc làm (code đã viết đủ trong commit `19d528b`, chỉ thiếu bản tóm
> tắt lưu vào `docs/`). Bổ sung lại cho đầy đủ mạch tài liệu.

## Phase 3 — Hoàn thiện Camera Controls

Mở rộng `feature:camerapreview` (đã có preview cơ bản từ Phase 2):

- **Exposure slider**: `ExposureSlider` composable, đọc/ghi qua
  `CameraRepository.setExposureIndex()` (đã có sẵn interface từ Phase 2,
  Phase 3 mới thật sự có UI dùng đến).
- **Tap-to-focus**: thêm `CameraPreviewBinder.focusAt(x, y)` — vì cần toạ
  độ theo `PreviewView.meteringPointFactory`, method này nằm ở
  `data:camera` (không đưa qua domain) theo đúng ngoại lệ kiến trúc đã nêu
  ở Phase 2 cho việc gắn surface.
- Gesture: `detectTapGestures` (focus) chạy song song với
  `detectTransformGestures` (pinch-to-zoom, đã có từ Phase 2) trên cùng
  `Box`, theo đúng pattern mẫu chính thức của CameraX + Compose.

File liên quan: `feature/camerapreview/CameraPreviewScreen.kt`,
`data/camera/CameraXController.kt` (method `focusAt`).

## Phase 4 — Grid Overlay (Compose Canvas thuần)

Module mới `feature:overlay`, vẽ hoàn toàn bằng `Canvas` — không dùng ảnh
bitmap nào, để overlay luôn sắc nét ở mọi kích thước preview:

| Grid | Cách vẽ |
|---|---|
| Rule of Thirds | 2 đường dọc + 2 đường ngang chia đều 3 phần |
| Golden Ratio | Tương tự nhưng tại 38.2%/61.8% thay vì 33.3%/66.6% |
| Golden Triangle | 1 đường chéo chính + 2 đường vuông góc hạ từ 2 góc còn lại xuống đường chéo |
| Square | Lưới 4x4 đều (đậm hơn thirds, hợp kiến trúc/pattern) |
| Diagonal | 2 đường chéo góc-đối-góc |

`GridType` là domain model (enum thuần, không phụ thuộc Android) để dùng
lại được ở Phase 7 (composition engine). Có `GridTypePicker` (chip chọn)
gắn trên thanh control của camera screen.

File: `feature/overlay/GridOverlay.kt`.

## Phase 5 — Horizon Level (sensor)

Module mới `data:sensor`, độc lập hoàn toàn với `data:camera`/`data:vision`:

- Dùng `Sensor.TYPE_ROTATION_VECTOR` (đã fused sẵn accelerometer +
  magnetometer/gyroscope trong Android, không cần tự viết complementary
  filter) → `SensorManager.getRotationMatrixFromVector` +
  `getOrientation` → lấy góc roll.
- Làm mượt bằng exponential smoothing đơn giản để kim không rung giật.
- Chỉ lắng nghe sensor khi có collector (`SharingStarted.WhileSubscribed`)
  — tự tắt khi rời màn hình camera, tiết kiệm pin.
- UI: `HorizonLevelOverlay` — 1 vạch tĩnh tham chiếu + 1 vạch xoay theo góc
  nghiêng thực tế, đổi màu xanh khi cân bằng, giống phong cách Sony/Fuji/
  Leica.

**Lưu ý đã ghi trong code:** dấu (trái/phải) của góc roll dựa trên hiểu
biết lý thuyết về `TYPE_ROTATION_VECTOR`, **chưa kiểm chứng được trên thiết
bị thật** trong môi trường viết code này — nếu vạch xoay ngược hướng khi
test thật, chỉ cần đảo dấu trong `HorizonSensorController.rollDegrees()`.

File: `data/sensor/HorizonSensorController.kt`, `feature/overlay/HorizonLevelOverlay.kt`.

## Phase 6 — ML Kit Face + Object Detection

Module mới `data:vision`:

- `CameraFrameProvider` (thêm vào `data:camera`): vì CameraX chỉ cho 1
  analyzer/`ImageAnalysis`, đây là "cửa" duy nhất để `data:vision` đăng ký
  nhận frame, tránh việc `data:vision` phải tự tạo `ImageAnalysis` riêng
  (không thể — CameraX không cho 2 analyzer cùng lúc trên 1 session).
- `MlKitFrameAnalyzer`: chạy tuần tự Face Detection → Object Detection
  (chain qua callback, không chạy song song, để dễ đảm bảo đúng đắn hơn là
  nhanh hơn) trên mỗi frame.
- `LuminanceEvaluator`: tự tính từ Y-plane (không cần model ML) để phát
  hiện thiếu sáng/thừa sáng/ngược sáng — so độ sáng vùng chủ thể với tổng
  thể khung hình.
- **Giới hạn đã ghi rõ trong code**: ML Kit Object Detection mặc định chỉ
  phân loại được 5 nhóm chung chung (không phải "chó"/"mèo"/"xe" như đặc tả
  gốc mong muốn) — xem KDoc của `SubjectLabel`.

File: `data/vision/MlKitFrameAnalyzer.kt`, `data/vision/LuminanceEvaluator.kt`,
`data/camera/CameraFrameProvider.kt`, `feature/overlay/BoundingBoxOverlay.kt`.

## Phase 7 — Composition Engine

`AnalyzeCompositionUseCase` (domain, pure function, có unit test đầy đủ) —
trái tim của app:

- So vị trí tâm bounding box chủ thể chính với 4 giao điểm 1/3 gần nhất →
  sinh `MOVE_LEFT`/`MOVE_RIGHT`/`RAISE_CAMERA`/`LOWER_CAMERA`.
- Headroom riêng cho khuôn mặt (label `FACE`) thay vì dùng chung logic
  thirds cho trục dọc.
- Kích thước chủ thể (diện tích bounding box) → `MOVE_CLOSER`/`MOVE_FARTHER`.
- Độ nghiêng đường chân trời (từ Phase 5) → `LEVEL_HORIZON`.
- Ánh sáng (từ Phase 6) → `IMPROVE_LIGHTING`.
- Chấm điểm 0-100: trừ điểm theo từng vấn đề, không cộng dồn vô hạn (đã
  `coerceIn(0, 100)`).
- Guidance được xếp hạng ưu tiên (`GuidanceType.priority`), UI chỉ hiện 1
  dòng quan trọng nhất tại một thời điểm — tránh spam nhiều hướng dẫn cùng
  lúc.

Wiring: `CameraPreviewViewModel` gọi `AnalyzeCompositionUseCase` mỗi khi có
dữ liệu vision/sensor mới (qua `combine()` nhiều Flow), hiển thị qua
`GuidanceBanner` + `CompositionScoreBadge`.

File: `domain/usecase/AnalyzeCompositionUseCase.kt` (+ test),
`feature/overlay/GuidanceBanner.kt`, `feature/overlay/CompositionScoreBadge.kt`.

## Hướng dẫn kiểm thử

1. `./gradlew :domain:test` — pass toàn bộ test của `AnalyzeCompositionUseCaseTest`.
2. Mở camera: kiểm tra pinch-to-zoom + tap-to-focus hoạt động đồng thời
   không xung đột; đổi grid qua các loại xem vẽ đúng hình dạng; nghiêng máy
   xem vạch horizon phản ứng đúng chiều; hướng vào người/vật xem bounding
   box + điểm số + hướng dẫn có xuất hiện hợp lý không.
