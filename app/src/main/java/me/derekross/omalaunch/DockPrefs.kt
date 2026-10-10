package me.derekross.omalaunch

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Persists the user-chosen dock. Order preserved, empty allowed, max [MAX] entries. */
object DockPrefs {
    private const val FILE = "omalaunch"
    private const val KEY = "dock"
    const val MAX = 5

    fun load(context: Context): List<AppModel> {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.getJSONObject(i)
                AppModel(
                    label = o.getString("label"),
                    packageName = o.getString("pkg"),
                    activityName = o.getString("act"),
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(context: Context, apps: List<AppModel>) {
        val arr = JSONArray()
        apps.forEach { a ->
            arr.put(
                JSONObject()
                    .put("label", a.label)
                    .put("pkg", a.packageName)
                    .put("act", a.activityName),
            )
        }
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, arr.toString())
            .apply()
    }
}
