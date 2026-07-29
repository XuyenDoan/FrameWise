# Chọn độ phân giải ảnh chụp

## Bối cảnh

Bạn báo ảnh chụp ra "dung lượng nhẹ, không giữ được chi tiết". Kiểm tra
code xác nhận **đây là lỗi thật**: `ImageCapture` (`CameraXController.kt`)
trước đó không hề chỉ định độ phân giải (`ImageCapture.Builder()` không có
`ResolutionSelector`), nên CameraX được tự do chọn độ phân giải theo tiêu
chí nội bộ của nó — kết hợp với `CAPTURE_MODE_MINIMIZE_LATENCY` (ưu tiên
tốc độ chụp hơn chất lượng), thực tế có thể chọn độ phân giải thấp hơn hẳn
mức tối đa máy hỗ trợ.

## Đã sửa

1. **Mặc định chụp ở độ phân giải cao nhất** — dùng
   `ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY` (API chính thức của
   CameraX cho đúng yêu cầu này), đổi `CAPTURE_MODE_MINIMIZE_LATENCY` →
   `CAPTURE_MODE_MAXIMIZE_QUALITY`.
2. **Thêm lựa chọn độ phân giải thủ công** — nút chip cạnh thanh chọn chế
   độ chụp, mở danh sách **độ phân giải thật mà camera máy bạn hỗ trợ**
   (đọc trực tiếp từ `CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP`
   qua Camera2 interop, không phải danh sách cứng viết sẵn trong app —
   mỗi máy sẽ thấy danh sách khác nhau tuỳ phần cứng thật).
3. Đổi camera trước/sau sẽ **reset về độ phân giải cao nhất** của camera đó
   (độ phân giải camera trước và sau có thể khác nhau, giữ lựa chọn cũ có
   thể không còn hợp lệ).

## Giới hạn kỹ thuật cần biết

- Dùng Camera2 interop (`Camera2CameraInfo`) để liệt kê độ phân giải — đây
  là API chính thức của CameraX 1.4.0 cho đúng nhu cầu này, nhưng **chưa
  kiểm chứng được trên thiết bị thật** (sandbox viết code không có camera).
  Cần bạn xác nhận: danh sách độ phân giải hiện ra có đúng với thông số kỹ
  thuật máy bạn không.
- Dung lượng file lớn hơn đồng nghĩa **chụp/lưu ảnh chậm hơn một chút** và
  **tốn nhiều bộ nhớ máy hơn** — đây là đánh đổi tất yếu của "giữ chi
  tiết", không phải lỗi.
- Độ phân giải preview và luồng phân tích ML Kit (`ImageAnalysis`) **không
  đổi theo** — vẫn cố định nhỏ (640×480) như trước, vì ML inference không
  cần độ chi tiết cao, tách biệt hoàn toàn với ảnh chụp thật lưu ra máy.

## Cách kiểm thử

1. Cài APK, chụp thử không đổi gì — kiểm tra ảnh trong Gallery có độ phân
   giải/dung lượng lớn hơn rõ rệt so với trước không (nên gần bằng megapixel
   cao nhất ghi trên thông số máy).
2. Chạm vào chip "Độ phân giải" cạnh thanh chọn chế độ — xem danh sách có
   khớp với các độ phân giải camera máy hỗ trợ không, thử chọn độ phân giải
   thấp hơn rồi chụp, kiểm tra file ảnh có đúng kích thước đã chọn không.
3. Đổi camera trước/sau, xem độ phân giải có tự về mức cao nhất của camera
   đó không.
