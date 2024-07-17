package com.malliina.http4s

import cats.Applicative
import cats.syntax.all.toFunctorOps
import io.circe.parser.decode
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder}
import org.http4s.{Request, Response, ResponseCookie}

trait FeedbackSupport[F[_]: Applicative]:
  def feedbackCookieName = "feedback"

  extension (res: F[Response[F]])
    def withFeedback[T: Encoder](t: T): F[Response[F]] =
      withFeedbackMessage(t.asJson.noSpaces)
    def withFeedbackMessage(message: String): F[Response[F]] =
      res.map(_.addCookie(ResponseCookie(feedbackCookieName, message, path = Option("/"))))
    def clearFeedback: F[Response[F]] =
      res.map(_.removeCookie(feedbackCookieName))

  extension (req: Request[F])
    def feedback: Option[String] =
      req.cookies.find(_.name == feedbackCookieName).map(_.content)
    def feedbackAs[T: Decoder]: Option[T] =
      feedback.flatMap(f => decode[T](f).toOption)
