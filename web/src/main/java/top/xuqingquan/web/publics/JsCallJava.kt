package top.xuqingquan.web.publics

import android.text.TextUtils
import android.util.Log
import android.webkit.WebView
import org.json.JSONException
import org.json.JSONObject
import top.xuqingquan.utils.Timber
import java.lang.reflect.Method
import java.util.*

@Suppress("unused")
class JsCallJava(interfaceObj: Any, interfaceName: String) {
    private var mMethodsMap: HashMap<String, Method>? = null
    private var mInterfaceObj: Any? = null
    private var mInterfacedName: String? = null
    var preloadInterfaceJs: String? = null
        private set

    init {
        try {
            if (TextUtils.isEmpty(interfaceName)) {
                throw Throwable("injected name can not be null")
            }
            mInterfaceObj = interfaceObj
            mInterfacedName = interfaceName
            mMethodsMap = HashMap()
            // getMethods会获得所有继承与非继承的方法
            val methods = mInterfaceObj!!.javaClass.methods
            // 拼接的js脚本可参照备份文件：./library/doc/injected.js
            val sb = StringBuilder("javascript:(function(b){console.log(\"")
            sb.append(mInterfacedName)
            sb.append(" init begin\");var a={queue:[],callback:function(){var d=Array.prototype.slice.call(arguments,0);var c=d.shift();var e=d.shift();this.queue[c].apply(this,d);if(!e){delete this.queue[c]}}};")
            for (method in methods) {
                Log.i("Info", "method:$method")
                val sign = genJavaMethodSign(method) ?: continue
                mMethodsMap!![sign] = method
                sb.append(String.format("a.%s=", method.name))
            }
            sb.append("function(){var f=Array.prototype.slice.call(arguments,0);if(f.length<1){throw\"")
            sb.append(mInterfacedName)
            sb.append(" call result, message:miss method name\"}var e=[];for(var h=1;h<f.length;h++){var c=f[h];var j=typeof c;e[e.length]=j;if(j==\"function\"){var d=a.queue.length;a.queue[d]=c;f[h]=d}}var k = new Date().getTime();var l = f.shift();var m=prompt('")
            sb.append(MSG_PROMPT_HEADER)
            sb.append("'+JSON.stringify(")
            sb.append(promptMsgFormat("'$mInterfacedName'", "l", "e", "f"))
            sb.append("));console.log(\"invoke \"+l+\", time: \"+(new Date().getTime()-k));var g=JSON.parse(m);if(g.CODE!=200){throw\"")
            sb.append(mInterfacedName)
            sb.append(" call result, CODE:\"+g.CODE+\", message:\"+g.result}return g.result};Object.getOwnPropertyNames(a).forEach(function(d){var c=a[d];if(typeof c===\"function\"&&d!==\"callback\"){a[d]=function(){return c.apply(a,[d].concat(Array.prototype.slice.call(arguments,0)))}}});b.")
            sb.append(mInterfacedName)
            sb.append("=a;console.log(\"")
            sb.append(mInterfacedName)
            sb.append(" init end\")})(window)")
            preloadInterfaceJs = sb.toString()
            sb.setLength(0)
        } catch (e: Throwable) {
            Timber.e("init js result:" + e.message)
        }

    }

    private fun genJavaMethodSign(method: Method): String? {
        val sign = StringBuilder(method.name)
        val argsTypes = method.parameterTypes
        for (ignoreMethod in IGNORE_UNSAFE_METHODS) {
            if (ignoreMethod == sign.toString()) {
                Timber.w("method($sign) is unsafe, will be pass")
                return null
            }
        }
        for (cls in argsTypes) {
            when (cls) {
                String::class.java -> {
                    sign.append("_S")
                }
                Int::class.javaPrimitiveType, Long::class.javaPrimitiveType, Float::class.javaPrimitiveType, Double::class.javaPrimitiveType -> {
                    sign.append("_N")
                }
                Boolean::class.javaPrimitiveType -> {
                    sign.append("_B")
                }
                JSONObject::class.java -> {
                    sign.append("_O")
                }
                JsCallback::class.java -> {
                    sign.append("_F")
                }
                else -> {
                    sign.append("_P")
                }
            }
        }
        return sign.toString()
    }

    fun call(webView: WebView, jsonObject: JSONObject?): String {
        val time = android.os.SystemClock.uptimeMillis()
        return if (jsonObject != null) {
            try {
                val methodName = jsonObject.getString(KEY_METHOD)
                val argsTypes = jsonObject.getJSONArray(KEY_TYPES)
                val argsVals = jsonObject.getJSONArray(KEY_ARGS)
                val sign = StringBuilder(methodName)
                val len = argsTypes.length()
                val values = arrayOfNulls<Any>(len)
                var numIndex = 0
                var currType: String

                for (k in 0 until len) {
                    currType = argsTypes.optString(k)
                    when (currType) {
                        "string" -> {
                            sign.append("_S")
                            values[k] = if (argsVals.isNull(k)) null else argsVals.getString(k)
                        }
                        "number" -> {
                            sign.append("_N")
                            numIndex = numIndex * 10 + k + 1
                        }
                        "boolean" -> {
                            sign.append("_B")
                            values[k] = argsVals.getBoolean(k)
                        }
                        "object" -> {
                            sign.append("_O")
                            values[k] = if (argsVals.isNull(k)) null else argsVals.getJSONObject(k)
                        }
                        "function" -> {
                            sign.append("_F")
                            values[k] = JsCallback(webView, mInterfacedName!!, argsVals.getInt(k))
                        }
                        else -> sign.append("_P")
                    }
                }

                val currMethod = mMethodsMap!![sign.toString()] ?: return getReturn(
                    jsonObject,
                    500,
                    "not found method($sign) with valid parameters",
                    time
                )

                // 方法匹配失败
                // 数字类型细分匹配
                if (numIndex > 0) {
                    val methodTypes = currMethod.parameterTypes
                    var currIndex: Int
                    var currCls: Class<*>
                    while (numIndex > 0) {
                        currIndex = numIndex - numIndex / 10 * 10 - 1
                        currCls = methodTypes[currIndex]
                        when (currCls) {
                            Int::class.javaPrimitiveType -> values[currIndex] =
                                argsVals.getInt(currIndex)
                            Long::class.javaPrimitiveType -> //WARN: argsJson.getLong(k + defValue) will return a bigger incorrect number
                                values[currIndex] =
                                    java.lang.Long.parseLong(argsVals.getString(currIndex))
                            else -> values[currIndex] = argsVals.getDouble(currIndex)
                        }
                        numIndex /= 10
                    }
                }

                getReturn(jsonObject, 200, currMethod.invoke(mInterfaceObj, *values), time)
            } catch (e: Throwable) {
                //优先返回详细的错误信息
                if (e.cause != null) {
                    return getReturn(
                        jsonObject,
                        500,
                        "method execute result:" + e.cause!!.message,
                        time
                    )
                }
                getReturn(jsonObject, 500, "method execute result:" + e.message, time)
            }

        } else {
            getReturn(null, 500, "call data empty", time)
        }
    }

    private fun getReturn(
        reqJson: JSONObject?,
        stateCode: Int,
        mResult: Any?,
        time: Long
    ): String {
        var result = mResult
        val insertRes: String
        when (result) {
            null -> insertRes = "null"
            is String -> {
                result = result.replace("\"", "\\\"")
                insertRes = "\"" + result.toString() + "\""
            }
            else -> // 其他类型直接转换
                insertRes = result.toString()

            // 兼容：如果在解决WebView注入安全漏洞时，js注入采用的是XXX:function(){return prompt(...)}的形式，函数返回类型包括：void、int、boolean、String；
            // 在返回给网页（onJsPrompt方法中jsPromptResult.confirm）的时候强制返回的是String类型，所以在此将result的值加双引号兼容一下；
            // insertRes = "\"".concat(String.valueOf(result)).concat("\"");
        }
        val resStr = String.format(Locale.getDefault(), RETURN_RESULT_FORMAT, stateCode, insertRes)
        Timber.d("call time: " + (android.os.SystemClock.uptimeMillis() - time) + ", request: " + reqJson + ", result:" + resStr)
        return resStr
    }

    companion object {
        private const val RETURN_RESULT_FORMAT = "{\"CODE\": %d, \"result\": %s}"
        private const val MSG_PROMPT_HEADER = "AgentWeb:"
        private const val KEY_OBJ = "obj"
        private const val KEY_METHOD = "method"
        private const val KEY_TYPES = "types"
        private const val KEY_ARGS = "args"
        private val IGNORE_UNSAFE_METHODS =
            arrayOf("getClass", "hashCode", "notify", "notifyAll", "equals", "toString", "wait")

        @Suppress("SameParameterValue")
        private fun promptMsgFormat(
            `object`: String,
            method: String,
            types: String,
            args: String
        ): String {
            val sb = StringBuilder()
            sb.append("{")
            sb.append(KEY_OBJ).append(":").append(`object`).append(",")
            sb.append(KEY_METHOD).append(":").append(method).append(",")
            sb.append(KEY_TYPES).append(":").append(types).append(",")
            sb.append(KEY_ARGS).append(":").append(args)
            sb.append("}")
            return sb.toString()
        }

        /**
         * 是否是“Java接口类中方法调用”的内部消息；
         *
         * @param message
         * @return
         */
        @JvmStatic
        fun isSafeWebViewCallMsg(message: String): Boolean {
            return message.startsWith(MSG_PROMPT_HEADER)
        }

        @JvmStatic
        fun getMsgJSONObject(msg: String): JSONObject {
            var message = msg
            message = message.substring(MSG_PROMPT_HEADER.length)
            return try {
                JSONObject(message)
            } catch (e: JSONException) {
                e.printStackTrace()
                JSONObject()
            }
        }

        @JvmStatic
        fun getInterfacedName(jsonObject: JSONObject): String {
            return jsonObject.optString(KEY_OBJ)
        }
    }
}