package com.malliina.play.tags

import play.api.mvc.Call

import scalatags.Text.GenericAttr

object PlayTags extends PlayTags

trait PlayTags {
  implicit val callAttr = new GenericAttr[Call]
}
