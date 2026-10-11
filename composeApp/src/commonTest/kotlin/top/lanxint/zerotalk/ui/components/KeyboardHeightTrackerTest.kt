package top.lanxint.zerotalk.ui.components

import androidx.compose.ui.unit.dp
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class KeyboardHeightTrackerTest {

    @BeforeTest
    @AfterTest
    fun resetTracker() {
        KeyboardHeightTracker.resetForTest(280.dp)
    }

    @Test
    fun `过滤小于200dp异常与导航栏高度`() {
        assertEquals(280.dp, KeyboardHeightTracker.lastKeyboardHeightDp)

        KeyboardHeightTracker.updatePeak(48.dp)
        assertEquals(280.dp, KeyboardHeightTracker.lastKeyboardHeightDp)

        KeyboardHeightTracker.updatePeak(130.dp)
        assertEquals(280.dp, KeyboardHeightTracker.lastKeyboardHeightDp)

        KeyboardHeightTracker.updatePeak(199.dp)
        assertEquals(280.dp, KeyboardHeightTracker.lastKeyboardHeightDp)
    }

    @Test
    fun `单调递增峰值锁定杜绝收起动画冲刷降低`() {
        // 键盘展开并达到真实高度
        KeyboardHeightTracker.updatePeak(312.dp)
        assertEquals(312.dp, KeyboardHeightTracker.lastKeyboardHeightDp)

        // 键盘收起过程中，逐帧变小的高度（即使 >= 200dp）绝不冲刷覆盖最高峰值
        KeyboardHeightTracker.updatePeak(290.dp)
        assertEquals(312.dp, KeyboardHeightTracker.lastKeyboardHeightDp)

        KeyboardHeightTracker.updatePeak(250.dp)
        assertEquals(312.dp, KeyboardHeightTracker.lastKeyboardHeightDp)

        KeyboardHeightTracker.updatePeak(205.dp)
        assertEquals(312.dp, KeyboardHeightTracker.lastKeyboardHeightDp)

        KeyboardHeightTracker.updatePeak(200.dp)
        assertEquals(312.dp, KeyboardHeightTracker.lastKeyboardHeightDp)

        // 若后续出现更高峰值（如换了带候选栏的输入法），允许向上更新
        KeyboardHeightTracker.updatePeak(340.dp)
        assertEquals(340.dp, KeyboardHeightTracker.lastKeyboardHeightDp)

        // 再次收起依然锁定最高峰值
        KeyboardHeightTracker.updatePeak(260.dp)
        assertEquals(340.dp, KeyboardHeightTracker.lastKeyboardHeightDp)
    }

    @Test
    fun `完全静止展开态确切锁定静止高度`() {
        // 首个静止高度
        KeyboardHeightTracker.recordRestingHeight(260.dp)
        assertEquals(260.dp, KeyboardHeightTracker.lastKeyboardHeightDp)

        // 收起中间帧不影响
        KeyboardHeightTracker.updatePeak(220.dp)
        assertEquals(260.dp, KeyboardHeightTracker.lastKeyboardHeightDp)

        // 更高静止高度更新
        KeyboardHeightTracker.recordRestingHeight(320.dp)
        assertEquals(320.dp, KeyboardHeightTracker.lastKeyboardHeightDp)
    }

    @Test
    fun `静止展开态确切校准低于初始默认值的真实键盘高度`() {
        KeyboardHeightTracker.resetForTest(280.dp)
        // 真实键盘展开后实测高度为 250dp，静止态应准确校准为 250dp，杜绝面板高出一段
        KeyboardHeightTracker.recordRestingHeight(250.dp)
        assertEquals(250.dp, KeyboardHeightTracker.lastKeyboardHeightDp)
    }
}
