package com.afyaquest.app.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Small date helpers shared by screens that track "done today" state.
 * All persisted dates use ISO yyyy-MM-dd in the device's local time zone.
 */
object DateUtils {
    /** Today's local date as yyyy-MM-dd. */
    fun todayIso(): String = LocalDate.now().toString()

    /**
     * Format an ISO date (yyyy-MM-dd, or a longer ISO timestamp) for display in the
     * user's locale, e.g. "Aug 25, 2026" / "25 ago 2026". Falls back to the raw
     * input when it cannot be parsed.
     */
    fun formatLocalized(isoDate: String?, locale: Locale = Locale.getDefault()): String {
        if (isoDate.isNullOrBlank()) return ""
        return try {
            LocalDate.parse(isoDate.take(10))
                .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
        } catch (e: Exception) {
            isoDate
        }
    }

    /** True when the ISO date string is strictly before today (overdue). */
    fun isBeforeToday(isoDate: String?): Boolean {
        if (isoDate.isNullOrBlank()) return false
        return try {
            LocalDate.parse(isoDate.take(10)).isBefore(LocalDate.now())
        } catch (e: Exception) {
            false
        }
    }
}
