package me.derekross.omalaunch.ui

import android.content.ComponentName
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.derekross.omalaunch.AppModel
import me.derekross.omalaunch.LauncherViewModel
import me.derekross.omalaunch.toImageBitmap
import me.derekross.omalaunch.ui.theme.Accent
import me.derekross.omalaunch.ui.theme.PanelField
import me.derekross.omalaunch.ui.theme.PanelStroke
import me.derekross.omalaunch.ui.theme.TextDim
import me.derekross.omalaunch.ui.theme.TextMain

@Composable
fun DrawerSheet(vm: LauncherViewModel) {
    val apps by vm.apps.collectAsState()
    val context = LocalContext.current
    val focus = remember { FocusRequester() }
    BackHandler { vm.closeDrawer() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF51A1B26))
            .statusBarsPadding()
            .imePadding(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(PanelField)
                .border(1.dp, PanelStroke, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text(
                text = "❯ ",
                color = Accent,
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
            )
            BasicTextField(
                value = vm.query,
                onValueChange = { vm.updateQuery(it) },
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    color = TextMain,
                    fontSize = 16.sp,
                ),
                cursorBrush = SolidColor(Accent),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focus),
            )
            if (vm.query.isNotEmpty()) {
                Text(
                    text = "✕",
                    color = TextDim,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .clickable { vm.updateQuery("") }
                        .padding(start = 10.dp),
                )
            }
        }
        LaunchedEffect(Unit) { focus.requestFocus() }
        val filtered = remember(apps, vm.query) { fuzzy(apps, vm.query) }
        if (filtered.isEmpty() && vm.query.isNotBlank()) {
            Text(
                text = "no match for \"${vm.query}\"",
                color = TextDim,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                modifier = Modifier.padding(24.dp),
            )
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(filtered, key = { it.key }) { app ->
                AppIcon(
                    app = app,
                    onClick = {
                        vm.launch(context, app)
                        vm.closeDrawer()
                    },
                )
            }
        }
    }
}

@Composable
fun AppIcon(
    app: AppModel,
    iconSize: Dp = 52.dp,
    labelSize: TextUnit = 11.sp,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val icon by produceState<ImageBitmap?>(null, app.key) {
        value = withContext(Dispatchers.IO) {
            try {
                pm.getActivityIcon(ComponentName(app.packageName, app.activityName))?.toImageBitmap()
            } catch (_: Exception) {
                null
            }
        }
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 8.dp),
    ) {
        val bmp = icon
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = app.label,
                modifier = Modifier.size(iconSize),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(iconSize)
                    .background(Accent.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = app.label,
            color = TextMain,
            fontFamily = FontFamily.Monospace,
            fontSize = labelSize,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Wofi-flavored ranking: prefix > word-prefix > substring > subsequence. */
private fun fuzzy(apps: List<AppModel>, qRaw: String): List<AppModel> {
    val q = qRaw.trim().lowercase()
    if (q.isEmpty()) return apps
    data class Scored(val app: AppModel, val score: Int)
    val scored = ArrayList<Scored>()
    for (a in apps) {
        val l = a.label.lowercase()
        val s = when {
            l.startsWith(q) -> 100
            l.split(' ').any { it.startsWith(q) } -> 80
            l.contains(q) -> 60
            isSubsequence(q, l) -> 30
            else -> 0
        }
        if (s > 0) scored.add(Scored(a, s))
    }
    return scored
        .sortedWith(compareByDescending<Scored> { it.score }.thenBy { it.app.label.lowercase() })
        .map { it.app }
}

private fun isSubsequence(needle: String, haystack: String): Boolean {
    var i = 0
    for (c in haystack) {
        if (i < needle.length && needle[i] == c) i++
    }
    return i == needle.length
}
