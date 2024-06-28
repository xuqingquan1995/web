package top.xuqingquan.web.system

import android.annotation.SuppressLint
import top.xuqingquan.utils.Timber
import top.xuqingquan.web.nokernel.JsInterfaceHolder
import top.xuqingquan.web.nokernel.JsInterfaceObjectException

/**
 * Created by 许清泉 on 12/21/20 11:19 PM
 */
class JsInterfaceHolderImpl private constructor(webCreator: WebCreator<*>) :
    JsBaseInterfaceHolder(webCreator) {

    private val mWebView = webCreator.getWebView()

    companion object {
        @JvmStatic
        fun getJsInterfaceHolder(webCreator: WebCreator<*>): JsInterfaceHolderImpl {
            return JsInterfaceHolderImpl(webCreator)
        }
    }

    override fun addJavaObjects(maps: MutableMap<String, Any>): JsInterfaceHolder {
        for ((k, v) in maps) {
            if (checkObject(v)) {
                addJavaObjectDirect(k, v)
            } else {
                throw JsInterfaceObjectException("this object has not offer method javascript to call , please check addJavascriptInterface annotation was be added")
            }
        }
        return this
    }

    override fun addJavaObject(k: String, v: Any): JsInterfaceHolder {
        if (checkObject(v)) {
            addJavaObjectDirect(k, v)
        } else {
            throw JsInterfaceObjectException("this object has not offer method javascript to call , please check addJavascriptInterface annotation was be added")
        }
        return this
    }

    @SuppressLint("JavascriptInterface")
    private fun addJavaObjectDirect(k: String, v: Any): JsInterfaceHolder {
        Timber.i("k:$k  v:$v")
        this.mWebView?.addJavascriptInterface(v, k)
        return this
    }
}