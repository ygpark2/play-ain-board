package controllers

import java.time.{LocalDateTime, OffsetDateTime}
import java.util.UUID
import javax.inject.Inject

import com.github.t3hnar.bcrypt.Password
import org.pac4j.core.client.{Clients, IndirectClient}
import org.pac4j.http.client.indirect.FormClient
import org.pac4j.jwt.profile.JwtGenerator
import play.api.mvc._
import org.pac4j.core.profile._
import org.pac4j.core.util.CommonHelper
import org.pac4j.play.PlayWebContext
import org.pac4j.play.scala._
import play.api.libs.json.Json
import org.pac4j.core.credentials.Credentials
import javax.inject.Inject

import play.libs.concurrent.HttpExecutionContext
import org.pac4j.core.config.Config
import org.pac4j.core.context.Pac4jConstants
import org.pac4j.jwt.config.signature.SecretSignatureConfiguration
import org.pac4j.play.store.PlaySessionStore

import scala.collection.JavaConversions._
import forms.UserForms
import models._
import models.account.MailTokenUser
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import services.{MailService, MailTokenUserService}
import utils.Mailer

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import com.github.t3hnar.bcrypt._

class Authentication @Inject() (val config: Config,
                             val messagesApi: MessagesApi,
                             val mailService: MailService,
                             val userForms: UserForms,
                             val playSessionStore: PlaySessionStore,
                             override val ec: HttpExecutionContext) extends Controller with Security[CommonProfile] with I18nSupport {

  implicit val ms = mailService
  val tokenService = new MailTokenUserService()

  private def getProfiles(implicit request: RequestHeader): List[CommonProfile] = {
    val webContext = new PlayWebContext(request, playSessionStore)
    val profileManager = new ProfileManager[CommonProfile](webContext)
    val profiles = profileManager.getAll(true)
    asScalaBuffer(profiles).toList
  }

  /**
    * Starts the sign up mechanism. It shows a form that the user have to fill in and submit.
    */
  def signup() = Action.async { implicit request =>
    Future( Ok(views.html.auth.signup(userForms.registerAccount)) )
  }

  /**
    * Handles the form filled by the user. The user and its password are saved and it sends him an email
    * with a link to verify his email address.
    */
  def handleSignUp() = Action.async { implicit request =>
    logger.debug("handle signup================> ")
    userForms.registerAccount.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.auth.signup(formWithErrors))),
      signupFormData => {
        if(signupFormData.password.nonEmpty && signupFormData.password == signupFormData.passwordAgain) {
          println("--------------- password matched -------------------------------")
          val salt = generateSalt
          val userInfo = UserInfo(signupFormData.phone, signupFormData.mobile, signupFormData.sex, signupFormData.zipcode.toString, signupFormData.address1, signupFormData.address2, "", "", signupFormData.introduction)
          val userConf = UserConf(1, 1, 1, 1, java.sql.Timestamp.valueOf(LocalDateTime.now()), "255.141.38.200", java.sql.Timestamp.valueOf(LocalDateTime.now()), "232.133.48.102")
          val user = User(UUID.randomUUID(), Common(), signupFormData.name, signupFormData.email, false, signupFormData.password.bcrypt(salt), salt, 2, 283, userInfo, userConf, NormalRole)
          for {
            id <- userForms.users.insert(user)
            token <- tokenService.create(MailTokenUser(signupFormData.email, isSignUp = true))
          } yield {
            println("created id => " + id)
            println("created token => " + token)
            if (id > 0) {
              token match {
                case Some(t) => {
                  Mailer.welcome(user, link = routes.Authentication.verifySignUp(t.id).absoluteURL())
                  Ok(views.html.auth.almostSignedUp(signupFormData))
                }
                case None => {
                  None
                  Ok(views.html.auth.almostSignedUp(signupFormData))
                }
              }
            } else {
              Ok(views.html.auth.almostSignedUp(signupFormData))
            }
          }
        } else {
          val form = userForms.registerAccount.fill(signupFormData).withError("passwordAgain", "Passwords don't match")
          Future.successful(BadRequest(views.html.auth.signup(form)))
        }
      }
    )
  }

  /**
    * Verifies the user's email address based on the token.
    */
  def verifySignUp(tokenId: String) = Action.async { implicit request =>
    tokenService.retrieve(tokenId).flatMap {
      case Some(token) if (token.isSignUp && !token.isExpired) => {
        tokenService.consume(tokenId)
        // set email confirmed to true for user
        for {
          user <- userForms.users.findByEmail(token.email)
        } yield {
          user.map(u => {
            userForms.users.update(u.withEmailConfirmed(true))
          })
          Ok("verified!!!!!!!!!!!")
        }
      }
      case Some(token) =>
        tokenService.consume(tokenId)
        Future.successful(NotFound(views.html.error.notFound(request)))
      case _ =>
        Future.successful(NotFound(views.html.error.notFound(request)))
    }
  }

}