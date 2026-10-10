package me.derekross.omalaunch

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LauncherViewModel(app: Application) : AndroidViewModel(app) {

    private val pm = app.packageManager

    var drawerOpen by mutableStateOf(false)
        private set
    var query by mutableStateOf("")

    private val _apps = MutableStateFlow<List<AppModel>>(emptyList())
    val apps: StateFlow<List<AppModel>> = _apps.asStateFlow()

    private val _dock = MutableStateFlow<List<AppModel>>(emptyList())
    val dock: StateFlow<List<AppModel>> = _dock.asStateFlow()

    private val launchCounts = HashMap<String, Int>()

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val self = getApplication<Application>().packageName
            val list = pm.queryIntentActivities(main, 0)
                .map { ri ->
                    AppModel(
                        label = ri.loadLabel(pm).toString(),
                        packageName = ri.activityInfo.packageName,
                        activityName = ri.activityInfo.name,
                    )
                }
                .filter { it.packageName != self }
                .distinctBy { it.key }
                .sortedBy { it.label.lowercase() }
            _apps.value = list
            _dock.value = resolveDock()
        }
    }

    private fun resolveIntentApp(intent: Intent): AppModel? {
        return try {
            val ri = pm.resolveActivity(intent, 0) ?: return null
            val ai = ri.activityInfo ?: return null
            AppModel(
                label = ri.loadLabel(pm).toString().ifBlank { ai.packageName },
                packageName = ai.packageName,
                activityName = ai.name,
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveDock(): List<AppModel> {
        val dial = resolveIntentApp(Intent(Intent.ACTION_DIAL, Uri.parse("tel:")))
        val sms = resolveIntentApp(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")))
        val web = resolveIntentApp(Intent(Intent.ACTION_VIEW, Uri.parse("https://omarchy.org")))
        val cam = resolveIntentApp(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
        return listOfNotNull(dial, sms, web, cam)
            .distinctBy { it.packageName }
            .take(4)
    }

    fun openDrawer() {
        drawerOpen = true
    }

    fun closeDrawer() {
        drawerOpen = false
        query = ""
    }

    fun updateQuery(q: String) {
        query = q
    }

    fun launch(context: Context, app: AppModel) {
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setClassName(app.packageName, app.activityName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            launchCounts[app.key] = (launchCounts[app.key] ?: 0) + 1
        } catch (_: Exception) {
        }
    }
}
