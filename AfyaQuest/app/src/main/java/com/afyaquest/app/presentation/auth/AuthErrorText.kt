package com.afyaquest.app.presentation.auth

import android.content.Context
import com.afyaquest.app.R
import com.afyaquest.app.util.AuthErrors

/**
 * The text to show for a failed sign-in / sign-up / password reset.
 *
 * Reasons the app knows ([AuthErrors]) are explained in the user's language; anything else is the
 * server's own sentence. Raw exception text never reaches the screen.
 */
fun authErrorText(context: Context, message: String?, fallback: String): String = when (message) {
    AuthErrors.NO_CONNECTION -> context.getString(R.string.auth_error_no_connection)
    AuthErrors.TIMEOUT -> context.getString(R.string.auth_error_timeout)
    AuthErrors.INVALID_CREDENTIALS -> context.getString(R.string.auth_error_invalid_credentials)
    AuthErrors.EMAIL_NOT_CONFIRMED -> context.getString(R.string.auth_error_email_not_confirmed)
    AuthErrors.ACCOUNT_INCOMPLETE -> context.getString(R.string.auth_error_account_incomplete)
    AuthErrors.SERVER -> context.getString(R.string.auth_error_server)
    null, "" -> fallback
    else -> message
}
