package com.malliina.http

import javax.net.ssl.HostnameVerifier

import com.malliina.security.SSLUtils
import org.apache.http.conn.ssl.{NoopHostnameVerifier, SSLConnectionSocketFactory}

object ApacheHttpHelper extends ApacheHttpHelper

trait ApacheHttpHelper {
  /**
    *
    * @return a socket factory that trusts all server certificates
    */
  def allowAllCertificatesSocketFactory(hostnameVerifier: HostnameVerifier = NoopHostnameVerifier.INSTANCE) =
    new SSLConnectionSocketFactory(SSLUtils.trustAllSslContext(), hostnameVerifier)
}
