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

case class SignupFormData(name:String, email: String, password: String, passwordAgain:String)

case class ProfileFormData(
                          phone: String, mobile: String, sex: String, zipcode: Int, address1: String,
                          address2: String, introduction: String
                         )
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
    )(SignupFormData.apply)(SignupFormData.unapply)
  )

  val updateAccount = userForm(text)

  val registerAccount = userForm(nonEmptyText)

  private[this] def profileForm() = Form(
    mapping(
      "phone" -> text,
      "mobile" -> text,
      "sex" -> text,
      "zipcode" -> number,
      "address1" -> text,
      "address2" -> text,
      "introduction" -> text
    )(ProfileFormData.apply)(ProfileFormData.unapply)
  )

  val updateProfile = profileForm()

  val registerProfile = profileForm()

}