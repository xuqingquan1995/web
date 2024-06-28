package top.xuqingquan.web.publics;

import android.webkit.WebView;

import androidx.annotation.Nullable;

import top.xuqingquan.utils.CharacterUtils;

public abstract class BaseJsAccessEntrace implements JsAccessEntrace {

    private final WebView mWebView;

    BaseJsAccessEntrace(WebView webView) {
        this.mWebView = webView;
    }

    @Override
    public void callJs(@Nullable String js, @Nullable android.webkit.ValueCallback<String> callback) {
        this.evaluateJs(js, callback);
    }

    @Override
    public void callJs(@Nullable String js) {
        this.callJs(js, null);
    }

    private void evaluateJs(@Nullable String js, @Nullable android.webkit.ValueCallback<String> callback) {
        if (mWebView == null || js == null || js.isEmpty()) {
            return;
        }
        mWebView.evaluateJavascript(js, value -> {
            if (callback != null) {
                callback.onReceiveValue(value);
            }
        });
    }

    @Override
    public void quickCallJs(@Nullable String method, @Nullable android.webkit.ValueCallback<String> callback, @Nullable String... params) {
        StringBuilder sb = new StringBuilder();
        sb.append("javascript:").append(method);
        if (params == null || params.length == 0) {
            sb.append("()");
        } else {
            sb.append("(").append(concat(params)).append(")");
        }
        callJs(sb.toString(), callback);
    }

    private String concat(String... params) {
        StringBuilder mStringBuilder = new StringBuilder();
        for (int i = 0; i < params.length; i++) {
            String param = params[i];
            if (!CharacterUtils.isJson(param)) {
                mStringBuilder.append("\"").append(param).append("\"");
            } else {
                mStringBuilder.append(param);
            }
            if (i != params.length - 1) {
                mStringBuilder.append(" , ");
            }
        }
        return mStringBuilder.toString();
    }

    @Override
    public void quickCallJs(@Nullable String method, @Nullable String... params) {
        this.quickCallJs(method, null, params);
    }

    @Override
    public void quickCallJs(@Nullable String method) {
        this.quickCallJs(method, (String[]) null);
    }
}
