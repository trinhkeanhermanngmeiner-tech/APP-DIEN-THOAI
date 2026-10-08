package vn.aurora.launcher.ui

import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import vn.aurora.launcher.R
import vn.aurora.launcher.effects.AuroraRenderer
import vn.aurora.launcher.effects.GlassPanel

private fun Context.isDefaultHome(): Boolean =
    getSystemService(RoleManager::class.java)?.isRoleHeld(RoleManager.ROLE_HOME) == true

/** Opens the system's "default home app" screen, falling back to more general settings pages. */
private fun Context.openHomeSettings() {
    val candidates = listOf(
        Intent(Settings.ACTION_HOME_SETTINGS),
        Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
        Intent(Settings.ACTION_SETTINGS),
    )
    for (intent in candidates) {
        try {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        } catch (_: ActivityNotFoundException) {
            // Try the next, more general page.
        }
    }
}

/**
 * Shown while Aurora is not the default home app. Asks for the HOME role through the
 * system dialog; if the system refuses or skips the dialog (common on HyperOS/MIUI),
 * opens the default-apps settings instead. Hides itself once Aurora is the default.
 */
@Composable
fun DefaultHomeBanner(renderer: AuroraRenderer, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isDefault by remember { mutableStateOf(context.isDefaultHome()) }

    DisposableEffect(context, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) isDefault = context.isDefaultHome()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val roleRequest = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isDefault = context.isDefaultHome()
        if (!isDefault) context.openHomeSettings()
    }

    AnimatedVisibility(visible = !isDefault, modifier = modifier) {
        GlassPanel(
            renderer = renderer,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.default_home_message),
                    style = TextStyle(color = Color.White, fontSize = 14.sp, shadow = TextGlowShadow),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        val roleManager = context.getSystemService(RoleManager::class.java)
                        val intent = roleManager
                            ?.takeIf { it.isRoleAvailable(RoleManager.ROLE_HOME) }
                            ?.createRequestRoleIntent(RoleManager.ROLE_HOME)
                        if (intent == null) {
                            context.openHomeSettings()
                        } else {
                            runCatching { roleRequest.launch(intent) }
                                .onFailure { context.openHomeSettings() }
                        }
                    },
                ) {
                    Text(stringResource(R.string.set_default))
                }
            }
        }
    }
}
