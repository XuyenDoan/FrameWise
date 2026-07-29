# Chế độ chụp (Chân dung/Thú vật/Phong cảnh) + chọn chủ thể + trợ giúp người mới

## Bối cảnh

Bạn phản hồi: app chưa hỗ trợ sâu — không giải thích quy tắc 1/3/tỷ lệ vàng
là gì, không có chế độ riêng cho chân dung/thú vật/phong cảnh, không chọn
được ai/vật gì để lấy nét khi nhiều chủ thể trong khung. Bạn chọn các
phương án cụ thể qua các câu hỏi làm rõ:
- Chế độ: **người dùng tự chọn** (không tự động đoán).
- Chọn chủ thể: **chạm vào khung nhận diện** trên preview.
- "AI hỗ trợ": **giải thích quy tắc bằng nội dung có sẵn trong app** (không
  gọi API AI ngoài — không cần mạng/API key) + **bố cục gợi ý thông minh
  hơn theo từng chế độ**.
- Người mới: **tooltip ngay trên màn hình chụp** (không làm onboarding
  nhiều trang riêng).

## Chế độ chụp (`ShootingMode`)

4 lựa chọn: **Tự động / Chân dung / Thú vật / Phong cảnh** — thanh chip
cuộn ngang, luôn hiện ngay dưới hàng nút điều khiển trên cùng. Đây là lựa
chọn **thủ công**, tách biệt với `SceneType` (nhãn cảnh app tự nhận diện
qua ML Kit Image Labeling, chỉ hiển thị làm badge/gợi ý, không ảnh hưởng
quy tắc chấm điểm).

Chế độ thay đổi cách `AnalyzeCompositionUseCase` chọn **chủ thể chính** khi
người dùng chưa tự chạm chọn, và quy tắc nào được áp dụng:

| Chế độ | Ưu tiên chọn chủ thể tự động | Quy tắc bị tắt |
|---|---|---|
| Chân dung | Ưu tiên khuôn mặt (FACE) nếu có, kể cả khi có vật thể lớn hơn trong khung | — |
| Thú vật | Ưu tiên chủ thể **không phải khuôn mặt người** (ML Kit không phân biệt được loài — xem giới hạn bên dưới) | — |
| Phong cảnh | Không ưu tiên gì (vẫn "vật lớn nhất thắng" như trước) | Tắt gợi ý "hậu cảnh rối" (`BUSY_BACKGROUND`) — phong cảnh thường muốn giữ nguyên toàn cảnh, không cần tách nền |
| Tự động | Giữ nguyên hành vi cũ (vật lớn nhất thắng) | — |

**Quan trọng — giới hạn thật, không phải thiếu sót:** ML Kit (thư viện
nhận diện đang dùng, unbundled Object Detection) **không phân biệt được
loài vật** (chó/mèo/chim...) — chỉ trả về nhãn chung "Vật thể". Nên "chế độ
Thú vật" hoạt động bằng cách ưu tiên chọn "vật thể không phải khuôn mặt
người" làm chủ thể chính, chứ **không** thể nói "đây là con mèo" hay tối ưu
riêng theo loài. Xem KDoc `SubjectLabel` trong domain để biết chi tiết.

## Chọn chủ thể lấy nét (chạm vào khung nhận diện)

Mỗi khuôn mặt/vật thể phát hiện được có khung bao quanh (đã có sẵn từ
trước). Giờ **chạm vào khung nào thì**:
1. Camera **lấy nét thật** vào tâm khung đó (gọi `focusAt` — hành vi lấy
   nét vật lý, không chỉ là gợi ý bố cục).
2. `AnalyzeCompositionUseCase` **ưu tiên tuyệt đối** chủ thể đó cho mọi gợi
   ý bố cục (bỏ qua chế độ ưu tiên tự động ở trên), khung của nó chuyển
   sang màu xanh + viền dày hơn để phân biệt.
3. Chạm lại vào khung đang chọn để **bỏ chọn**, quay về chế độ tự động.
4. Đổi chế độ chụp sẽ **tự huỷ lựa chọn thủ công** (vì tiêu chí ưu tiên tự
   động đã đổi theo chế độ mới, giữ lựa chọn cũ có thể không còn hợp lý).

### Giới hạn kỹ thuật quan trọng

Việc "theo chủ thể qua các khung hình" dựa vào **tracking ID thật của ML
Kit** (`Face.enableTracking()`, Object Detection tự tracking sẵn ở
`STREAM_MODE`) — không phải app tự đoán. Nghĩa là:
- Nếu ML Kit **mất tracking** (chủ thể ra khỏi khung, bị che khuất, ánh
  sáng thay đổi đột ngột...), lựa chọn **tự động huỷ** và quay về chế độ tự
  động — đây là hành vi fail-safe có chủ đích, **không** đoán bừa chủ thể
  nào khác.
- Trường hợp này **chưa được kiểm chứng trên thiết bị thật** (sandbox viết
  code không có camera) — cần bạn test thực tế: chạm chọn 1 người trong
  nhóm nhiều người, di chuyển camera nhẹ, xem lựa chọn có "theo" đúng người
  đó không.

## Trợ giúp người mới (glossary tĩnh)

Nút (?) trong thanh chọn chế độ mở hộp thoại giải thích ngắn gọn: quy tắc
1/3, tỷ lệ vàng, khoảng trống đầu (headroom), đường chân trời, cách dùng
chế độ chụp + chọn chủ thể. **Đây là nội dung tĩnh viết sẵn trong app**
(không gọi AI/API ngoài, không cần mạng, không phát sinh chi phí) — đúng
theo lựa chọn của bạn ("giải thích quy tắc bằng AI" hiểu là nội dung diễn
giải sẵn có, không phải gọi LLM thật).

## Đối chiếu với checklist giải ảnh

Cập nhật `docs/COMPOSITION_CHECKLIST.md` — tiêu chí Chân dung #1 ("lấy nét
sắc vào mắt/khuôn mặt") giờ có thêm lựa chọn chạm-để-lấy-nét chủ động, tuy
auto-focus-vào-mắt (eye-level autofocus) hoàn toàn tự động vẫn chưa có (ML
Kit Face Detection trả bounding box khuôn mặt, không trả toạ độ mắt riêng
lẻ).

## Việc CHƯA làm / cần test trên máy thật

- Tracking ID có ổn định thực tế không (xem giới hạn ở trên).
- Chế độ Thú vật với vật thể thật (chó/mèo/chim) — ML Kit có nhận diện
  được bounding box chính xác không (chỉ biết đó KHÔNG phải khuôn mặt
  người, còn độ chính xác khung bao vật thể phụ thuộc hoàn toàn vào ML Kit
  Object Detection, chưa kiểm chứng với ảnh động vật thật).
- Hộp thoại trợ giúp hiển thị đúng, dễ đọc trên các kích thước màn hình
  khác nhau chưa (chỉ rà soát code tĩnh, xem `docs/STATUS.md` mục audit).
