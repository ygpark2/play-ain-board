package forms

import java.sql.Timestamp
import java.util.UUID
import javax.inject.Inject

import models._
import play.api.data.validation.Constraints._
import play.api.data.validation.{Constraint, Invalid, Valid}

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import play.api.data.{Form, Mapping}
import play.api.data.Forms._

case class NewCommentFormData(
                        content: String,
                        password: String,
                        username: String,
                        email: String,
                        homepage: String,
                        enable_secret: Boolean,
                        enable_html: Boolean
                      ) {

}

class CommentForms @Inject()(val comments: Comments) {

  private[this] def form() = Form(
    mapping(
      "content" -> nonEmptyText,
      "password" -> text,
      "username" -> text,
      "email" -> email,
      "homepage" -> text,
      "enable_secret" -> boolean,
      "enable_html" -> boolean
    )(NewCommentFormData.apply)(NewCommentFormData.unapply)
  )

  val newForm = form()

  // val registerProfile = profileForm()

}