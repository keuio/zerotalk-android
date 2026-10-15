package top.lanxint.zerotalk.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val PACKAGE_NAME = "top.lanxint.zerotalk"

/**
 * 采集 ZeroTalk 的启动期 Baseline Profile。
 *
 * 覆盖路径刻意只包含「冷启动 → 首启说明 → 首页」这条主线：
 * Baseline Profile 应当只收录启动必经代码。把只在特定功能里才会用到的类
 * 也塞进来会挤占 AOT 编译预算，反而拖慢真正的启动路径。
 *
 * 由 `:composeApp:generateBaselineProfile` 驱动，产出写回
 * `composeApp/src/androidMain/generated/baselineProfiles/baseline-prof.txt`
 * （路径由 :composeApp 的 `baselineProfile { mergeIntoMain = true }` 决定）。
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(
        packageName = PACKAGE_NAME,
        // 把这段覆盖的方法同时登记进 Startup Profile —— 冷启动优化真正吃的是它。
        // 不开启时只会产出 Baseline Profile，Gradle 会警告 "No startup profile rules were generated"。
        includeInStartupProfile = true
    ) {
        pressHome()
        startActivityAndWait()
        device.wait(Until.hasObject(By.pkg(PACKAGE_NAME)), 5_000)

        // 首次安装会出现「使用说明与须知」浮层，点掉它才能进首页。
        // 非首次启动时该节点不存在，findObject 返回 null，不会报错。
        device.findObject(By.text("我已阅读并知悉"))?.click()
        device.waitForIdle()

        // 等首页核心卡片完成一次布局
        device.wait(Until.hasObject(By.text("开始匹配")), 5_000)
        device.waitForIdle()
    }
}
