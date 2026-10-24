package com.playmusic.app

import android.content.Context

class SettingsStore(context: Context) {
    private val p = context.getSharedPreferences("play_music_settings", Context.MODE_PRIVATE)
    var darkMode: Boolean
        get() = p.getBoolean("dark", false)
        set(v) = p.edit().putBoolean("dark", v).apply()
}
