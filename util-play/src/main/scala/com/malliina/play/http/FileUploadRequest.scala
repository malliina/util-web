package com.malliina.play.http

import java.nio.file.Path

import play.api.mvc.Request

class FileUploadRequest[A](val files: Seq[Path], user: String, request: Request[A])
  extends AuthRequest(user, request)
