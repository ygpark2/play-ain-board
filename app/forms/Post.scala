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

case class NewPostFormData(
                        title: String,
                        content: String,
                        password: String,
                        username: String,
                        email: String,
                        homepage: String,
                        enable_secret: Boolean,
                        enable_html: Boolean,
                        enable_notice: Boolean
                      ) {

}

/*
case class PostConf(
                     enable_secret: Boolean,
                     enable_html: Boolean,
                     enable_hide_comment: Boolean,
                     enable_notice: Boolean,
                     enable_receive_email: Boolean
                   )

case class PostInfo(
                     link_count: Int,
                     hit: Int,
                     like: Int,
                     dislike: Int,
                     ip_address: String,
                     blame_count: Int,
                     device: String,
                     attached_file_count: Int,
                     attached_image_count: Int

                        board: UUID,
                        category: UUID,
                        title: String,
                        content: String,
                        password: String,
                        created_datetime: Timestamp,
                        updated_datetime: Timestamp,
                        updated_userid: String,
                        comment_count: Int,
                        comment_updated_datetime: Timestamp,
                        postUser: PostUser,
                        postConf: PostConf,
                        postInfo: PostInfo,
                        deleted: Boolean
 */

// @Singleton
class PostForms @Inject()(val posts: Posts) {

  private[this] def form() = Form(
    mapping(
      "title" -> nonEmptyText,
      "content" -> nonEmptyText,
      "password" -> text,
      "username" -> text,
      "email" -> email,
      "homepage" -> text,
      "enable_secret" -> boolean,
      "enable_html" -> boolean,
      "enable_notice" -> boolean
    )(NewPostFormData.apply)(NewPostFormData.unapply)
  )

  val newForm = form()

  // val registerProfile = profileForm()

}