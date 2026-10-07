package vn.aurora.launcher.data

import android.content.ComponentName
import android.os.UserHandle
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import java.text.Normalizer

@Immutable
data class AppInfo(
    val label: String,
    val component: ComponentName,
    val user: UserHandle,
    val icon: ImageBitmap,
) {
    val packageName: String get() = component.packageName

    /** Unique across work/personal profiles, used as a stable list key. */
    val key: String = "${component.flattenToShortString()}#${user.hashCode()}"

    /** Lowercase, accent-free label so "tin nhan" matches "Tin nhắn". */
    val searchKey: String = label.normalizeForSearch()
}

private val combiningMarks = Regex("\\p{Mn}+")

fun String.normalizeForSearch(): String =
    Normalizer.normalize(lowercase(), Normalizer.Form.NFD)
        .replace(combiningMarks, "")
        .replace('đ', 'd')
