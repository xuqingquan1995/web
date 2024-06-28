package top.xuqingquan.web.system;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.webkit.WebView;
import android.widget.FrameLayout;

import top.xuqingquan.utils.DimensionsKt;
import top.xuqingquan.utils.ProcessUtils;
import top.xuqingquan.utils.Timber;
import top.xuqingquan.web.R;
import top.xuqingquan.web.nokernel.BaseIndicatorSpec;
import top.xuqingquan.web.nokernel.BaseIndicatorView;
import top.xuqingquan.web.nokernel.WebConfig;
import top.xuqingquan.web.nokernel.WebIndicator;
import top.xuqingquan.web.publics.WebParentLayout;

@SuppressWarnings("rawtypes")
public final class DefaultWebCreator implements WebCreator {
    private final Activity mActivity;
    private final ViewGroup mViewGroup;
    private final boolean mIsNeedDefaultProgress;
    private final int mIndex;
    private BaseIndicatorView mProgressView;
    private final ViewGroup.LayoutParams mLayoutParams;
    private int mColor = -1;
    /**
     * 单位dp
     */
    private int mHeight;
    private boolean mIsCreated = false;
    private final IWebLayout mIWebLayout;
    private BaseIndicatorSpec mBaseIndicatorSpec;
    private WebView mWebView;
    private FrameLayout mFrameLayout = null;
    private int mWebViewType = WebConfig.WEB_VIEW_DEFAULT_TYPE;

    /**
     * 使用默认的进度条
     */
    public DefaultWebCreator(
            @NonNull Activity activity, @Nullable ViewGroup viewGroup, ViewGroup.LayoutParams lp, int index,
            int color, int mHeight, WebView webView, IWebLayout webLayout) {
        this.mActivity = activity;
        this.mViewGroup = viewGroup;
        this.mIsNeedDefaultProgress = true;
        this.mIndex = index;
        this.mColor = color;
        this.mLayoutParams = lp;
        this.mHeight = mHeight;
        this.mWebView = webView;
        this.mIWebLayout = webLayout;
    }

    /**
     * 关闭进度条
     */
    public DefaultWebCreator(@NonNull Activity activity, @Nullable ViewGroup viewGroup, ViewGroup.LayoutParams lp, int index, @Nullable WebView webView, IWebLayout webLayout) {
        this.mActivity = activity;
        this.mViewGroup = viewGroup;
        this.mIsNeedDefaultProgress = false;
        this.mIndex = index;
        this.mLayoutParams = lp;
        this.mWebView = webView;
        this.mIWebLayout = webLayout;
    }

    /**
     * 自定义Indicator
     */
    public DefaultWebCreator(@NonNull Activity activity, @Nullable ViewGroup viewGroup, ViewGroup.LayoutParams lp, int index, BaseIndicatorView progressView, WebView webView, IWebLayout webLayout) {
        this.mActivity = activity;
        this.mViewGroup = viewGroup;
        this.mIsNeedDefaultProgress = false;
        this.mIndex = index;
        this.mLayoutParams = lp;
        this.mProgressView = progressView;
        this.mWebView = webView;
        this.mIWebLayout = webLayout;
    }

    public void setWebView(WebView webView) {
        mWebView = webView;
    }

    @NonNull
    @Override
    public DefaultWebCreator create() {
        if (mIsCreated) {
            return this;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // 安卓9.0后不允许多进程使用同一个数据目录，需设置前缀来区分
            // 参阅 https://blog.csdn.net/lvshuchangyin/article/details/89446629
            Context context = mActivity;
            String processName = ProcessUtils.getCurrentProcessName(context);
            if (!context.getApplicationContext().getPackageName().equals(processName)) {
                try {
                    WebView.setDataDirectorySuffix(processName);
                } catch (Throwable ignored) {
                }
            }
        }
        mIsCreated = true;
        ViewGroup mViewGroup = this.mViewGroup;
        this.mFrameLayout = (FrameLayout) createLayout();
        if (mViewGroup == null) {
            mViewGroup = this.mFrameLayout;
            mActivity.setContentView(mViewGroup);
        } else {
            if (mIndex == -1) {
                mViewGroup.addView(this.mFrameLayout, mLayoutParams);
            } else {
                mViewGroup.addView(this.mFrameLayout, mIndex, mLayoutParams);
            }
        }
        return this;
    }

    @Override
    public WebView getWebView() {
        return mWebView;
    }

    @Override
    @NonNull
    public FrameLayout getWebParentLayout() {
        return mFrameLayout;
    }

    @Override
    public int getWebViewType() {
        return this.mWebViewType;
    }

    private ViewGroup createLayout() {
        Activity mActivity = this.mActivity;
        WebParentLayout mFrameLayout = new WebParentLayout(mActivity);
        mFrameLayout.setId(R.id.scaffold_web_parent_layout_id);
        mFrameLayout.setBackgroundColor(Color.WHITE);
        FrameLayout.LayoutParams mLayoutParams = new FrameLayout.LayoutParams(-1, -1);
        this.mWebView = createWebView();
        View target;
        if (mIWebLayout == null) {
            target = this.mWebView;
        } else {
            target = webLayout();
        }
        mFrameLayout.addView(target, mLayoutParams);
        mFrameLayout.bindWebView(this.mWebView);
        Timber.i("  instanceof  AgentWebView:" + (this.mWebView instanceof AgentWebView));
        if (this.mWebView instanceof AgentWebView) {
            this.mWebViewType = WebConfig.WEB_VIEW_AGENT_WEB_SAFE_TYPE;
        }
        ViewStub mViewStub = new ViewStub(mActivity);
        mViewStub.setId(R.id.scaffold_mainframe_error_viewsub_id);
        mFrameLayout.addView(mViewStub, new FrameLayout.LayoutParams(-1, -1));
        if (mIsNeedDefaultProgress) {
            FrameLayout.LayoutParams lp;
            WebIndicator mWebIndicator = new WebIndicator(mActivity);
            if (mHeight > 0) {
                lp = new FrameLayout.LayoutParams(-2, DimensionsKt.dip(mActivity, mHeight));
            } else {
                lp = mWebIndicator.offerLayoutParams();
            }
            if (mColor != -1) {
                mWebIndicator.setColor(mColor);
            }
            lp.gravity = Gravity.TOP;
            this.mBaseIndicatorSpec = mWebIndicator;
            mFrameLayout.addView((View) this.mBaseIndicatorSpec, lp);
            mWebIndicator.setVisibility(View.GONE);
        } else if (mProgressView != null) {
            this.mBaseIndicatorSpec = mProgressView;
            mFrameLayout.addView((View) (this.mBaseIndicatorSpec), mProgressView.offerLayoutParams());
            mProgressView.setVisibility(View.GONE);
        }
        return mFrameLayout;
    }

    private View webLayout() {
        WebView mWebView = mIWebLayout.getWebView();
        if (mWebView == null) {
            mWebView = createWebView();
            mIWebLayout.getLayout().addView(mWebView, -1, -1);
            Timber.i("add webview");
        } else {
            this.mWebViewType = WebConfig.WEB_VIEW_CUSTOM_TYPE;
        }
        this.mWebView = mWebView;
        return mIWebLayout.getLayout();
    }

    private WebView createWebView() {
        WebView mWebView;
        if (this.mWebView != null) {
            mWebView = this.mWebView;
            this.mWebViewType = WebConfig.WEB_VIEW_CUSTOM_TYPE;
        } else if (WebConfig.IS_KITKAT_OR_BELOW_KITKAT) {
            mWebView = new AgentWebView(mActivity);
            this.mWebViewType = WebConfig.WEB_VIEW_AGENT_WEB_SAFE_TYPE;
        } else {
            mWebView = new LollipopFixedWebView(mActivity);
            this.mWebViewType = WebConfig.WEB_VIEW_DEFAULT_TYPE;
        }
        return mWebView;
    }

    @Override
    @NonNull
    public BaseIndicatorSpec offer() {
        return mBaseIndicatorSpec;
    }
}
