package com.malliina.play.models

import com.malliina.play.json.SimpleCompanion
import play.api.data.format.Formats.stringFormat

case class Password(pass: String) {
  override def toString: String = pass
}

object Password extends SimpleCompanion[String, Password] {
  override def raw(p: Password): String = p.pass
}
