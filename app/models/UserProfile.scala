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

case class UserInfo(
                   phone: String,
                   mobile: String,
                   sex: String,
                   zipcode: String,
                   address1: String,
                   address2: String,
                   address3: String,
                   address4: String,
                   introduction: String
                  )

case class UserConf(
                     enable_receive_email: Short = 1,
                     enable_use_message: Short = 1,
                     enable_receive_sms: Short = 1,
                     enable_open_profile: Short = 1,
                     register_datetime: Timestamp,
                     register_ip: String,
                     lastlogin_datetime: Timestamp,
                     lastlogin_ip: String
                   )

case class UserProfile( id: UUID,
                 common: Common,
                 name: String,
                 email: String,
                 emailConfirmed: Boolean,
                 password: String,
                 salt: String,
                 level: Int,
                 point: Int,
                 userInfo: UserInfo,
                 userConf: UserConf
               ) extends HasId {

  def withEmailConfirmed(v : Boolean) : UserProfile = this.copy(emailConfirmed = v)

}

class UserProfiles @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends GenericCrud[UserProfile] {

  /*
  val dbConfig = dbConfigProvider.get[JdbcProfile]
  val db = dbConfig.db

  import dbConfig.driver.api._

  private val Users = TableQuery[UsersTable]

  */

  import profile.api._

  override type SpecificTable = UserProfilesTable
  override protected val query = TableQuery[SpecificTable]

  override protected val TableName = "USER_PROFILES"
  // db.run(query.schema.create)

  def findById(id: UUID): Future[Option[UserProfile]] =
    db.run(query.filter(_.id === id).result.headOption)

  def findByEmail(email: String): Future[Option[UserProfile]] =
    db.run(query.filter(_.email === email).result.headOption)

  def findByName(name: String): DBIO[Option[UserProfile]] =
    query.filter(_.name === name).result.headOption

  def all: Future[List[UserProfile]] =
    db.run(query.to[List].result)

  /*
  def add(id: UUID, name: String, email: String, emailConfirmed: Boolean = false, password: String, salt: String, role: UserRole): Future[UUID] = {
    val user = User(id, Common(), name, email, emailConfirmed, password, salt, role)
    this.insert(user)
    // db.run(query returning query.map(_.id) += user)
    Future.successful(id)
  }
  */

  protected class UserProfilesTable(tag: Tag) extends AbstractTable(tag, TableName) {

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
    val phone = column[String]("PHONE")
    val mobile = column[String]("MOBILE")
    val sex = column[String]("SEX")
    val zipcode = column[String]("ZIPCODE")
    val address1 = column[String]("ADDRESS1")
    val address2 = column[String]("ADDRESS2")
    val address3 = column[String]("ADDRESS3")
    val address4 = column[String]("ADDRESS4")
    val enable_receive_email = column[Short]("ENABLE_RECEIVE_EMAIL")
    val enable_use_message = column[Short]("ENABLE_USE_MESSAGE")
    val enable_receive_sms = column[Short]("ENABLE_RECEIVE_SMS")
    val enable_open_profile = column[Short]("ENABLE_OPEN_PROFILE")
    val register_datetime = column[Timestamp]("REGISTER_DATETIME")
    val register_ip = column[String]("REGISTER_IP")
    val lastlogin_datetime = column[Timestamp]("LASTLOGIN_DATETIME")
    val lastlogin_ip = column[String]("LASTLOGIN_IP")
    val introduction = column[String]("INTRODUCTION")

    val email_index = index("users_email_key", email, unique = true)

    val common = (createdAt, updatedAt) <> (Common.tupled, Common.unapply)

    val userInfo = (phone, mobile, sex, zipcode, address1, address2, address3, address4, introduction) <> (UserInfo.tupled, UserInfo.unapply)

    val userConf = (enable_receive_email, enable_use_message, enable_receive_sms, enable_open_profile, register_datetime, register_ip,
      lastlogin_datetime, lastlogin_ip) <> (UserConf.tupled, UserConf.unapply)

    def * = (id, common, name, email, emailConfirmed, password, salt, level, point, userInfo, userConf) <> (UserProfile.tupled, UserProfile.unapply)


    /*
    def ? = (common, name.?, email.?, emailConfirmed.?, password.?, role).shaped.<>({ r =>
      import r._
      _1.map( _ => User.tupled(( _1.get, _2.get, _3.get, _4.get, _5.get, _6.get )) )
    }, (_: Any) => throw new Exception("Inserting into ? projection not supported.") )
    */

  }

  override protected val testData = {
    val salt = generateSalt
    val userInfo = UserInfo("064-283-3894", "010-3882-3842", "남", "23838", "대한민국", "전남 나주 황동 7길 382-4", "", "", "안녕하세요 만난서 반가와요.")
    val userConf = UserConf(1, 1, 1, 1, java.sql.Timestamp.valueOf(LocalDateTime.now()), "255.141.38.200", java.sql.Timestamp.valueOf(LocalDateTime.now()), "232.133.48.102")
    List(
      UserProfile(UUID.randomUUID(), Common(), "test_name1", "email1@abc.com", false, "password".bcrypt(salt), salt, 2, 283, userInfo, userConf),
      UserProfile(UUID.randomUUID(), Common(), "test_name2", "email2@abc.com", false, "password".bcrypt(salt), salt, 4, 383, userInfo, userConf)
    )
  }
}
