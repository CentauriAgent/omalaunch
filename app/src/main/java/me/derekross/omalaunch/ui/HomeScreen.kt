package me.derekross.omalaunch.ui

import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import me.derekross.omalaunch.LauncherViewModel
import me.derekross.omalaunch.ui.theme.Accent
import me.derekross.omalaunch.ui.theme.Panel
import me.derekross.omalaunch.ui.theme.PanelField
import me.derekross.omalaunch.ui.theme.PanelStroke
import me.derekross.omalaunch.ui.theme.Red
import me.derekross.omalaunch.ui.theme.TextDim
import me.derekross.omalaunch.ui.theme.TextMain

private val FMT_TIME = DateTimeFormatter.ofPattern("HH:mm")
private val FMT_DATE = DateTimeFormatter.ofPattern("EEE d MMM yyyy")

@Composable
fun HomeScreen(vm: LauncherViewModel) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0x55000000), Color(0x22000000), Color(0x55000000)),
                ),
            )
            .pointerInput(Unit) {
                var acc = 0f
                detectDragGestures(
                    onDragEnd = { acc = 0f },
                    onDragCancel = { acc = 0f },
                ) { change, amount ->
                    change.consume()
                    acc += amount.y
                    if (acc < -90f && !vm.drawerOpen) {
                        vm.openDrawer()
                        acc = 0f
                    }
                }
            },
    ) {
        Column(Modifier.fillMaxSize()) {
            PanelBar(pageCount = 2, currentPage = pagerState.currentPage, drawerOpen = vm.drawerOpen)
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                if (page == 0) ClockPage() else InfoPage(vm)
            }
            DockBar(vm)
        }
    }
    if (vm.drawerOpen) {
        DrawerSheet(vm)
    }
}

@Composable
private fun PanelBar(pageCount: Int, currentPage: Int, drawerOpen: Boolean) {
    val context = LocalContext.current
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var batt by remember { mutableStateOf(-1 to false) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            val i = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            if (i != null) {
                val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                val status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                if (level >= 0 && scale > 0) {
                    batt = (level * 100 / scale) to (status == BatteryManager.BATTERY_STATUS_CHARGING)
                }
            }
            delay(10_000)
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Panel)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { i ->
            Box(
                modifier = Modifier
                    .padding(end = 6.dp)
                    .size(7.dp)
                    .background(
                        if (i == currentPage) Accent else TextDim.copy(alpha = 0.4f),
                        CircleShape,
                    ),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = when {
                drawerOpen -> "drawer"
                currentPage == 0 -> "home"
                else -> "info"
            },
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = TextDim,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = when {
                batt.first >= 0 && batt.second -> "${batt.first}% ⚡"
                batt.first >= 0 -> "${batt.first}%"
                else -> ""
            },
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = if (batt.first in 0..19) Red else TextMain,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = now.format(FMT_TIME),
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            color = TextMain,
        )
    }
}

@Composable
private fun ClockPage() {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(15_000)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = now.format(FMT_TIME),
            fontFamily = FontFamily.Monospace,
            fontSize = 76.sp,
            fontWeight = FontWeight.Bold,
            color = TextMain,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = now.format(FMT_DATE),
            fontFamily = FontFamily.Monospace,
            fontSize = 16.sp,
            color = TextDim,
        )
        Spacer(Modifier.height(28.dp))
        Text(
            text = "⌃ swipe up for apps",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = TextDim,
        )
    }
}

@Composable
private fun InfoPage(vm: LauncherViewModel) {
    val context = LocalContext.current
    val apps by vm.apps.collectAsState()
    val isDefaultHome = remember {
        try {
            val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val def = context.packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
            def?.activityInfo?.packageName == context.packageName
        } catch (_: Exception) {
            false
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "omalaunch",
            fontFamily = FontFamily.Monospace,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Accent,
        )
        Text(
            text = "p0 · wofi",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = TextDim,
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = "${apps.size} apps indexed",
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            color = TextMain,
        )
        if (!isDefaultHome) {
            Spacer(Modifier.height(28.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(PanelField)
                    .border(1.dp, PanelStroke, RoundedCornerShape(12.dp))
                    .clickable {
                        try {
                            context.startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
                        } catch (_: Exception) {
                        }
                    }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Text(
                    text = "set as default home",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = Accent,
                )
            }
        }
        Spacer(Modifier.height(28.dp))
        Text(
            text = "themes + peridot sync land in p1/p2",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = TextDim,
        )
    }
}

@Composable
private fun DockBar(vm: LauncherViewModel) {
    val context = LocalContext.current
    val dock by vm.dock.collectAsState()
    if (dock.isEmpty()) {
        Text(
            text = "dock empty · long-press an app to pin it",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = TextDim,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 14.dp),
        )
        return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .background(Color(0x6616161E), RoundedCornerShape(20.dp))
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        dock.forEach { app ->
            AppIcon(
                app = app,
                iconSize = 44.dp,
                labelSize = 9.sp,
                onClick = { vm.launch(context, app) },
                onLongClick = { vm.removeFromDock(app) },
            )
        }
    }
}
