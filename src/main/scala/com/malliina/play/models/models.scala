package com.malliina.play.models

import com.malliina.play.json.SimpleCompanion
import play.api.data.format.Formats.stringFormat

case class Username(name: String) {
  override def toString = name
}

object Username extends SimpleCompanion[String, Username] {
  val empty = Username("")

  override def raw(t: Username): String = t.name
}

case class Password(pass: String) {
  override def toString: String = pass
}

object Password extends SimpleCompanion[String, Password] {
  override def raw(p: Password): String = p.pass
}

case class Email(email: String) {
  override def toString = email
}

object Email extends SimpleCompanion[String, Email] {
  override def raw(t: Email) = t.email
}
