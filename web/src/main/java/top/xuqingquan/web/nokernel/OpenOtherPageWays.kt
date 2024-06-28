package top.xuqingquan.web.nokernel

/**
 * Created by 许清泉 on 2019-06-19 00:30
 */
@Suppress("unused")
enum class OpenOtherPageWays(var code: Int) {

    /**
     * 直接打开跳转页
     */
    DIRECT(WebConfig.DIRECT_OPEN_OTHER_PAGE),
    /**
     * 咨询用户是否打开
     */
    ASK(WebConfig.ASK_USER_OPEN_OTHER_PAGE),
    /**
     * 禁止打开其他页面
     */
    DISALLOW(WebConfig.DISALLOW_OPEN_OTHER_APP)
}
