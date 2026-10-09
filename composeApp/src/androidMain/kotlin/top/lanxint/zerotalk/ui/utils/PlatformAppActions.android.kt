package top.lanxint.zerotalk.ui.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

@Composable
actual fun rememberPlatformAppActions(): PlatformAppActions {
    val context = LocalContext.current
    return remember(context) {
        object : PlatformAppActions {
            override fun showToast(message: String) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }

            override fun exitApp() {
                context.findActivity()?.finish()
            }
        }
    }
}
