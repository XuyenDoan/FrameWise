# FrameWise — Trạng thái dự án (tính đến lần dừng gần nhất)

> Đọc file này đầu tiên khi quay lại dự án. Tóm tắt: đã làm gì, còn thiếu
> gì, và cần làm gì tiếp theo.

## Repo & branch

- Repo: `XuyenDoan/FrameWise`
- Branch đang phát triển: `claude/ai-photography-assistant-a1is53`
- CI: `.github/workflows/build-apk.yml` — tự build APK debug + chạy
  `:domain:test` mỗi khi push lên branch này, upload artifact
  `framewise-debug-apk` (xem tab Actions trên GitHub để tải).

## Chế độ chụp + chọn chủ thể + trợ giúp người mới (2026-07-29)

Bạn phản hồi app chưa hỗ trợ sâu (không giải thích quy tắc, không có chế
độ riêng chân dung/thú vật/phong cảnh, không chọn được ai để lấy nét). Đã
làm xong, xem chi tiết đầy đủ (thiết kế, giới hạn kỹ thuật, cách test) ở
**`docs/SHOOTING_MODES.md`**. Tóm tắt nhanh:

- Thanh chọn chế độ **Tự động/Chân dung/Thú cưng/Phong cảnh** (thủ công,
  người dùng tự chọn) — đổi cách app ưu tiên chọn chủ thể + tắt/bật một số
  quy tắc gợi ý theo chế độ.
- **Chạm vào khung nhận diện** (khuôn mặt/vật thể) trên preview để chọn chủ
  thể → lấy nét thật vào đúng người/vật đó + ưu tiên tuyệt đối chủ thể đó
  cho mọi gợi ý bố cục. Dựa trên tracking ID thật của ML Kit (không phải
  app tự đoán) — nếu ML Kit mất tracking, lựa chọn tự huỷ, không đoán bừa.
- Nút (?) mở hộp thoại giải thích tĩnh: quy tắc 1/3, tỷ lệ vàng, headroom,
  đường chân trời, cách dùng chế độ/chọn chủ thể — nội dung viết sẵn, không
  gọi AI ngoài.
- **Giới hạn cần biết:** ML Kit không phân biệt được loài vật, nên "chế độ
  Thú cưng" chỉ ưu tiên "vật thể không phải khuôn mặt người", không nhận
  diện đúng là con gì.

**Chưa test được trên thiết bị thật** — đặc biệt là độ ổn định của tracking
ID khi chọn chủ thể (subject có "theo" đúng khi di chuyển máy nhẹ không).

**Lỗi bạn báo tiếp theo, đã sửa:** chọn chế độ Chân dung nhưng app hiện gợi
ý "thú cưng" — do badge/tip trước đó lấy từ `SceneType` tự nhận diện (ML
Kit), độc lập hoàn toàn với `ShootingMode` tự chọn. Đã sửa: chế độ thủ công
giờ ghi đè tuyệt đối gợi ý tự động, badge cảnh tự động chỉ hiện ở chế độ Tự
động. Cũng đổi tên nút "Thú vật" → "Thú cưng" theo yêu cầu. Chi tiết đầy đủ
ở `docs/SHOOTING_MODES.md` mục "Lỗi đã sửa: gợi ý tự động mâu thuẫn với chế
độ đã chọn".

## Đã làm xong (Phase 1 → 10 + Pose Assistant)

| Phase | Nội dung | Tài liệu |
|---|---|---|
| 1 | Phân tích yêu cầu, kiến trúc Clean Architecture + MVVM, multi-module | `docs/PHASE_1_ARCHITECTURE.md` |
| 2 | Setup project multi-module + CameraX Preview cơ bản | `docs/PHASE_2_CAMERAX_SETUP.md` |
| 3 | Camera controls đầy đủ (exposure slider, tap-to-focus) | `docs/PHASE_3_TO_7.md` |
| 4 | Grid Overlay (Rule of Thirds/Golden Ratio/Golden Triangle/Square/Diagonal) vẽ bằng Canvas | `docs/PHASE_3_TO_7.md` |
| 5 | Horizon Level (rotation vector sensor) | `docs/PHASE_3_TO_7.md` |
| 6 | ML Kit Face + Object Detection, bounding box overlay | `docs/PHASE_3_TO_7.md` |
| 7 | Composition Engine (rule of thirds, headroom, lighting, score 0-100) | `docs/PHASE_3_TO_7.md` |
| 8 | Scene Recognition (ML Kit Image Labeling) + Photography Tips | `data/vision/SceneClassifier.kt`, `domain/usecase/GetPhotographyTipsUseCase.kt` |
| 9 | AR Guidance arrows (Canvas) + Voice Assistant (TextToSpeech, debounce) | `feature/overlay/ArGuidanceArrow.kt`, `feature/camerapreview/VoiceGuidanceSpeaker.kt` |
| 10 | Capture History ("before/after" theo điểm số, không phải AI sinh ảnh) | `feature/camerapreview/CaptureHistoryPanel.kt` |
| Pose Assistant | MediaPipe Pose Landmarker — gợi ý "thả lỏng vai"/"ngẩng cằm" | `docs/POSE_ASSISTANT.md` |
| Horizon line detection | Ước lượng vị trí đường chân trời trong ảnh (heuristic gradient độ sáng), đưa về gần 1/3 khi không có chủ thể chính | `data/vision/HorizonLineDetector.kt` (có ghi rõ giới hạn về rotation) |
| Semantic Segmentation | MediaPipe Selfie Segmentation ước lượng độ "rối" hậu cảnh (proxy gián tiếp qua hình dạng mask), guidance `BUSY_BACKGROUND` | `docs/SEMANTIC_SEGMENTATION.md` |

Toàn bộ tài liệu chi tiết từng phase nằm trong `docs/`:
- `PHASE_1_ARCHITECTURE.md`, `PHASE_2_CAMERAX_SETUP.md`, `PHASE_3_TO_7.md`,
  `PHASE_8_TO_10.md`, `POSE_ASSISTANT.md`, `SEMANTIC_SEGMENTATION.md`,
  `COMPOSITION_CHECKLIST.md` (đối chiếu tiêu chí giải ảnh uy tín Sony
  WPA/IPA với tính năng app).

## Trạng thái build

Sandbox môi trường Claude (nơi code này được viết) **không có Android SDK
và bị chặn Google Maven** → không tự build/verify được. Toàn bộ việc build
thật dựa vào GitHub Actions (`build-apk.yml`), nơi đã phát hiện và tôi đã
sửa **5 lỗi biên dịch thật** qua nhiều lần lặp:

1. `targetSdk` không tồn tại trên `LibraryExtension` (build-logic)
2. Thiếu khai báo `apply false` plugin AGP/Kotlin/KSP/Hilt ở root `build.gradle.kts`
3. Cú pháp type-safe accessor `libs.androidx.xxx` lỗi toàn project → đổi sang `libs.findLibrary(...)`
4. Truth's `StringSubject` không có `.isNotBlank()` (unit test)
5. Thiếu `import androidx.compose.runtime.getValue` cho `by` delegate trong `ArGuidanceArrow.kt`

**Cập nhật: CI đã XANH.** Commit `5abe75d` (sửa lỗi gợi ý tự động mâu thuẫn
với chế độ đã chọn + đổi "Thú vật" → "Thú cưng") build thành công — cả
`:domain:test` lẫn `:app:assembleDebug`. Đây là commit mới nhất trên
branch tính đến lúc ghi chú này. Chi tiết tính năng: xem
`docs/SHOOTING_MODES.md`, "Sửa lỗi nút chụp ảnh" và "Audit layout/logic
toàn app" bên dưới.

Luôn kiểm tra trạng thái build của commit mới nhất trên GitHub Actions
trước khi giả định branch đang ở trạng thái build được.

## Sửa lỗi nút chụp ảnh (2026-07-29)

Bạn báo bấm nút chụp không thấy lưu ảnh. Kiểm tra code xác nhận **đây là 2
lỗi thật**, không phải thiếu tính năng:

1. `capturePhoto()` (`CameraXController.kt`) lưu ảnh vào
   `getExternalFilesDir(Pictures)` — thư mục **riêng của app**, không được
   quét vào MediaStore nên **không hiện trong Gallery/Thư viện ảnh** dù
   thực tế file vẫn được ghi ra đĩa mỗi lần chụp.
2. `CameraPreviewViewModel` đã emit `captureEvents` (thành công/thất bại)
   nhưng **không có nơi nào trong UI lắng nghe** — bấm chụp xong không có
   thông báo gì, giống như nút không hoạt động.

**Đã sửa:**
- Lưu ảnh qua `MediaStore` (`Pictures/FrameWise`, dùng `RELATIVE_PATH` từ
  API 29+) — giờ ảnh hiện trong Gallery như ảnh chụp bằng app Camera bình
  thường. Đổi `CapturedPhoto.filePath` → `uri` (giờ là content:// URI, lan
  ra `CaptureHistoryEntry`/`CaptureEvent`); `CaptureHistoryPanel` đọc ảnh
  qua `ContentResolver.openInputStream()` thay vì `BitmapFactory.decodeFile()`.
- Thêm quyền `WRITE_EXTERNAL_STORAGE` (`maxSdkVersion="28"`) — cần cho ghi
  MediaStore trên máy Android 8-9 (API 26-28); từ Android 10 trở lên
  (scoped storage) không cần quyền này. Xin cùng lúc với quyền Camera
  (`CameraPermissionState.kt`), chỉ trên các máy API <29.
- `CameraPreviewRoute` giờ lắng nghe `captureEvents` và hiện Snackbar "Đã
  lưu ảnh vào thư viện" / báo lỗi cụ thể nếu chụp thất bại.

**Build CI xanh ở commit `3fbcb07`.** Vẫn cần bạn xác nhận trên máy thật:
chụp ảnh → có hiện Snackbar không → mở Gallery xem ảnh có xuất hiện trong
album "FrameWise" không.

## Audit layout/logic toàn app (2026-07-29)

Bạn yêu cầu kiểm tra tổng thể bố trí UI trên nhiều máy Android khác nhau và
chức năng có đúng không. **Sandbox không có emulator/thiết bị thật** (đã
kiểm tra: không có `adb`, `emulator`, không `$ANDROID_HOME`) nên **không
thể tự chạy app để test bằng mắt/chụp màn hình**. Đã làm thay bằng
**rà soát code tĩnh toàn bộ** (đọc hết mọi Composable màn hình camera,
overlay, banner, panel + mọi ViewModel/UseCase). Kết quả:

- **Lỗi layout thật đã sửa**: `GridTypePicker` (`CameraPreviewScreen.kt`)
  trước đây là `LazyRow` rộng cố định 280dp chèn thẳng vào Row các nút
  điều khiển kiểu `SpaceBetween` → trên máy màn hình hẹp sẽ tràn, đè lên
  badge điểm số/nút đổi camera. Đã sửa: đưa picker ra `Popup` nổi bên dưới
  nút Grid (không còn tham gia layout của Row), giới hạn `widthIn(max =
  260.dp)` thay vì width cố định.
- **Lỗi logic thật đã sửa**: `Preview` và `ImageAnalysis` trước đây bind
  độc lập, không chia sẻ vùng crop — `Preview` không set aspect ratio (bám
  theo tỉ lệ màn hình thực tế) trong khi `ImageAnalysis` cố định 4:3 (qua
  `setTargetResolution(640, 480)`). Trên màn hình càng lệch xa 4:3 (hầu hết
  điện thoại hiện nay), toạ độ overlay (bounding box, grid...) tính từ khung
  phân tích sẽ lệch khỏi vị trí thật trên preview hiển thị. Đã sửa bằng
  `UseCaseGroup` + `ViewPort` lấy từ `PreviewView` — đây là cách CameraX
  chính thức khuyến nghị cho đúng bài toán này (`CameraXController.kt`).
- **Đã kiểm tra và thấy ổn** (không phải lỗi): toàn bộ `when` exhaustive
  trên `GuidanceType`/`SceneType`/`FlashMode`/`HorizonLevel`/`GridType`/
  `PoseSuggestion`; `WindowInsets` (status bar/navigation bar) đã xử lý ở
  cột điều khiển chính và panel lịch sử; `CaptureHistoryPanel` dùng
  `LazyColumn` nên tự cuộn, không tràn; các chuỗi `combine()` trong
  `CameraPreviewViewModel` không có dấu hiệu lệch/stale value.
- **Quan trọng — vẫn cần xác nhận trên thiết bị thật**: cả 2 sửa lỗi trên
  đúng theo API/pattern chính thức của Compose và CameraX, đã qua CI biên
  dịch thành công (commit `f6f7bb8`), nhưng **tôi chưa thể tự mắt kiểm
  chứng trên nhiều kích thước máy thật** (máy nhỏ ~5", máy màn hình cao,
  máy có notch/cutout, tablet/foldable). Khi bạn cài APK, ưu tiên kiểm tra:
  1. Mở Grid picker trên máy màn hình hẹp — không còn bị đè/tràn.
  2. Bounding box quanh khuôn mặt/vật thể có bám đúng vị trí thật trên
     preview không (trước đây có nguy cơ lệch, nhất là gần viền khung).

## Việc CHƯA làm / cần người dùng xác nhận

- **Test trên thiết bị thật**: chưa có xác nhận app chạy đúng trên máy
  Android thật (camera preview, ML Kit, MediaPipe, sensor...). Cần bạn cài
  APK và báo lại.
- **Pose Assistant cần mạng ở lần chạy đầu** để tải model MediaPipe
  (~5-9MB) — chưa kiểm chứng được URL tải model có đúng/còn hoạt động hay
  không (xem `docs/POSE_ASSISTANT.md`).
- **Horizon line detection trong ảnh**: đã làm (heuristic, không phải model
  thật) — nhưng có **giới hạn quan trọng chưa kiểm chứng được**: detector
  quét theo trục chưa chuẩn hoá xoay ảnh (rotation) như ML Kit đã làm ở chỗ
  khác trong cùng file `MlKitFrameAnalyzer.kt`, nên trên máy cầm dọc (phổ
  biến nhất khi chụp) có thể quét sai trục. Cần xác nhận trên thiết bị
  thật rồi sửa nếu sai — xem ghi chú trong `data/vision/HorizonLineDetector.kt`.
- **Semantic Segmentation cần mạng ở lần chạy đầu** để tải model
  `selfie_segmenter.tflite` — giống Pose Assistant, chưa kiểm chứng URL
  model trên thiết bị thật. Đây cũng chỉ là **proxy gián tiếp** (đo hình
  dạng mask, không đo trực tiếp độ rối hình ảnh thật) — xem
  `docs/SEMANTIC_SEGMENTATION.md`.
- **Checklist bố cục** (`docs/COMPOSITION_CHECKLIST.md`) vẫn còn tiêu chí
  "nghệ thuật" chưa làm được: leading lines, cân bằng thị giác tổng thể —
  đây đều là các mô hình thị giác máy tính phức tạp hơn nhiều, cần phase
  riêng nếu muốn làm tiếp.
- **Chưa test được UI thực tế** (không có màn hình/thiết bị để chạy Compose
  Preview hay app thật trong môi trường viết code này).

## Cách quay lại

Chỉ cần nhắn tiếp tục — toàn bộ context/quyết định kiến trúc đã ghi trong
`docs/`, không cần giải thích lại từ đầu. Có thể yêu cầu cụ thể như:
- "Kiểm tra CI mới nhất, sửa lỗi nếu còn"
- "Làm Horizon line detection trong ảnh"
- "Tôi đã test APK, đây là ảnh chụp, chấm theo checklist"
- "Làm PR để merge nhánh này"
