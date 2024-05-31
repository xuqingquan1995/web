@file:JvmName("SystemDownloadUtils")

package top.xuqingquan.web.utils

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import top.xuqingquan.utils.Timber
import top.xuqingquan.utils.getCacheFilePath
import top.xuqingquan.web.nokernel.DownLoadBroadcast
import top.xuqingquan.web.nokernel.WebUtils
import java.io.File

/**
 * Created by 许清泉 on 2020/6/22 14:22
 */

@SuppressLint("UnspecifiedRegisterReceiverFlag")
@Throws(Throwable::class)
fun download(context: Context, fileName: String, url: String) {
    val downloadPath = getCacheFilePath(context)
    val downloadFile = File(downloadPath, fileName)
    if (downloadFile.exists()) {
        val mIntent = WebUtils.getCommonFileIntentCompat(context, downloadFile)
        mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(mIntent)
        return
    }
    try {
        val state = context.packageManager.getApplicationEnabledSetting("com.android.providers.downloads")
        if (state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            || state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
            || state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED
        ) {
            return
        }
    } catch (e: Throwable) {
        Timber.e("DownloadManager getApplicationEnabledSetting err")
        e.printStackTrace()
        return
    }
    val downloadManager = ContextCompat.getSystemService(context, DownloadManager::class.java)
    if (downloadManager != null) {
        val request = DownloadManager.Request(Uri.parse(url))
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationInExternalFilesDir(context, "/", fileName)
        downloadManager.enqueue(request)
        val intentFilter = IntentFilter()
        intentFilter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        context.registerReceiver(DownLoadBroadcast(), intentFilter)
    }
}