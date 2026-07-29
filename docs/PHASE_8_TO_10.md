# FrameWise — Phase 8-10: Scene Recognition, AR Guidance, Voice, Capture History

## Phase 8 — Scene Recognition + Photography Tips

- `data:vision`: thêm ML Kit **Image Labeling** (`SceneClassifier.kt`), chạy
  **throttled mỗi 15 frame** (không phải mọi frame) vì cảnh vật ít khi đổi
  nhanh như vị trí chủ thể — tránh cạnh tranh tài nguyên với Face/Object
  Detection vốn cần gần real-time.
- Mapping nhãn ML Kit → `SceneType` là **heuristic theo từ khóa**, không
  phải model scene-classification chuyên dụng (ML Kit Image Labeling mặc
  định là tập ~400 nhãn kiểu ImageNet, không có nhãn "hoàng hôn"/"ban đêm"
  trực tiếp). Đã ghi rõ giới hạn này trong KDoc của `SceneClassifier`.
- `GetPhotographyTipsUseCase` (domain, pure function): mẹo chụp tiếng Việt
  theo từng `SceneType`, có unit test.
- UI: `SceneBadge` + `PhotographyTipCaption` hiển thị giữa khung hình khi
  nhận diện được cảnh.

## Phase 9 — AR Guidance Arrows + Voice Assistant

- `ArGuidanceArrow` (feature:overlay): mũi tên chỉ hướng vẽ bằng Canvas
  thuần (trái/phải/lên/xuống/tiến/lùi/xoay ngang), có hiệu ứng pulse nhẹ.
  Không hiện gì khi bố cục đã đẹp (`GuidanceType.GOOD`).
- `VoiceGuidanceSpeaker` (feature:camerapreview): bọc `android.speech.tts.TextToSpeech`,
  tiếng Việt. ViewModel chỉ gọi `speak()` khi **cặp (bật/tắt, hướng dẫn ưu
  tiên cao nhất) thực sự đổi** (`distinctUntilChanged`), tránh đọc liên tục/
  chồng chéo — đúng góp ý đã nêu ở Phase 1. Có nút bật/tắt riêng, mặc định
  **tắt** (opt-in, không làm phiền người dùng).

## Phase 10 — Capture History ("Before/After")

**Điều chỉnh so với đặc tả gốc:** "Before/After" ban đầu được hiểu là so
sánh khung hình hiện tại với khung hình do AI đề xuất — việc này cần một
mô hình sinh ảnh/tái bố cục (generative), ngoài phạm vi hợp lý của các
phase này. Thay vào đó, tôi làm **Capture History**: mỗi lần chụp được lưu
lại kèm điểm bố cục tại đúng thời điểm bấm máy; màn hình lịch sử cho so
sánh 2 lần chụp gần nhất cạnh nhau (ảnh + điểm), cộng danh sách đầy đủ các
lần chụp trong phiên. Đây là điều thực sự đo lường được sự "tiến bộ" giữa
các lần chụp, đúng tinh thần "so sánh trước/sau" mà không cần bịa ra kết
quả AI không có thật.
- Lưu trong bộ nhớ (`ViewModel`), không cần database — mất khi thoát app,
  chấp nhận được cho phạm vi hiện tại.
- Thumbnail decode bằng `BitmapFactory` trên `Dispatchers.IO`, không thêm
  thư viện load ảnh (Coil...) vì chỉ cần hiển thị vài ảnh nhỏ trong phiên.

## Pose Assistant (MediaPipe Pose Landmarker) — bổ sung sau

Đã triển khai ở lượt tiếp theo (xem `docs/POSE_ASSISTANT.md` để biết chi
tiết + giới hạn quan trọng cần biết trước khi test, đặc biệt việc app cần
mạng ở lần chạy đầu để tải model MediaPipe).

## Việc CHƯA làm (từ roadmap Phase 1) và vì sao

- **Horizon line detection trong ảnh** (khác sensor): vẫn dùng sensor góc
  nghiêng, chưa dò đường chân trời thật trong khung hình bằng thị giác máy
  tính — đã ghi trong `docs/COMPOSITION_CHECKLIST.md`.

## Hướng dẫn kiểm thử

1. `./gradlew :domain:test` — toàn bộ unit test (composition engine +
   photography tips) phải pass.
2. Cài APK mới (build lại qua GitHub Actions), mở camera:
   - Hướng máy vào đồ ăn/khuôn mặt/hoa... → xem badge cảnh + mẹo chụp có
     xuất hiện và hợp lý không (nhớ đây là heuristic, có thể sai với vật
     thể không rõ ràng).
   - Bật icon loa (Voice) → nghe hướng dẫn đọc bằng tiếng Việt khi di
     chuyển máy; đổi hướng liên tục xem có bị đọc chồng/lặp lãng phí
     không.
   - Xem mũi tên AR ở giữa khung hình có đúng hướng với text hướng dẫn bên
     dưới không.
   - Chụp 2-3 ảnh liên tiếp ở các góc khác nhau → bấm icon History → xem
     so sánh điểm số 2 ảnh gần nhất + danh sách đầy đủ.
