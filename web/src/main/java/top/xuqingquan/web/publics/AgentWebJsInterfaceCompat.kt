package top.xuqingquan.web.publics

import android.app.Activity
import android.webkit.JavascriptInterface
import top.xuqingquan.utils.Timber
import top.xuqingquan.web.AgentWeb
import top.xuqingquan.web.nokernel.WebConfig
import java.lang.ref.WeakReference

class AgentWebJsInterfaceCompat(agentWeb: AgentWeb, activity: Activity) {

    private val mReference: WeakReference<AgentWeb> = WeakReference(agentWeb)
    private val mActivityWeakReference: WeakReference<Activity> = WeakReference(activity)

    @Suppress("unused")
    @JavascriptInterface
    @JvmOverloads
    fun uploadFile(acceptType: String = "*/*") {
        Timber.i(acceptType + "  " + mActivityWeakReference.get() + "  " + mReference.get())
        if (mActivityWeakReference.get() != null && mReference.get() != null) {
            if (WebConfig.isTbsEnable() && mReference.get()?.x5WebCreator != null && mReference.get()?.x5WebCreator?.getWebView() != null) {
                AgentWebUtils.showFileChooserCompat(
                    mActivityWeakReference.get()!!,
                    mReference.get()!!.x5WebCreator!!.getWebView()!!, null, null,
                    mReference.get()?.permissionInterceptor, null,
                    acceptType
                ) {
                    mReference.get()?.jsAccessEntrace
                        ?.quickCallJs(
                            "uploadFileResult",
                            if (it.obj is String) {
                                it.obj as String
                            } else {
                                null
                            }
                        )
                    true
                }
            } else if (mReference.get()?.webCreator != null && mReference.get()?.webCreator?.getWebView() != null) {
                AgentWebUtils.showFileChooserCompat(
                    mActivityWeakReference.get()!!,
                    mReference.get()!!.webCreator!!.getWebView()!!, null, null,
                    mReference.get()?.permissionInterceptor, null,
                    acceptType
                ) {
                    mReference.get()?.jsAccessEntrace
                        ?.quickCallJs(
                            "uploadFileResult",
                            if (it.obj is String) {
                                it.obj as String
                            } else {
                                null
                            }
                        )
                    true
                }
            }
        }
    }
}
