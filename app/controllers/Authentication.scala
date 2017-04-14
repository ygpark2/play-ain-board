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
import org.joda.time.DateTime

class Authentication @Inject() (val config: Config,
                                val messagesApi: MessagesApi,
                                val tokenService: MailTokenUserService,
                                val userForms: UserForms,
                                val playSessionStore: PlaySessionStore,
                                implicit val mailService: MailService,
                                implicit val webJarAssets: WebJarAssets,
                                override val ec: HttpExecutionContext) extends Controller with Security[CommonProfile] with I18nSupport {

  // implicit val ms = mailService
  // val tokenService = new MailTokenUserService()
  def notFoundDefault(implicit request: RequestHeader) = Future.successful(NotFound(views.html.error.notFound(request)))

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
          /*
          val userInfo = UserInfo(signupFormData.phone, signupFormData.mobile, signupFormData.sex, signupFormData.zipcode.toString, signupFormData.address1, signupFormData.address2, "", "", signupFormData.introduction)
          val userConf = UserConf(1, 1, 1, 1, java.sql.Timestamp.valueOf(LocalDateTime.now()), "255.141.38.200", java.sql.Timestamp.valueOf(LocalDateTime.now()), "232.133.48.102")
          val user = User(UUID.randomUUID(), Common(), signupFormData.name, signupFormData.email, false, signupFormData.password.bcrypt(salt), salt, 2, 283, userInfo, userConf, NormalRole)
          */
          val user = User(UUID.randomUUID(), Common(), signupFormData.name, signupFormData.email, false, signupFormData.password.bcrypt(salt), salt, 2, 283, NormalRole)
          for {
            id <- userForms.users.insert(user)
            token <- tokenService.create(MailTokenUser(UUID.randomUUID(), signupFormData.email, DateTime.now, isSignUp = true))
          } yield {
            println("created id => " + id)
            println("created token => " + token)
            if (id > 0) {
              token match {
                case Some(t) => {
                  Mailer.welcome(user, link = routes.Authentication.verifySignUp(t.id.toString).absoluteURL())
                  Ok(views.html.auth.almostSignedUp(signupFormData))
                }
                case None => {
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
    val uuidToken = UUID.fromString(tokenId)
    tokenService.retrieve(uuidToken).flatMap {
      case Some(token) if (token.isSignUp && !token.isExpired) => {
        tokenService.consume(uuidToken)
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
        tokenService.consume(uuidToken)
        Future.successful(NotFound(views.html.error.notFound(request)))
      case _ =>
        Future.successful(NotFound(views.html.error.notFound(request)))
    }
  }

  def forgotPassword = Action.async { implicit request =>
    Future.successful(Ok(views.html.auth.forgotPassword(userForms.emailForm)))
    //Future.successful(request.identity match {
    //  case Some(_) => Redirect(routes.Application.index)
    //  case None => Ok(views.html.auth.forgotPassword(emailForm))
    //})
  }

  /**
    * Sends an email to the user with a link to reset the password
    */
  def handleForgotPassword = Action.async { implicit request =>
    userForms.emailForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.auth.forgotPassword(formWithErrors))),
      email => {
        val user = userForms.users.findByEmail(email)
        user.flatMap {
          case Some(user) =>
            val token = MailTokenUser(UUID.randomUUID(), email, DateTime.now, isSignUp = false)
            for {
              tokenUser <- tokenService.create(token)
            } yield {
              tokenUser.map(tu => {
                Mailer.forgotPassword(email, link = routes.Authentication.resetPassword(tu.id.toString).absoluteURL())
              })
              Ok(views.html.auth.forgotPasswordSent(email))
            }
          case _ =>
            Future.successful(NotFound(views.html.error.notFound(request)))
        }

        /*
        user.result.headOption.flatMap {
          case Some(user) =>
            val token = MailTokenUser(email, isSignUp = false)
            tokenService.create(token).map { _ =>
              Mailer.forgotPassword(email, link = routes.Authentication.resetPassword(token.id).absoluteURL())
              Ok(views.html.auth.forgotPasswordSent(email))
            }
          case None =>
            Future.successful(BadRequest(views.html.auth.forgotPassword(userForms.emailForm.withError("email", Messages("auth.user.notexists")))))
        }
        */
      }
    )
  }


  /**
    * Confirms the user's link based on the token and shows him a form to reset the password
    */
  def resetPassword(tokenId: String) = Action.async { implicit request =>
    val uuidToken = UUID.fromString(tokenId)
    tokenService.retrieve(uuidToken).flatMap {
      case Some(token) if (!token.isSignUp && !token.isExpired) => {
        Future.successful(Ok(views.html.auth.resetPassword(uuidToken, userForms.resetPasswordForm)))
      }
      case Some(token) => {
        tokenService.consume(uuidToken)
        Future.successful(NotFound(views.html.error.notFound(request)))
      }
      case None =>
        Future.successful(NotFound(views.html.error.notFound(request)))
    }
  }

  /**
    * Saves the new password and authenticates the user
    */
  def handleResetPassword(tokenId: String) = Action.async { implicit request =>
    val uuidToken = UUID.fromString(tokenId)
    userForms.resetPasswordForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.auth.resetPassword(uuidToken, formWithErrors))),
      passwords => {
        tokenService.retrieve(uuidToken).flatMap {
          case Some(token) if (!token.isSignUp && !token.isExpired) => {
            for {
              user <- userForms.users.findByEmail(token.email)
            } yield {
              user.map(u => {
                userForms.users.update(u.withEmailConfirmedPassword(true, passwords._1.bcrypt))
                tokenService.consume(uuidToken)
              })
              Ok("verified!!!!!!!!!!!")
            }

            /*
            val query = for { u <- Tables.Account if u.email === token.email } yield (u.emailConfirmed, u.password)
            val updateAction = query.update(true, passwords._1.bcrypt)
            val queryId = Tables.Account.filter (_.email === token.email)

            for {
              update <- database.runAsync(updateAction)
              user <- database.runAsync(queryId.result.headOption)
              result <- gotoLoginSucceeded(user.get.id)
            } yield {
              tokenService.consume(tokenId)
              result
            }
            */
          }
          case Some(token) => {
            tokenService.consume(uuidToken)
            notFoundDefault
          }
          case None => notFoundDefault
        }
      }
    )
  }
}