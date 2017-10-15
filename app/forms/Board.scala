package forms

import javax.inject.Inject

import models.{Boards}
import play.api.data.validation.Constraints._
import play.api.data.validation.{Constraint, Invalid, Valid}

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import play.api.data.{Form, Mapping}
import play.api.data.Forms._

case class NewBoardFormData(key: String, name: String, mobile_name: String, order: Int, search_flag: Boolean)

// @Singleton
class BoardForms @Inject()(val boards: Boards) {

  private[this] def form() = Form(
    mapping(
      "key" -> text,
      "name" -> text,
      "mobile_name" -> text,
      "order" -> number,
      "search_flag" -> boolean
    )(NewBoardFormData.apply)(NewBoardFormData.unapply)
  )

  val newForm = form()

  // val registerProfile = profileForm()

}