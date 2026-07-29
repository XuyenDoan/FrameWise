# FrameWise — Phase 2: Setup Project Multi-module + CameraX Preview Cơ Bản

## 1. Đã làm gì

Đã dựng bộ khung multi-module Gradle như Phase 1 đã thống nhất, và cài đặt
xong luồng camera preview cơ bản đầu tiên — chưa có overlay, sensor, hay ML,
đúng phạm vi Phase 2.

### Cấu trúc module đã tạo

```
build-logic/convention/   Convention plugins dùng chung (Android app/library/
                           feature/compose/hilt, JVM library)
app/                       Application module — FrameWiseApplication,
                           MainActivity, NavHost
core/common/               Dispatcher qualifiers, AppResult/AppError
core/designsystem/         Dark theme kiểu Sony/Fuji/Leica (Color, Type, Theme)
core/ui/                   ShutterButton, CameraControlButton dùng chung
domain/                    Pure Kotlin — LensFacing, FlashMode, CameraState,
                           CapturedPhoto, CameraRepository (interface)
data/camera/               CameraXController (implement CameraRepository +
                           CameraPreviewBinder), Hilt DI module
feature/camerapreview/     CameraPreviewScreen (Compose), ViewModel,
                           UiState, xử lý quyền Camera
```

### Vì sao có 1 ngoại lệ so với quy tắc "feature chỉ phụ thuộc domain"

Trong Phase 1 mình đặt quy tắc `feature:* → domain`. Khi bắt tay code thực
tế, phát sinh một vấn đề: CameraX `Preview` cần được gắn (`bind`) trực tiếp
vào một `PreviewView`/`LifecycleOwner` — đây là kiểu Android UI, không thể
tồn tại trong module `domain` (module JVM thuần, không có Android SDK).

Giải pháp: tách `CameraRepository` (domain) — chỉ chứa phần **điều
khiển/trạng thái** (zoom, flash, exposure, lens, chụp ảnh) — ra khỏi phần
**gắn surface** (`CameraPreviewBinder`, sống ở `data:camera`). ViewModel vẫn
test được bình thường bằng cách fake `CameraRepository`; riêng việc gắn
surface được gọi thẳng từ Composable, nơi vốn đã là lớp UI/platform-specific.
`feature:camerapreview` vì vậy được phép thêm 1 dependency trực tiếp tới
`data:camera` — đây là ngoại lệ duy nhất, có ghi chú rõ trong code
(`CameraPreviewBinder.kt` và `build.gradle.kts` của feature module).

### Camera Pipeline đã cài

- `CameraXController` (Hilt `@Singleton`) sở hữu `ProcessCameraProvider`,
  `Preview`, `ImageCapture`.
- Binding chạy trên `Dispatchers.Main.immediate` (bắt buộc theo yêu cầu của
  CameraX), state phát ra qua `StateFlow<CameraState>` nên an toàn đọc từ
  bất kỳ thread nào.
- Hỗ trợ: camera trước/sau (`setLensFacing`), flash OFF/AUTO/ON/TORCH
  (`setFlashMode`), zoom bằng pinch gesture (`detectTransformGestures` trong
  Compose), tap... (auto focus/tap-to-focus sẽ hoàn thiện ở Phase 3 cùng
  UI controls đầy đủ), exposure (`setExposureIndex` — đã có API, chưa có UI
  slider, sẽ thêm khi làm control bar đầy đủ ở Phase 3), chụp ảnh lưu vào
  `getExternalFilesDir(Pictures)`.
- Xin quyền `CAMERA` runtime bằng `ActivityResultContracts.RequestPermission`
  (không dùng Accompanist Permissions vì chỉ cần 1 permission, thêm thư viện
  không đáng).

### Design System

`FrameWiseTheme` — nền gần đen (#0B0B0C), accent màu hổ phách (#FFB300) dùng
làm điểm nhấn duy nhất (nút chụp viền trắng kiểu máy ảnh thật, không dùng
icon Material mặc định cho nút shutter). Camera screen luôn ép dark theme
bất kể theme hệ thống — đúng quy ước app máy ảnh chuyên nghiệp.

## 2. Giới hạn quan trọng cần bạn biết

Môi trường tôi đang chạy (sandbox) **chặn truy cập `dl.google.com`** (Google
Maven repo) theo chính sách mạng, nên tôi **không build/run được** project
này bằng Gradle thật ở đây để tự kiểm chứng. Tôi đã:

- Rà lại thủ công từng file để tránh lỗi cú pháp/API rõ ràng.
- Tạo Gradle wrapper (`./gradlew`) sẵn trong repo.
- Viết 2 unit test đơn giản cho `domain` (`FlashModeTest`, `LensFacingTest`)
  để minh hoạ domain module test được độc lập, không cần Android.

Bạn cần build lần đầu trên máy có Android Studio (Giraffe/Koala trở lên,
JDK 17) để tôi hoặc bạn phát hiện và sửa nốt các lỗi biên dịch nhỏ nếu có
(luôn có khả năng có typo/API mismatch khi viết mù không compiler-check).

## 3. Hướng dẫn kiểm thử

1. Mở project bằng Android Studio (chọn thư mục gốc `FrameWise`), để Gradle
   sync (lần đầu sẽ tải dependencies từ Google/Maven Central — cần mạng
   bình thường, không bị chặn như sandbox này).
2. Chạy `./gradlew :domain:test` — phải pass 3 test (`FlashModeTest`,
   `LensFacingTest`).
3. Cắm thiết bị Android thật (khuyến nghị — CameraX cần camera thật, không
   chạy tốt trên emulator không có camera ảo cấu hình sẵn) hoặc dùng
   emulator có bật webcam ảo.
4. Chạy app (`app` module). Lần đầu mở sẽ hiện dialog xin quyền Camera →
   Cho phép.
5. Kỳ vọng thấy: preview camera sau, full màn hình, nền tối; góc trên có 2
   nút tròn mờ (flash, đổi camera); nút chụp tròn viền trắng ở dưới cùng.
6. Test: bấm nút đổi camera → chuyển trước/sau mượt, không crash. Bấm flash
   → icon đổi theo thứ tự OFF → AUTO → ON → OFF. Pinch 2 ngón để zoom. Bấm
   nút chụp → icon chuyển thành loading ngắn rồi trở lại nút chụp, ảnh được
   lưu vào `Android/data/com.framewise.app/files/Pictures/`.
7. Xoay app vào nền/quay lại (home → mở lại) → preview phải tự bind lại,
   không bị đứng hình hay crash (kiểm tra `DisposableEffect` unbind/bind
   đúng lifecycle).

## 4. Việc cần bạn xác nhận trước khi sang Phase 3

1. Build có chạy được không, có lỗi biên dịch nào cần tôi sửa không?
2. Trải nghiệm preview/flash/lens-switch/zoom/chụp ảnh có ổn không?
3. Đồng ý sang **Phase 3 — hoàn thiện Compose UI** (control bar đầy đủ:
   exposure slider, tap-to-focus, layout chuẩn theo phong cách Sony/Fuji/
   Leica) trước khi làm Grid Overlay ở Phase 4?
