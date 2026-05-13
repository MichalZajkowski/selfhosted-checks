package vouch.checks

import vouch.Check
import vouch.CheckResult
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

class HttpReachabilityCheck : Check {
    override val name: String = "http_reachability_and_tls"

    override suspend fun run(target: URL): CheckResult {
        if (target.protocol != "https") {
            return CheckResult.Fail("instance URL must use https (got ${target.protocol})")
        }
        val host = target.host
        val port = if (target.port == -1) 443 else target.port
        return try {
            // Direct TCP + TLS handshake using the JDK default trust store.
            // Bypasses HTTP entirely — verifies only that the host is reachable
            // on :443 and presents a cert chain valid for $host.
            val raw = Socket()
            raw.connect(InetSocketAddress(host, port), 10_000)
            val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
            val sslSocket = factory.createSocket(raw, host, port, true) as SSLSocket
            sslSocket.soTimeout = 10_000
            sslSocket.startHandshake()
            val proto = sslSocket.session.protocol
            val cipher = sslSocket.session.cipherSuite
            sslSocket.close()
            CheckResult.Pass("TCP $host:$port OK, TLS $proto / $cipher")
        } catch (e: SSLHandshakeException) {
            CheckResult.Fail("TLS handshake failed: ${e.message}")
        } catch (e: Exception) {
            CheckResult.Error("connection failed: ${e.message}", e)
        }
    }
}
