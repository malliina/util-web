package com.malliina.http

import com.malliina.security.SSLUtils
import org.apache.http.conn.ssl.{SSLConnectionSocketFactory, X509HostnameVerifier}

trait ApacheHttpHelper {
  /**
   *
   * @return a socket factory that trusts all server certificates
   */
  def allowAllCertificatesSocketFactory(hostnameVerifier: X509HostnameVerifier = SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER) =
    new SSLConnectionSocketFactory(SSLUtils.trustAllSslContext(), hostnameVerifier)
}

object ApacheHttpHelper extends ApacheHttpHelper
