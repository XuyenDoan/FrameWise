# FrameWise — Trạng thái dự án (tính đến lần dừng gần nhất)

> Đọc file này đầu tiên khi quay lại dự án. Tóm tắt: đã làm gì, còn thiếu
> gì, và cần làm gì tiếp theo.

## Repo & branch

- Repo: `XuyenDoan/FrameWise`
- Branch đang phát triển: `claude/ai-photography-assistant-a1is53`
- CI: `.github/workflows/build-apk.yml` — tự build APK debug + chạy
  `:domain:test` mỗi khi push lên branch này, upload artifact
  `framewise-debug-apk` (xem tab Actions trên GitHub để tải).

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

**Cập nhật: CI đã XANH.** Commit `d4054f2` (Semantic Segmentation) build
thành công ngay lần đầu — cả `:domain:test` (nay có thêm test cho
`BUSY_BACKGROUND`) lẫn `:app:assembleDebug`, kể cả API MediaPipe
`ImageSegmenter` mới (`ByteBufferExtractor`, `categoryMask().orElse(null)`,
`ImageSegmenterOptions.builder()`). Đây là commit mới nhất trên branch
tính đến lúc ghi chú này.

Luôn kiểm tra trạng thái build của commit mới nhất trên GitHub Actions
trước khi giả định branch đang ở trạng thái build được.

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
