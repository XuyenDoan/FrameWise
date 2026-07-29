# Checklist bố cục theo tiêu chí giải ảnh uy tín — đối chiếu với FrameWise

> Tài liệu này liệt kê các tiêu chí bố cục thường thấy trong tiêu chí chấm
> giải của các cuộc thi/giải thưởng nhiếp ảnh uy tín (Sony World Photography
> Awards, International Photography Awards, National Geographic Photo
> Contest, Nikon Photo Contest...) cho 2 thể loại **Chân dung** và **Phong
> cảnh**, rồi đối chiếu từng tiêu chí với tính năng FrameWise hiện có.
>
> **Quan trọng:** tôi (Claude) không có camera/thiết bị vật lý để tự chụp
> ảnh bằng app và chấm điểm — checklist dưới đây là **đối chiếu ở mức thiết
> kế/tính năng** (feature này có giải quyết được tiêu chí này về mặt kỹ
> thuật hay không), không phải kết quả đo trên ảnh thật. Sau khi bạn cài
> APK và chụp thử, gửi ảnh lại, tôi sẽ chấm trực tiếp theo đúng checklist
> này.

## Checklist — Chân dung (Portrait)

| # | Tiêu chí | FrameWise hỗ trợ? | Ghi chú |
|---|---|---|---|
| 1 | Lấy nét sắc vào mắt/khuôn mặt chủ thể | ⚠️ Một phần | Tap-to-focus đã có; auto lấy nét ưu tiên vào face bounding box (ML Kit Face Detection) **chưa** tự động — hiện người dùng phải tự tap vào mắt |
| 2 | Bố cục theo quy tắc 1/3 hoặc có chủ đích rõ ràng, không đặt giữa ngẫu nhiên | ✅ Có | `AnalyzeCompositionUseCase` so vị trí chủ thể với 4 giao điểm 1/3, sinh guidance MOVE_LEFT/RIGHT/RAISE/LOWER_CAMERA |
| 3 | Khoảng trống đầu (headroom) hợp lý | ✅ Có | Khi chủ thể là FACE (ML Kit Face Detection), `AnalyzeCompositionUseCase` tính khoảng trống phía trên khuôn mặt so với mép khung, so với ngưỡng 5%-15% chiều cao khung → RAISE_CAMERA/LOWER_CAMERA |
| 4 | Hậu cảnh không gây xao nhãng, tách chủ thể khỏi nền | ⚠️ Một phần | MediaPipe Selfie Segmentation (`BackgroundSegmentationProcessor`) ước lượng độ "rối" hậu cảnh qua độ gồ ghề của mask tách nền/chủ thể, sinh guidance `BUSY_BACKGROUND`. **Đây là proxy gián tiếp** (đo hình dạng mask, không đo trực tiếp độ rối hình ảnh thật) — xem giới hạn trong `BackgroundSegmentationProcessor.kt` |
| 5 | Ánh sáng có hướng rõ, không ngược sáng làm mất chi tiết mặt | ✅ Có | `LuminanceEvaluator` so độ sáng vùng chủ thể vs toàn khung → phát hiện BACKLIT, sinh guidance IMPROVE_LIGHTING |
| 6 | Đường chân trời (nếu có) phải thẳng | ✅ Có | `HorizonSensorController` (rotation vector sensor) + `HorizonLevelOverlay` + guidance LEVEL_HORIZON |
| 7 | Biểu cảm/tư thế tự nhiên | ❌ Chưa | Đây là Pose Assistant (Phase 9 trong roadmap Phase 1, dùng MediaPipe Pose Landmarker) — chưa làm |
| 8 | Độ tương phản & phơi sáng vùng da hài hòa, không cháy sáng/thiếu sáng | ✅ Có | `LuminanceEvaluator` phát hiện UNDEREXPOSED/OVEREXPOSED tổng thể; **chưa** phân tích riêng vùng da/tone da |
| 9 | Không cắt cụt gây khó chịu (khớp tay/chân) | ❌ Chưa | Cần phân tích pose/khung xương để biết "khớp" nằm ở đâu — thuộc Pose Assistant, chưa làm |

**Điểm chân dung: 5/9 tiêu chí có hỗ trợ kỹ thuật đầy đủ, 2/9 hỗ trợ một phần, 2/9 chưa làm.**

## Checklist — Phong cảnh (Landscape)

| # | Tiêu chí | FrameWise hỗ trợ? | Ghi chú |
|---|---|---|---|
| 1 | Đường chân trời phải thẳng tuyệt đối | ✅ Có | Horizon sensor + overlay + guidance, đây là tiêu chí hay bị loại nhất trong các giải nên được ưu tiên làm sớm (Phase 5) |
| 2 | Đặt đường chân trời ở 1/3 trên hoặc dưới khung (không đặt giữa trừ khi phản chiếu đối xứng chủ đích) | ⚠️ Một phần | `HorizonLineDetector` (heuristic gradient độ sáng theo hàng ảnh, không phải model line-detection thật) ước lượng vị trí đường chân trời khi không có chủ thể chính, `AnalyzeCompositionUseCase` sinh guidance đưa nó về gần 1/3. **Giới hạn quan trọng:** detector quét theo trục chưa chuẩn hoá xoay ảnh (rotation) như ML Kit — xem ghi chú trong `HorizonLineDetector.kt` — độ chính xác thực tế trên máy cầm dọc (phổ biến nhất) chưa được kiểm chứng |
| 3 | Có tiền cảnh (foreground interest) tạo chiều sâu | ❌ Chưa | Cần depth estimation hoặc phân vùng ngữ nghĩa (semantic segmentation foreground/background) — chưa làm |
| 4 | Có leading lines dẫn mắt vào khung | ❌ Chưa | Cần edge/line detection (OpenCV hoặc mô hình riêng) — ngoài phạm vi hiện tại |
| 5 | Bầu trời không cháy trắng mất chi tiết mây | ✅ Có (một phần) | `LuminanceEvaluator` phát hiện OVEREXPOSED tổng thể; **chưa** khoanh vùng riêng phần bầu trời để cảnh báo cục bộ |
| 6 | Vùng tối không mất chi tiết | ✅ Có (một phần) | Tương tự — phát hiện UNDEREXPOSED tổng thể, chưa phân vùng cục bộ |
| 7 | Chủ thể chính không đặt giữa khung ngẫu nhiên | ✅ Có | Rule-of-thirds guidance áp dụng chung cho mọi loại chủ thể ML Kit phát hiện được (object detection), không riêng gì phong cảnh |
| 8 | Không có vật thể gây xao nhãng bị cắt cụt ở viền khung | ❌ Chưa | Cần phát hiện vật thể phụ ở rìa khung và cảnh báo riêng — chưa làm |
| 9 | Cân bằng trọng lượng thị giác giữa các phần khung hình | ❌ Chưa | Đây là tiêu chí "nghệ thuật" khó lượng hoá, cần mô hình composition-score phức tạp hơn nhiều (ngoài phạm vi rule-based hiện tại) |

**Điểm phong cảnh: 3/9 tiêu chí có hỗ trợ đầy đủ, 3/9 hỗ trợ một phần, 3/9 chưa làm.**

## Tóm tắt trung thực

FrameWise ở trạng thái hiện tại (sau Phase 2-7) giải quyết tốt nhóm tiêu chí
**"cơ học, đo được trực tiếp bằng sensor/ML Kit sẵn có"**: đường chân trời
thẳng, chủ thể theo quy tắc 1/3, phơi sáng cơ bản. Đây đúng là nhóm lỗi phổ
biến nhất khiến ảnh nghiệp dư bị loại ở vòng đầu của các giải, nên giá trị
thực tế không nhỏ.

Nhóm tiêu chí còn thiếu đều là những tiêu chí **mang tính nghệ thuật/ngữ
nghĩa cao** (headroom chuẩn, tách nền, leading lines, pose tự nhiên, cân
bằng thị giác) — đòi hỏi mô hình thị giác máy tính phức tạp hơn nhiều (phân
vùng ảnh, ước lượng độ sâu, phát hiện đường nét, phân tích dáng người chi
tiết). Scene Recognition, Pose Assistant (MediaPipe), Horizon line
detection trong ảnh, và Semantic Segmentation tách nền (một phần) đều đã
được bổ sung ở các lượt sau — xem `docs/PHASE_8_TO_10.md`,
`docs/POSE_ASSISTANT.md`, `docs/SEMANTIC_SEGMENTATION.md`.

**Đề xuất tiếp theo** nếu muốn tăng % tiêu chí đạt được: leading lines
detection (edge/line detection cho tiêu chí "đường dẫn mắt") và cân bằng
thị giác tổng thể — cả hai đều cần mô hình/heuristic thị giác máy tính
phức tạp hơn, chưa làm.
