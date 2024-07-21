package com.malliina.http

import com.malliina.values.Readable
import org.typelevel.ci.{CIString, CIStringSyntax}

case class CSRFConf(tokenName: String, cookieName: String, headerName: CIString, noCheck: String)

object CSRFConf:
  val default: CSRFConf = CSRFConf(
    "csrfToken",
    "csrfToken",
    ci"Csrf-Token",
    "nocheck"
  )

opaque type CSRFToken = String

object CSRFToken:
  def apply(s: String): CSRFToken = s

  extension (t: CSRFToken) def value: String = t

  given Readable[CSRFToken] = Readable.string.map(apply)
