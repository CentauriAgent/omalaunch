package me.derekross.omalaunch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import me.derekross.omalaunch.ui.HomeScreen
import me.derekross.omalaunch.ui.theme.OmalaunchTheme

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(0),
            navigationBarStyle = SystemBarStyle.dark(0),
        )
        setContent {
            OmalaunchTheme {
                val vm: LauncherViewModel = viewModel()
                HomeScreen(vm)
            }
        }
    }
}
