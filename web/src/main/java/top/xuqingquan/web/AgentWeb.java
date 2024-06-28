package top.xuqingquan.web;

import android.app.Activity;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.ColorInt;
import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Map;

import top.xuqingquan.utils.Timber;
import top.xuqingquan.web.nokernel.BaseIndicatorView;
import top.xuqingquan.web.nokernel.EventInterceptor;
import top.xuqingquan.web.nokernel.HttpHeaders;
import top.xuqingquan.web.nokernel.IEventHandler;
import top.xuqingquan.web.nokernel.IUrlLoader;
import top.xuqingquan.web.nokernel.JsInterfaceHolder;
import top.xuqingquan.web.nokernel.OpenOtherPageWays;
import top.xuqingquan.web.nokernel.PermissionInterceptor;
import top.xuqingquan.web.nokernel.WebLifeCycle;
import top.xuqingquan.web.publics.AbsAgentWebUIController;
import top.xuqingquan.web.publics.AgentWebConfig;
import top.xuqingquan.web.publics.AgentWebJsInterfaceCompat;
import top.xuqingquan.web.publics.AgentWebUIControllerImplBase;
import top.xuqingquan.web.publics.AgentWebUtils;
import top.xuqingquan.web.publics.DefaultWebLifeCycleImpl;
import top.xuqingquan.web.publics.EventHandlerImpl;
import top.xuqingquan.web.publics.IVideo;
import top.xuqingquan.web.publics.IndicatorController;
import top.xuqingquan.web.publics.IndicatorHandler;
import top.xuqingquan.web.publics.JsAccessEntrace;
import top.xuqingquan.web.publics.JsAccessEntraceImpl;
import top.xuqingquan.web.publics.UrlLoaderImpl;
import top.xuqingquan.web.publics.VideoImpl;
import top.xuqingquan.web.publics.WebParentLayout;
import top.xuqingquan.web.system.AbsAgentWebSettings;
import top.xuqingquan.web.system.AgentWebSettingsImpl;
import top.xuqingquan.web.system.DefaultChromeClient;
import top.xuqingquan.web.system.DefaultWebClient;
import top.xuqingquan.web.system.DefaultWebCreator;
import top.xuqingquan.web.system.IAgentWebSettings;
import top.xuqingquan.web.system.IWebLayout;
import top.xuqingquan.web.system.JsInterfaceHolderImpl;
import top.xuqingquan.web.system.MiddlewareWebChromeBase;
import top.xuqingquan.web.system.MiddlewareWebClientBase;
import top.xuqingquan.web.system.WebCreator;
import top.xuqingquan.web.system.WebListenerManager;

@SuppressWarnings({"rawtypes", "unused", "RedundantSuppression"})
public final class AgentWeb {
    /**
     * Activity
     */
    private final Activity mActivity;
    /**
     * 承载 WebParentLayout 的 ViewGroup
     */
    private final ViewGroup mViewGroup;
    /**
     * 负责创建布局 WebView ，WebParentLayout  Indicator等。
     */
    private final WebCreator mWebCreator;
    /**
     * 管理 WebSettings
     */
    private IAgentWebSettings mAgentWebSettings;
    /**
     * IndicatorController 控制Indicator
     */
    private IndicatorController mIndicatorController;
    /**
     * WebChromeClient
     */
    private final WebChromeClient mWebChromeClient;
    /**
     * WebViewClient
     */
    private final WebViewClient mWebViewClient;
    /**
     * is show indicator
     */
    private final boolean mEnableIndicator;
    /**
     * IEventHandler 处理WebView相关返回事件
     */
    private IEventHandler mIEventHandler;
    /**
     * WebView 注入对象
     */
    private final ArrayMap<String, Object> mJavaObjects = new ArrayMap<>();
    /**
     * WebListenerManager
     */
    private WebListenerManager mWebListenerManager;
    /**
     * Activity
     */
    private static final int ACTIVITY_TAG = 0;
    /**
     * Fragment
     */
    private static final int FRAGMENT_TAG = 1;
    /**
     * JsAccessEntrace 提供快速JS方法调用
     */
    private JsAccessEntrace mJsAccessEntrace = null;
    /**
     * URL Loader ， 提供了 WebView#loadUrl(url) reload() stopLoading（） postUrl()等方法
     */
    private final IUrlLoader mIUrlLoader;
    /**
     * WebView 生命周期 ， 跟随生命周期释放CPU
     */
    private final WebLifeCycle mWebLifeCycle;
    /**
     * Video 视屏播放管理类
     */
    private IVideo mIVideo = null;
    /**
     * WebViewClient 辅助控制开关
     */
    private final boolean mWebClientHelper;
    /**
     * WebViewClient 解析种子文件开关
     */
    private final boolean mParseThunder;
    /**
     * PermissionInterceptor 权限拦截
     */
    private final PermissionInterceptor mPermissionInterceptor;
    /**
     * 是否拦截未知的Url， @link{DefaultWebClient}
     */
    private final boolean mIsInterceptUnknownUrl;
    /**
     * Url处理方式，是直接跳转还是弹窗让用户去选择
     */
    private int mUrlHandleWays = -1;
    /**
     * MiddlewareWebClientBase WebViewClient 中间件
     */
    private final MiddlewareWebClientBase mMiddlewareWebClientBaseHeader;
    /**
     * MiddlewareWebChromeBase WebChromeClient 中间件
     */
    private final MiddlewareWebChromeBase mMiddlewareWebChromeBaseHeader;
    /**
     * 事件拦截
     */
    private EventInterceptor mEventInterceptor;
    /**
     * 注入对象管理类
     */
    private JsInterfaceHolder mJsInterfaceHolder = null;


    private AgentWeb(AgentBuilder agentBuilder) {
        this.mActivity = agentBuilder.mActivity;
        this.mViewGroup = agentBuilder.mViewGroup;
        this.mIEventHandler = agentBuilder.mIEventHandler;
        this.mEnableIndicator = agentBuilder.mEnableIndicator;
        //noinspection ReplaceNullCheck
        if (agentBuilder.mWebCreator == null) {
            mWebCreator = configWebCreator(agentBuilder.mBaseIndicatorView, agentBuilder.mIndex, agentBuilder.mLayoutParams, agentBuilder.mIndicatorColor, agentBuilder.mHeight, agentBuilder.mWebView, agentBuilder.mWebLayout);
        } else {
            mWebCreator = agentBuilder.mWebCreator;
        }
        //noinspection ConstantValue
        mIndicatorController = agentBuilder.mIndicatorController;
        this.mWebChromeClient = agentBuilder.mWebChromeClient;
        this.mWebViewClient = agentBuilder.mWebViewClient;
        this.mAgentWebSettings = agentBuilder.mAgentWebSettings;
        this.mMiddlewareWebClientBaseHeader = agentBuilder.mMiddlewareWebClientBaseHeader;
        this.mMiddlewareWebChromeBaseHeader = agentBuilder.mChromeMiddleWareHeader;
        this.mIUrlLoader = new UrlLoaderImpl(getWebCreator().create().getWebView(), agentBuilder.mHttpHeaders);
        this.mWebLifeCycle = new DefaultWebLifeCycleImpl(getWebCreator().getWebView());
        if (getWebCreator().getWebParentLayout() instanceof WebParentLayout mWebParentLayout) {
            mWebParentLayout.bindController(agentBuilder.mAgentWebUIController == null ? AgentWebUIControllerImplBase.build() : agentBuilder.mAgentWebUIController);
            mWebParentLayout.setErrorLayoutRes(agentBuilder.mErrorLayout, agentBuilder.mReloadId);
            mWebParentLayout.setErrorView(agentBuilder.mErrorView);
        }
        if (agentBuilder.mJavaObject != null && !agentBuilder.mJavaObject.isEmpty()) {
            this.mJavaObjects.putAll((Map<? extends String, ?>) agentBuilder.mJavaObject);
            Timber.i("mJavaObject size:" + this.mJavaObjects.size());
        }
        if (agentBuilder.mPermissionInterceptor == null) {
            this.mPermissionInterceptor = null;
        } else {
            this.mPermissionInterceptor = agentBuilder.mPermissionInterceptor;
        }
        this.mWebClientHelper = agentBuilder.mWebClientHelper;
        this.mParseThunder = agentBuilder.mParseThunder;
        this.mIsInterceptUnknownUrl = agentBuilder.mIsInterceptUnknownUrl;
        if (agentBuilder.mOpenOtherPage != null) {
            this.mUrlHandleWays = agentBuilder.mOpenOtherPage.getCode();
        }
        doCompat();
    }

    /**
     * @return PermissionInterceptor 权限控制者
     */
    @Nullable
    public PermissionInterceptor getPermissionInterceptor() {
        return this.mPermissionInterceptor;
    }

    public WebLifeCycle getWebLifeCycle() {
        return this.mWebLifeCycle;
    }

    public JsAccessEntrace getJsAccessEntrace() {
        JsAccessEntrace mJsAccessEntrace = this.mJsAccessEntrace;
        if (mJsAccessEntrace == null) {
            this.mJsAccessEntrace = JsAccessEntraceImpl.getInstance(getWebCreator().getWebView());
            mJsAccessEntrace = this.mJsAccessEntrace;
        }
        return mJsAccessEntrace;
    }

    public AgentWeb clearWebCache() {
        if (getWebCreator() != null && getWebCreator().getWebView() != null) {
            AgentWebUtils.clearWebViewAllCache(mActivity, getWebCreator().getWebView());
        } else {
            AgentWebUtils.clearWebViewAllCache(mActivity);
        }
        return this;
    }

    public static AgentBuilder with(@NonNull Activity activity) {
        return new AgentBuilder(activity);
    }

    public static AgentBuilder with(@NonNull Fragment fragment) {
        Activity mActivity = fragment.getActivity();
        if (mActivity == null) {
            throw new NullPointerException("fragment.getActivity() can not be null .");
        }
        return new AgentBuilder(fragment);
    }

    public boolean handleKeyEvent(int keyCode, KeyEvent keyEvent) {
        this.mIEventHandler = getIEventHandler();
        if (mIEventHandler != null) {
            return mIEventHandler.onKeyDown(keyCode, keyEvent);
        } else {
            return false;
        }
    }

    public boolean back() {
        this.mIEventHandler = getIEventHandler();
        if (mIEventHandler != null) {
            return mIEventHandler.back();
        } else {
            return false;
        }
    }

    @Nullable
    public WebCreator getWebCreator() {
        return this.mWebCreator;
    }

    private IEventHandler getIEventHandler() {
        if (this.mIEventHandler == null) {
            if (getWebCreator() != null) {
                mIEventHandler = EventHandlerImpl.getInstance(getWebCreator().getWebView(), getInterceptor());
            }
        }
        return this.mIEventHandler;
    }

    public IAgentWebSettings getAgentWebSettings() {
        return this.mAgentWebSettings;
    }

    @SuppressWarnings("WeakerAccess")
    public IndicatorController getIndicatorController() {
        return this.mIndicatorController;
    }

    public JsInterfaceHolder getJsInterfaceHolder() {
        return this.mJsInterfaceHolder;
    }

    @Nullable
    public IUrlLoader getUrlLoader() {
        return this.mIUrlLoader;
    }

    public void destroy() {
        this.mWebLifeCycle.onDestroy();
    }

    private void doCompat() {
        mJavaObjects.put("agentWeb", new AgentWebJsInterfaceCompat(this, mActivity));
    }

    private WebCreator configWebCreator(BaseIndicatorView progressView, int index, ViewGroup.LayoutParams lp, int indicatorColor, int height_dp, WebView webView, IWebLayout webLayout) {
        if (progressView != null && mEnableIndicator) {
            return new DefaultWebCreator(mActivity, mViewGroup, lp, index, progressView, webView, webLayout);
        } else {
            return mEnableIndicator ?
                    new DefaultWebCreator(mActivity, mViewGroup, lp, index, indicatorColor, height_dp, webView, webLayout)
                    : new DefaultWebCreator(mActivity, mViewGroup, lp, index, webView, webLayout);
        }
    }


    private AgentWeb go(String url) {
        if (getUrlLoader() != null) {
            getUrlLoader().loadUrl(url);
        }
        IndicatorController mIndicatorController = getIndicatorController();
        if (!TextUtils.isEmpty(url) && mIndicatorController != null && mIndicatorController.offerIndicator() != null) {
            //noinspection ConstantConditions
            getIndicatorController().offerIndicator().show();
        }
        return this;
    }

    private EventInterceptor getInterceptor() {
        if (this.mEventInterceptor != null) {
            return this.mEventInterceptor;
        }
        if (mIVideo instanceof VideoImpl) {
            this.mEventInterceptor = (EventInterceptor) this.mIVideo;
            return this.mEventInterceptor;
        }
        return null;
    }

    private IVideo getIVideo() {
        if (mIVideo == null) {
            mIVideo = new VideoImpl(mActivity, getWebCreator().getWebView());
        }
        return mIVideo;
    }

    @SuppressWarnings("ConstantConditions")
    private WebViewClient getWebViewClient() {
        Timber.i("getDelegate:" + this.mMiddlewareWebClientBaseHeader);
        DefaultWebClient mDefaultWebClient = DefaultWebClient
                .createBuilder()
                .setActivity(this.mActivity)
                .setClient(mWebViewClient)
                .setWebClientHelper(this.mWebClientHelper)
                .setParseThunder(this.mParseThunder)
                .setPermissionInterceptor(this.mPermissionInterceptor)
                .setWebView(getWebCreator().getWebView())
                .setInterceptUnkownUrl(this.mIsInterceptUnknownUrl)
                .setUrlHandleWays(this.mUrlHandleWays)
                .build();
        MiddlewareWebClientBase header = this.mMiddlewareWebClientBaseHeader;
        if (header != null) {
            MiddlewareWebClientBase tail = header;
            int count = 1;
            MiddlewareWebClientBase tmp = header;
            while (tmp.next() != null) {
                tmp = tmp.next();
                tail = tmp;
                count++;
            }
            Timber.i("MiddlewareWebClientBase middleware count:" + count);
            tail.setDelegate(mDefaultWebClient);
            return header;
        } else {
            return mDefaultWebClient;
        }
    }

    /**
     * @noinspection deprecation, UnusedReturnValue
     */
    private AgentWeb ready() {
        AgentWebConfig.initCookiesManager(mActivity.getApplicationContext());
        IAgentWebSettings mAgentWebSettings = this.mAgentWebSettings;
        if (mAgentWebSettings == null) {
            this.mAgentWebSettings = AgentWebSettingsImpl.getInstance();
            mAgentWebSettings = this.mAgentWebSettings;
        }
        if (mAgentWebSettings instanceof AbsAgentWebSettings) {
            ((AbsAgentWebSettings) mAgentWebSettings).bindAgentWeb(this);
            if (mWebListenerManager == null) {
                mWebListenerManager = (WebListenerManager) mAgentWebSettings;
            }
        }
        WebView webView = getWebCreator().getWebView();
        mAgentWebSettings.toSetting(webView);
        if (mJsInterfaceHolder == null) {
            mJsInterfaceHolder = JsInterfaceHolderImpl.getJsInterfaceHolder(mWebCreator);
        }
        Timber.i("mJavaObjects:" + mJavaObjects.size());
        if (!mJavaObjects.isEmpty()) {
            mJsInterfaceHolder.addJavaObjects(mJavaObjects);
        }
        if (mWebListenerManager != null) {
            mWebListenerManager.setDownloader(webView, null);
            mWebListenerManager.setWebChromeClient(webView, getChromeClient());
            mWebListenerManager.setWebViewClient(webView, getWebViewClient());
        }
        return this;
    }

    @SuppressWarnings("ConstantConditions")
    private WebChromeClient getChromeClient() {
        this.mIndicatorController = (this.mIndicatorController == null) ?
                IndicatorHandler.getInstance().injectIndicator(getWebCreator().offer())
                : this.mIndicatorController;
        this.mIVideo = getIVideo();
        DefaultChromeClient mDefaultChromeClient =
                new DefaultChromeClient(this.mActivity,
                        this.mIndicatorController,
                        mWebChromeClient, this.mIVideo,
                        this.mPermissionInterceptor, getWebCreator().getWebView());
        Timber.i("WebChromeClient:" + this.mWebChromeClient);
        MiddlewareWebChromeBase header = this.mMiddlewareWebChromeBaseHeader;
        if (header != null) {
            MiddlewareWebChromeBase tail = header;
            int count = 1;
            MiddlewareWebChromeBase tmp = header;
            while (tmp.next() != null) {
                tmp = tmp.next();
                tail = tmp;
                count++;
            }
            Timber.i("MiddlewareWebClientBase middleware count:" + count);
            tail.setDelegate(mDefaultChromeClient);
            return header;
        } else {
            return mDefaultChromeClient;
        }
    }

    public static final class PreAgentWeb {
        private final AgentWeb mAgentWeb;
        private boolean isReady = false;

        PreAgentWeb(AgentWeb agentWeb) {
            this.mAgentWeb = agentWeb;
        }

        @SuppressWarnings("UnusedReturnValue")
        public PreAgentWeb ready() {
            if (!isReady) {
                mAgentWeb.ready();
                isReady = true;
            }
            return this;
        }

        public AgentWeb get() {
            ready();
            return mAgentWeb;
        }

        public AgentWeb go(@Nullable String url) {
            if (!isReady) {
                ready();
            }
            return mAgentWeb.go(url);
        }
    }

    public static final class AgentBuilder {
        private final Activity mActivity;
        private ViewGroup mViewGroup;
        private int mIndex = -1;
        private BaseIndicatorView mBaseIndicatorView;
        private final IndicatorController mIndicatorController = null;
        /*默认进度条是显示的*/
        private boolean mEnableIndicator = true;
        private ViewGroup.LayoutParams mLayoutParams = null;
        private WebViewClient mWebViewClient;
        private WebChromeClient mWebChromeClient;
        private int mIndicatorColor = -1;
        private IAgentWebSettings mAgentWebSettings;
        private WebCreator mWebCreator;
        private HttpHeaders mHttpHeaders = null;
        private IEventHandler mIEventHandler;
        private int mHeight = -1;
        private ArrayMap<String, Object> mJavaObject;
        private WebView mWebView;
        private boolean mWebClientHelper = true;
        private boolean mParseThunder = false;
        private IWebLayout mWebLayout = null;
        private PermissionInterceptor mPermissionInterceptor = null;
        private AbsAgentWebUIController mAgentWebUIController;
        private OpenOtherPageWays mOpenOtherPage = null;
        private boolean mIsInterceptUnknownUrl = false;
        private MiddlewareWebClientBase mMiddlewareWebClientBaseHeader;
        private MiddlewareWebClientBase mMiddlewareWebClientBaseTail;
        private MiddlewareWebChromeBase mChromeMiddleWareHeader = null;
        private MiddlewareWebChromeBase mChromeMiddleWareTail = null;
        private View mErrorView;
        private int mErrorLayout;
        private int mReloadId;
        private final int mTag;

        AgentBuilder(@NonNull Fragment fragment) {
            mActivity = fragment.getActivity();
            mTag = AgentWeb.FRAGMENT_TAG;
        }

        AgentBuilder(@NonNull Activity activity) {
            mActivity = activity;
            mTag = AgentWeb.ACTIVITY_TAG;
        }


        public IndicatorBuilder setAgentWebParent(@NonNull ViewGroup v, @NonNull ViewGroup.LayoutParams lp) {
            this.mViewGroup = v;
            this.mLayoutParams = lp;
            return new IndicatorBuilder(this);
        }

        public IndicatorBuilder setAgentWebParent(@NonNull ViewGroup v, int index, @NonNull ViewGroup.LayoutParams lp) {
            this.mViewGroup = v;
            this.mLayoutParams = lp;
            this.mIndex = index;
            return new IndicatorBuilder(this);
        }

        private PreAgentWeb buildAgentWeb() {
            if (mTag == AgentWeb.FRAGMENT_TAG && this.mViewGroup == null) {
                throw new NullPointerException("ViewGroup is null,Please check your parameters .");
            }
            return new PreAgentWeb(new AgentWeb(this));
        }

        private void addJavaObject(String key, Object o) {
            if (mJavaObject == null) {
                mJavaObject = new ArrayMap<>();
            }
            mJavaObject.put(key, o);
        }

        private void addHeader(String baseUrl, String k, String v) {
            if (mHttpHeaders == null) {
                mHttpHeaders = HttpHeaders.create();
            }
            mHttpHeaders.additionalHttpHeader(baseUrl, k, v);
        }

        private void addHeader(String baseUrl, Map<String, String> headers) {
            if (mHttpHeaders == null) {
                mHttpHeaders = HttpHeaders.create();
            }
            mHttpHeaders.additionalHttpHeaders(baseUrl, headers);
        }
    }

    public static class IndicatorBuilder {
        private final AgentBuilder mAgentBuilder;

        IndicatorBuilder(AgentBuilder agentBuilder) {
            this.mAgentBuilder = agentBuilder;
        }

        public CommonBuilder useDefaultIndicator(int color) {
            this.mAgentBuilder.mEnableIndicator = true;
            this.mAgentBuilder.mIndicatorColor = color;
            return new CommonBuilder(mAgentBuilder);
        }

        public CommonBuilder useDefaultIndicator() {
            this.mAgentBuilder.mEnableIndicator = true;
            return new CommonBuilder(mAgentBuilder);
        }

        public CommonBuilder closeIndicator() {
            this.mAgentBuilder.mEnableIndicator = false;
            this.mAgentBuilder.mIndicatorColor = -1;
            this.mAgentBuilder.mHeight = -1;
            return new CommonBuilder(mAgentBuilder);
        }

        public CommonBuilder setCustomIndicator(@Nullable BaseIndicatorView v) {
            if (v != null) {
                this.mAgentBuilder.mEnableIndicator = true;
                this.mAgentBuilder.mBaseIndicatorView = v;
            } else {
                this.mAgentBuilder.mEnableIndicator = true;
            }
            return new CommonBuilder(mAgentBuilder);
        }

        public CommonBuilder useDefaultIndicator(@ColorInt int color, int height_dp) {
            this.mAgentBuilder.mIndicatorColor = color;
            this.mAgentBuilder.mHeight = height_dp;
            return new CommonBuilder(this.mAgentBuilder);
        }
    }

    public static class CommonBuilder {
        private final AgentBuilder mAgentBuilder;

        CommonBuilder(AgentBuilder agentBuilder) {
            this.mAgentBuilder = agentBuilder;
        }

        public CommonBuilder setEventHanadler(@Nullable IEventHandler iEventHandler) {
            mAgentBuilder.mIEventHandler = iEventHandler;
            return this;
        }

        public CommonBuilder closeWebViewClientHelper() {
            mAgentBuilder.mWebClientHelper = false;
            return this;
        }

        public CommonBuilder parseThunder() {
            mAgentBuilder.mParseThunder = true;
            return this;
        }

        public CommonBuilder setWebChromeClient(@Nullable WebChromeClient webChromeClient) {
            this.mAgentBuilder.mWebChromeClient = webChromeClient;
            return this;
        }

        public CommonBuilder setWebViewClient(@Nullable WebViewClient webViewClient) {
            this.mAgentBuilder.mWebViewClient = webViewClient;
            return this;
        }

        public CommonBuilder useMiddlewareWebClient(@Nullable MiddlewareWebClientBase middleWrareWebClientBase) {
            if (middleWrareWebClientBase == null) {
                return this;
            }
            this.mAgentBuilder.mMiddlewareWebClientBaseTail = middleWrareWebClientBase;
            if (this.mAgentBuilder.mMiddlewareWebClientBaseHeader == null) {
                this.mAgentBuilder.mMiddlewareWebClientBaseHeader = middleWrareWebClientBase;
            } else {
                this.mAgentBuilder.mMiddlewareWebClientBaseTail.enq(middleWrareWebClientBase);
            }
            return this;
        }

        public CommonBuilder useMiddlewareWebChrome(@Nullable MiddlewareWebChromeBase middlewareWebChromeBase) {
            if (middlewareWebChromeBase == null) {
                return this;
            }
            this.mAgentBuilder.mChromeMiddleWareTail = middlewareWebChromeBase;
            if (this.mAgentBuilder.mChromeMiddleWareHeader == null) {
                this.mAgentBuilder.mChromeMiddleWareHeader = middlewareWebChromeBase;
            } else {
                this.mAgentBuilder.mChromeMiddleWareTail.enq(middlewareWebChromeBase);
            }
            return this;
        }

        public CommonBuilder setMainFrameErrorView(@NonNull View view) {
            this.mAgentBuilder.mErrorView = view;
            return this;
        }

        public CommonBuilder setMainFrameErrorView(@LayoutRes int errorLayout, @IdRes int clickViewId) {
            this.mAgentBuilder.mErrorLayout = errorLayout;
            this.mAgentBuilder.mReloadId = clickViewId;
            return this;
        }

        public CommonBuilder setAgentWebWebSettings(@Nullable IAgentWebSettings agentWebSettings) {
            this.mAgentBuilder.mAgentWebSettings = agentWebSettings;
            return this;
        }

        public PreAgentWeb createAgentWeb() {
            return this.mAgentBuilder.buildAgentWeb();
        }


        public CommonBuilder addJavascriptInterface(@NonNull String name, @NonNull Object o) {
            this.mAgentBuilder.addJavaObject(name, o);
            return this;
        }

        public CommonBuilder setWebView(@Nullable WebView webView) {
            this.mAgentBuilder.mWebView = webView;
            return this;
        }

        public CommonBuilder setWebLayout(@Nullable IWebLayout iWebLayout) {
            this.mAgentBuilder.mWebLayout = iWebLayout;
            return this;
        }

        public CommonBuilder additionalHttpHeader(String baseUrl, String k, String v) {
            this.mAgentBuilder.addHeader(baseUrl, k, v);
            return this;
        }

        public CommonBuilder additionalHttpHeader(String baseUrl, Map<String, String> headers) {
            this.mAgentBuilder.addHeader(baseUrl, headers);
            return this;
        }

        public CommonBuilder setPermissionInterceptor(@Nullable PermissionInterceptor permissionInterceptor) {
            this.mAgentBuilder.mPermissionInterceptor = permissionInterceptor;
            return this;
        }

        public CommonBuilder setAgentWebUIController(@Nullable AbsAgentWebUIController agentWebUIController) {
            this.mAgentBuilder.mAgentWebUIController = agentWebUIController;
            return this;
        }

        public CommonBuilder setOpenOtherPageWays(@Nullable OpenOtherPageWays openOtherPageWays) {
            this.mAgentBuilder.mOpenOtherPage = openOtherPageWays;
            return this;
        }

        public CommonBuilder interceptUnknownUrl() {
            this.mAgentBuilder.mIsInterceptUnknownUrl = true;
            return this;
        }
    }

    public Activity getActivity() {
        return this.mActivity;
    }
}
