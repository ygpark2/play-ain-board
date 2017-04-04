package config

import javax.inject.Inject

import com.google.inject.Guice
import models.{User, Users}
import org.pac4j.core.context.WebContext
import org.pac4j.core.credentials.UsernamePasswordCredentials
import org.pac4j.core.credentials.authenticator.Authenticator
import org.pac4j.core.exception.{AccountNotFoundException, BadCredentialsException, MultipleAccountsFoundException, TechnicalException}
import org.pac4j.core.util.CommonHelper
import org.pac4j.core.credentials.password.PasswordEncoder
import org.pac4j.sql.profile.DbProfile

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class AinAuthenticator extends Authenticator[UsernamePasswordCredentials] {

  // val dbConfig = DatabaseConfigProvider.get[JdbcProfile]("default")(Play.current
  // val config = DatabaseConfigProvider.get[JdbcProfile]("somedb")
  // var users: Users = play.api.Play.current.injector.instanceOf

  lazy val users: Users = play.api.Play.current.injector.instanceOf(classOf[Users])

  val passwordEncoder = new BlowfishPasswordEncoder()

  /*
  val injector = Guice.createInjector()
  play.api.Play.current.injector
  val users = injector.getInstance(classOf[Users])
  */

  override def validate(creds: UsernamePasswordCredentials, ctx: WebContext):Unit = {

    println("+++++++++++++++++++++++++++++++++++++")
    // ...

    val username = creds.getUsername
    Await.result(users.findByEmail(creds.getUsername), Duration("10s")) match {
      case Some(user) =>
        val encodedPassword = user.password
        val returnedPassword = creds.getPassword
        println("expectedPassword : " + encodedPassword + " returnedPassword : " + returnedPassword)
        if (passwordEncoder.matches(creds.getPassword, user.password)) {
          val profile = createProfile(username, user)
          creds.setUserProfile(profile)
          println("------- setUserProfile -------------")
        }
        else
          throw new BadCredentialsException("Bad credentials for: " + username)
      case None => throw new AccountNotFoundException("No account found for: " + username)

    }

    /*
    user.onSuccess {
      case Some(user) => {
        val encodedPassword = user.password
        val returnedPassword = creds.getPassword
        println("expectedPassword : " + encodedPassword + " returnedPassword : " + returnedPassword)
        if (passwordEncoder.matches(creds.getPassword, user.password)) {
          val profile = createProfile(username, user)
          creds.setUserProfile(profile)
          println("------- setUserProfile -------------")
        }
        else
          throw new BadCredentialsException("Bad credentials for: " + username)

      }
      case None => {
        throw new AccountNotFoundException("No account found for: " + username)
      }
    }
    user.onFailure { case f => throw new TechnicalException("Cannot fetch username / password from DB", f) }
    */
  }

  protected def createProfile(username: String, user: User): DbProfile = {

    println("=============== creating profile ==================")

    val profile = new DbProfile
    profile.setId(user.id)

    profile.addAttribute("name", user.name)
    profile.addAttribute("email", user.email)
    /*
    for (attribute <- attributes) {
      profile.addAttribute(attribute, user..get(attribute)
    }
    */
    profile.addRole("ROLE_ADMIN")
    profile
  }
}