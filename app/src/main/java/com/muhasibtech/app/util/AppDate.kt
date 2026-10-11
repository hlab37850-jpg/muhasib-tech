package com.muhasibtech.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppDate {

    fun today(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
}
