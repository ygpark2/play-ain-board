package models

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

sealed trait UserRole
case object NormalRole extends UserRole
case object ManagerRole extends UserRole
case object AdminRole extends UserRole

case class User( id: UUID,
                 common: Common,
                 name: String,
                 email: String,
                 emailConfirmed: Boolean,
                 password: String,
                 salt: String,
                 level: Int,
                 point: Int,
                 role: UserRole
               ) extends HasId {

  def withEmailConfirmed(v : Boolean) : User = this.copy(emailConfirmed = v)

  def withEmailConfirmedPassword(v : Boolean, p: String) : User = this.copy(emailConfirmed = v, password = p)

  def isAdmin: Boolean = role == AdminRole
}

class Users @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends GenericCrud[User] {

  import profile.api._

  override type SpecificTable = UsersTable
  override protected val query = TableQuery[SpecificTable]

  override protected val TableName = "USERS"
  // db.run(query.schema.create)

  def findById(id: UUID): Future[Option[User]] =
    db.run(query.filter(_.id === id).result.headOption)

  def findByEmail(email: String): Future[Option[User]] =
    db.run(query.filter(_.email === email).result.headOption)

  def findByName(name: String): DBIO[Option[User]] =
    query.filter(_.name === name).result.headOption

  def all: Future[List[User]] =
    db.run(query.to[List].result)

  /*
  def add(id: UUID, name: String, email: String, emailConfirmed: Boolean = false, password: String, salt: String, role: UserRole): Future[UUID] = {
    val user = User(id, Common(), name, email, emailConfirmed, password, salt, role)
    this.insert(user)
    // db.run(query returning query.map(_.id) += user)
    Future.successful(id)
  }
  */

  implicit lazy val userRoleColumnType = MappedColumnType.base[UserRole, String](
    {
      case NormalRole => "normal"
      case ManagerRole => "manager"
      case AdminRole => "admin"
    },
    {
      case "normal" => NormalRole
      case "manager" => ManagerRole
      case "admin" => AdminRole
    }
  )

  protected class UsersTable(tag: Tag) extends AbstractTable(tag, TableName) {

    // val id = column[UUID]("ID", O.PrimaryKey)

    val createdAt = column[Timestamp]("createdAt")
    val updatedAt = column[Timestamp]("updatedAt")

    val name = column[String]("NAME")
    val email = column[String]("EMAIL")
    val emailConfirmed = column[Boolean]("EMAIL_CONFIRMED")
    val password = column[String]("PASSWORD")
    val salt = column[String]("SALT")
    val level = column[Int]("LEVEL")
    val point = column[Int]("POINT")
    val role = column[UserRole]("ROLE")

    val email_index = index("users_email_key", email, unique = true)

    val common = (createdAt, updatedAt) <> (Common.tupled, Common.unapply)

    def * = (id, common, name, email, emailConfirmed, password, salt, level, point, role) <> (User.tupled, User.unapply)


    /*
    def ? = (common, name.?, email.?, emailConfirmed.?, password.?, role).shaped.<>({ r =>
      import r._
      _1.map( _ => User.tupled(( _1.get, _2.get, _3.get, _4.get, _5.get, _6.get )) )
    }, (_: Any) => throw new Exception("Inserting into ? projection not supported.") )
    */

  }

  override protected val testData = {
    val salt = generateSalt
    List(
      User(UUID.randomUUID(), Common(), "test_name1", "email1@abc.com", false, "password".bcrypt(salt), salt, 2, 283, AdminRole),
      User(UUID.randomUUID(), Common(), "test_name2", "email2@abc.com", false, "password".bcrypt(salt), salt, 4, 383, NormalRole)
    )
  }
}
