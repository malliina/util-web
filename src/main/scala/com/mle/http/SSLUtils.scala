package com.mle.http

import javax.net.ssl.{TrustManager, X509TrustManager, SSLContext}
import java.security.cert.X509Certificate

object SSLUtils {
  /**
   * Builds and initializes an [[javax.net.ssl.SSLContext]] that
   * trusts all certificates. Use this with SSL-enabled clients
   * that speak to servers with self-signed certificates.
   *
   * MITM BLAH BLAH BLAH
   *
   * @return an SSL context that trusts all certificates
   */
  def trustAllSslContext() = {
    val sslContext = SSLContext.getInstance("TLS")
    val trustManager = SSLUtils.trustAllTrustManager()
    sslContext.init(null, Array[TrustManager](trustManager), null)
    sslContext
  }

  def trustAllTrustManager() = new X509TrustManager() {
    override def checkClientTrusted(chain: Array[X509Certificate], authType: String) {
    }

    override def checkServerTrusted(chain: Array[X509Certificate], authType: String) {
    }

    override def getAcceptedIssuers: Array[X509Certificate] = null
  }
}