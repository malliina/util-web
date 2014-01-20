package com.mle.http

import org.apache.http.conn.ssl.{SSLConnectionSocketFactory, X509HostnameVerifier}
import com.mle.security.SSLUtils

/**
 *
 * @author mle
 */
trait ApacheHttpHelper {
  /**
   *
   * @return a socket factory that trusts all server certificates
   */
  def allowAllCertificatesSocketFactory(hostnameVerifier: X509HostnameVerifier = SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER) =
    new SSLConnectionSocketFactory(SSLUtils.trustAllSslContext(), hostnameVerifier)
}

object ApacheHttpHelper extends ApacheHttpHelper
