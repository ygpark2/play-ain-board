package services

// externals
import java.util.UUID
import javax.inject.Inject

import models.account.MailTokenUsers
import org.joda.time.DateTime
import org.pac4j.core.config.Config

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

// internals
import models.account.MailTokenUser

trait MailToken {
  def id: UUID
  def email: String
  def expirationTime: DateTime
  def isExpired = expirationTime.isBeforeNow
}

trait MailTokenService[T <: MailToken] {
  def create(token: T): Future[Option[T]]
  def retrieve(id: UUID): Future[Option[T]]
  def consume(id: UUID): Unit
}

class MailTokenUserService @Inject() (val mailTokenUsers: MailTokenUsers) extends MailTokenService[MailTokenUser] {

  def create(token: MailTokenUser): Future[Option[MailTokenUser]] = {
    for {
      cnt <- mailTokenUsers.insert(token)
    } yield {
      if (cnt > 0) {
        Some(token)
      } else {
        None
      }
    }
  }

  def retrieve(id: UUID): Future[Option[MailTokenUser]] = mailTokenUsers.findById(id)

  def consume(id: UUID): Unit = mailTokenUsers.delete(id)

}