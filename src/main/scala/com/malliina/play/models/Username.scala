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