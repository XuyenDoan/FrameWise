package com.framewise.domain.usecase

import com.framewise.domain.model.SceneType
import javax.inject.Inject

/**
 * Static, hand-curated tips per scene. Kept as a pure function (not a
 * repository reading from a database/network) since the tip set is small,
 * fixed, and ships with the app - there is no case where it needs to be
 * fetched or updated independently.
 */
class GetPhotographyTipsUseCase @Inject constructor() {

    operator fun invoke(scene: SceneType): String = when (scene) {
        SceneType.PORTRAIT -> "Lấy nét vào mắt, chừa khoảng trống đầu vừa phải."
        SceneType.LANDSCAPE -> "Đặt đường chân trời tại 1/3 khung hình, tìm tiền cảnh tạo chiều sâu."
        SceneType.FOOD -> "Chụp góc 45° hoặc từ trên xuống, tận dụng ánh sáng tự nhiên."
        SceneType.ARCHITECTURE -> "Giữ các đường thẳng đứng song song, tránh méo phối cảnh."
        SceneType.PET -> "Hạ máy ngang tầm mắt thú cưng, chờ khoảnh khắc tự nhiên."
        SceneType.FLOWER -> "Lại gần để làm nổi chi tiết, dùng hậu cảnh đơn giản."
        SceneType.SUNSET -> "Đo sáng vào bầu trời, thử chụp ngược sáng tạo silhouette."
        SceneType.NIGHT -> "Giữ máy thật vững hoặc tì vào vật cố định để tránh rung."
        SceneType.STREET -> "Quan sát trước, chờ khoảnh khắc, chú ý hậu cảnh gọn gàng."
        SceneType.MACRO -> "Giữ khoảng cách lấy nét ổn định, tránh rung tay ở cự ly gần."
        SceneType.UNKNOWN -> "Di chuyển máy để tìm góc chụp có bố cục rõ ràng hơn."
    }
}
