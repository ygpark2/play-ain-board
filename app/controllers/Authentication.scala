package controllers

import java.time.OffsetDateTime
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
import models.{NormalRole, User, Users}
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
    Future( Ok(views.html.auth.signup(userForms.addAccount)) )
  }

  /**
    * Handles the form filled by the user. The user and its password are saved and it sends him an email
    * with a link to verify his email address.
    */
  def handleSignUp() = Action.async { implicit request =>
    logger.debug("handle signup================> ")
    userForms.addAccount.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.auth.signup(formWithErrors))),
      userFormData => {
        if(userFormData.password.nonEmpty && userFormData.password == userFormData.passwordAgain) {
          println("--------------- password matched -------------------------------")
          val salt = generateSalt
          for {
            id <- userForms.users.add(UUID.randomUUID(), userFormData.name, userFormData.email, false, userFormData.password.bcrypt(salt), salt, NormalRole)
            token <- tokenService.create(MailTokenUser(userFormData.email, isSignUp = true))
            user <- userForms.users.find(id)
          } yield {
            println("created id => " + id)
            println("created token => " + token)
            user match {
              case Some(u) => {
                Mailer.welcome(u, link = routes.Authentication.verifySignUp(token.get.id).absoluteURL())
                Ok(views.html.auth.almostSignedUp(userFormData))
              }
              case None => {
                None
                Ok(views.html.auth.almostSignedUp(userFormData))
              }
            }
          }

        } else {
          val form = userForms.addAccount.fill(userFormData).withError("passwordAgain", "Passwords don't match")
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
            u.emailConfirmed = true
            userForms.users.update(u)
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