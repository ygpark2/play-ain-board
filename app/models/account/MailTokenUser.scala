package models.account

import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

import models.db.generic.{GenericCrud, HasId}
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile

import scala.concurrent.Future
import slick.jdbc.meta.MTable
import slick.jdbc.meta.MTable.getTables
import com.github.t3hnar.bcrypt._
import models.Common
import org.joda.time.DateTime
import services.MailToken

case class MailTokenUser( id: UUID,
                          email: String,
                          expirationTime: DateTime,
                          isSignUp: Boolean
               ) extends MailToken with HasId {

}

class MailTokenUsers @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends GenericCrud[MailTokenUser] {

  import profile.api._

  override type SpecificTable = MailTokenUsersTable
  override protected val query = TableQuery[SpecificTable]

  override protected val TableName = "MAIL_TOKEN_USER"
  // db.run(query.schema.create)

  def findById(id: UUID): Future[Option[MailTokenUser]] =
    db.run(query.filter(_.id === id).result.headOption)

  def findByEmail(email: String): Future[Option[MailTokenUser]] =
    db.run(query.filter(_.email === email).result.headOption)

  def all: Future[List[MailTokenUser]] =
    db.run(query.to[List].result)

  /*
  def add(id: UUID, name: String, email: String, emailConfirmed: Boolean = false, password: String, salt: String, role: UserRole): Future[UUID] = {
    val user = User(id, Common(), name, email, emailConfirmed, password, salt, role)
    this.insert(user)
    // db.run(query returning query.map(_.id) += user)
    Future.successful(id)
  }
  */

  implicit val jodatimeColumnType = MappedColumnType.base[DateTime, Timestamp](
    { jodatime => new Timestamp(jodatime.getMillis()) },
    { sqltime => new DateTime(sqltime.getTime) }
  )

  protected class MailTokenUsersTable(tag: Tag) extends AbstractTable(tag, TableName) {

    val email = column[String]("EMAIL")
    val isSignUp = column[Boolean]("IS_SIGNUP")
    val expirationTime = column[DateTime]("EXPIRATION_TIME")

    def * = (id, email, expirationTime, isSignUp) <> (MailTokenUser.tupled, MailTokenUser.unapply)

    /*
    def ? = (common, name.?, email.?, emailConfirmed.?, password.?, role).shaped.<>({ r =>
      import r._
      _1.map( _ => User.tupled(( _1.get, _2.get, _3.get, _4.get, _5.get, _6.get )) )
    }, (_: Any) => throw new Exception("Inserting into ? projection not supported.") )
    */

  }

  override protected val testData = {
    List(
    )
  }
}
