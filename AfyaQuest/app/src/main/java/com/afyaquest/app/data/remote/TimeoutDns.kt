package com.afyaquest.app.data.remote

import okhttp3.Dns
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Name lookups with a deadline.
 *
 * The system resolver has no deadline of its own and OkHttp's connect timeout does not cover it.
 * On a network that swallows DNS packets (a captive portal, a weak rural link, an emulator woken
 * from a snapshot) a lookup can hang for minutes, and with it the Sign In spinner. Failing after
 * [timeoutSeconds] turns that into an ordinary "no connection" error the user can act on.
 */
class TimeoutDns(
    private val timeoutSeconds: Long = DEFAULT_TIMEOUT_SECONDS,
    private val delegate: Dns = Dns.SYSTEM
) : Dns {

    private val executor = Executors.newCachedThreadPool { runnable ->
        Thread(runnable, "dns-lookup").apply { isDaemon = true }
    }

    override fun lookup(hostname: String): List<InetAddress> {
        val pending = executor.submit<List<InetAddress>> { delegate.lookup(hostname) }
        try {
            return pending.get(timeoutSeconds, TimeUnit.SECONDS)
        } catch (e: TimeoutException) {
            pending.cancel(true)
            throw UnknownHostException("Looking up $hostname took longer than $timeoutSeconds s")
        } catch (e: ExecutionException) {
            throw (e.cause as? UnknownHostException)
                ?: UnknownHostException(hostname).apply { initCause(e.cause) }
        } catch (e: InterruptedException) {
            pending.cancel(true)
            Thread.currentThread().interrupt()
            throw UnknownHostException(hostname)
        }
    }

    companion object {
        const val DEFAULT_TIMEOUT_SECONDS = 8L
    }
}
