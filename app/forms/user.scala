package forms

import javax.inject.Inject

import models.Users
import play.api.data.validation.Constraints._
import play.api.data.validation.{Constraint, Invalid, Valid}

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import play.api.data.{Form, Mapping}
import play.api.data.Forms._
import services.DBService

case class LoginFormData(email: String, password: String)

case class UserFormData(name:String, email: String, password: String, passwordAgain:String)

// @Singleton
class UserForms @Inject()(val users: Users) {

  val login = Form(
    mapping(
      "email" -> email,
      "password" -> nonEmptyText
    )(LoginFormData.apply)(LoginFormData.unapply)
  )

  val uniqueEmail = Constraint[String] { email: String =>
    import scala.concurrent.ExecutionContext.Implicits.global

    val userFuture = users.findByEmail(email)

    Await.result(userFuture, Duration.Inf) match {
      case None => Valid
      case user => Invalid("email already taken")
      case _ => Valid
    }
  }

  private[this] def userForm(passwordMapping:Mapping[String]) = Form(
    mapping(
      "name" -> nonEmptyText,
      "email" -> email.verifying(maxLength(250), uniqueEmail),
      "password" -> passwordMapping,
      "passwordAgain" -> passwordMapping
    )(UserFormData.apply)(UserFormData.unapply)
  )

  val updateAccount = userForm(text)

  val addAccount = userForm(nonEmptyText)

}