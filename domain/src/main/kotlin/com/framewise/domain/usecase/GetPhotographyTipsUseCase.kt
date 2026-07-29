package com.framewise.domain.usecase

import com.framewise.domain.model.SceneType
import com.framewise.domain.model.ShootingMode
import javax.inject.Inject

/**
 * Static, hand-curated tips per scene. Kept as a pure function (not a
 * repository reading from a database/network) since the tip set is small,
 * fixed, and ships with the app - there is no case where it needs to be
 * fetched or updated independently.
 *
 * [mode] takes priority over the auto-detected [scene] whenever the user
 * has explicitly picked one: [scene] comes from ML Kit's own scene
 * classifier running on the live frame and can mislabel (or briefly flicker
 * to) the wrong category - e.g. reporting [SceneType.PET] while a pet
 * happens to walk through a portrait shot the user deliberately chose
 * PORTRAIT mode for. Showing that auto-detected tip anyway would silently
 * contradict the mode the user just picked, so a manual mode always wins.
 */
class GetPhotographyTipsUseCase @Inject constructor() {

    operator fun invoke(scene: SceneType, mode: ShootingMode = ShootingMode.AUTO): String {
        val modeTip = when (mode) {
            ShootingMode.PORTRAIT -> "Lấy nét vào mắt, chừa khoảng trống đầu vừa phải."
            ShootingMode.ANIMAL -> "Hạ máy ngang tầm mắt thú cưng, chờ khoảnh khắc tự nhiên."
            ShootingMode.LANDSCAPE -> "Đặt đường chân trời tại 1/3 khung hình, tìm tiền cảnh tạo chiều sâu."
            ShootingMode.AUTO -> null
        }
        if (modeTip != null) return modeTip

        return when (scene) {
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
}
