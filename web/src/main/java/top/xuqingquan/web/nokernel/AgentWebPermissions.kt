package top.xuqingquan.web.nokernel

import android.Manifest
import android.os.Build

/**
 * @author 许清泉 xuqingquan1995@gmail.com
 * @since 2024-06-28
 */
object AgentWebPermissions {
    @JvmField
    var CAMERA: Array<String>
    @JvmField
    var LOCATION: Array<String>
    @JvmField
    var MEDIA: Array<String>
    const val ACTION_CAMERA: String = "Camera"
    const val ACTION_LOCATION: String = "Location"
    const val ACTION_MEDIA: String = "Media"

    init {
        CAMERA = arrayOf(Manifest.permission.CAMERA)

        LOCATION = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        MEDIA = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_IMAGES,
            )
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    private fun emptyMediaPermission() {
        MEDIA = arrayOf()
    }

    private fun emptyCameraPermission() {
        CAMERA = arrayOf()
    }

    fun dontAskUnnecessaryPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            emptyMediaPermission()
            emptyCameraPermission()
        }
    }
}
