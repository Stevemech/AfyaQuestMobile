package com.afyaquest.app.util

/**
 * Reasons a sign-in or sign-up can fail that the app can explain itself, in the user's language.
 * They travel as the message of a [Resource.Error]; any other message is text from the server.
 */
object AuthErrors {
    const val NO_CONNECTION = "auth/no-connection"
    const val TIMEOUT = "auth/timeout"
    const val INVALID_CREDENTIALS = "auth/invalid-credentials"
    const val EMAIL_NOT_CONFIRMED = "auth/email-not-confirmed"
    const val ACCOUNT_INCOMPLETE = "auth/account-incomplete"
    const val SERVER = "auth/server"

    /**
     * What a failed sign-in means, from the HTTP status and the server's own error text.
     * A 404 ("no such user") is reported as wrong credentials: the sign-in screen should not
     * tell a stranger which email addresses have an account.
     */
    fun forSignIn(httpCode: Int, serverMessage: String?): String = when {
        httpCode == 403 -> EMAIL_NOT_CONFIRMED
        // The password was right but there is no profile behind the account.
        httpCode == 409 -> ACCOUNT_INCOMPLETE
        // A disabled account gets its own explanation from the server ("pending approval").
        httpCode == 401 && serverMessage != null && serverMessage.contains("pending", ignoreCase = true) -> serverMessage
        httpCode == 401 || httpCode == 404 -> INVALID_CREDENTIALS
        httpCode >= 500 -> SERVER
        else -> serverMessage?.takeIf { it.isNotBlank() } ?: SERVER
    }
}
