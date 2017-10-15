package models

import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

import models.db.generic.{GenericCrud, HasId}
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile
import slick.jdbc.meta.MTable.getTables
import com.github.t3hnar.bcrypt._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

case class CommentUser(
                      userid: String,
                      username: String,
                      nickname: String,
                      email: String,
                      homepage: String
                   )

case class CommentInfo(
                     like: Int,
                     dislike: Int,
                     ip_address: String,
                     blame_count: Int,
                     device: String
                   )

case class Comment(id: UUID,
                    board: UUID,
                    post: UUID,
                    content: String,
                    password: String,
                    enable_secret: Boolean,
                    enable_html: Boolean,
                    created_datetime: Timestamp,
                    updated_datetime: Timestamp,
                    updated_userid: String,
                    commentUser: CommentUser,
                    commentInfo: CommentInfo,
                    deleted: Boolean) extends HasId {

  def patch(board: Option[UUID], post: Option[UUID], content: Option[String], password: Option[String], enable_secret: Option[Boolean],
            enable_html: Option[Boolean], created_datetime: Option[Timestamp], updated_datetime: Option[Timestamp],
            updated_userid: Option[String], commentUser: Option[CommentUser], commentInfo: Option[CommentInfo], deleted: Option[Boolean]): Comment =
    this.copy(
      board = board.getOrElse(this.board),
      post = post.getOrElse(this.post),
      content = content.getOrElse(this.content),
      password = password.getOrElse(this.password),
      enable_secret = enable_secret.getOrElse(this.enable_secret),
      enable_html = enable_html.getOrElse(this.enable_html),
      created_datetime = created_datetime.getOrElse(this.created_datetime),
      updated_datetime = updated_datetime.getOrElse(this.updated_datetime),
      updated_userid = updated_userid.getOrElse(this.updated_userid),
      commentUser = commentUser.getOrElse(this.commentUser),
      commentInfo = commentInfo.getOrElse(this.commentInfo),
      deleted = deleted.getOrElse(this.deleted)
    )

  // def withEmailConfirmed(v : Boolean) : User = this.copy(emailConfirmed = v)

}

class Comments @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends GenericCrud[Comment] {

  import driver.api._

  override type SpecificTable = CommentsTable
  override protected val query = TableQuery[SpecificTable]

  override protected val TableName = "COMMENTS"

  /** Count with a filter. */
  def count(filter: String): Future[Int] =
    db.run(query.filter { post => post.content.toLowerCase like filter.toLowerCase }.length.result)

  def findByPost(post_id: String): Future[List[Comment]] =
    db.run(query.filter(_.post === UUID.fromString(post_id)).to[List].result)

  def all(): Future[Seq[Comment]] = db.run(query.result)

  def _deleteAllInBoard(board: UUID): Future[Int] = db.run(query.filter(_.board === board).delete)

  protected class CommentsTable(tag: Tag) extends AbstractTable(tag, TableName) {

    val boards: Boards = new Boards(dbConfigProvider)
    val posts: Posts = new Posts(dbConfigProvider)

    def board = column[UUID]("BAORD_ID")
    def post = column[UUID]("POST_ID")
    def content = column[String]("CONTENT")
    def password = column[String]("PASSWORD")
    def created_datetime = column[Timestamp]("CREATED_DATETIME")
    def updated_datetime = column[Timestamp]("UPDATED_DATETIME")
    def updated_userid = column[String]("UPDATED_USERID")

    def userid = column[String]("USER_ID")
    def username = column[String]("USERNAME")
    def nickname = column[String]("NICKNAME")
    def email = column[String]("EMAIL")
    def homepage = column[String]("HOMEPAGE")

    def enable_secret = column[Boolean]("ENABLE_SECRET")
    def enable_html = column[Boolean]("ENABLE_HTML")

    def like = column[Int]("LIKE")
    def dislike = column[Int]("DISLIKE")
    def ip_address = column[String]("IP_ADDRESS")
    def blame_count = column[Int]("BLAME_COUNT")
    def device = column[String]("DEVICE")

    def deleted = column[Boolean]("DELETED")

    val commentUser = (userid, username, nickname, email, homepage) <> (CommentUser.tupled, CommentUser.unapply)

    val commentInfo = (like, dislike, ip_address, blame_count, device) <> (CommentInfo.tupled, CommentInfo.unapply)

    def boardFK = foreignKey("board_fk", board, boards.getQuery())(_.id)
    def postFK = foreignKey("post_fk", post, posts.getQuery())(_.id)

    val created_datetime_index = index("created_datetime_key", created_datetime, unique = true)

    def * = (id, board, post, content, password, enable_secret, enable_html, created_datetime, updated_datetime, updated_userid,
      commentUser, commentInfo, deleted) <> (Comment.tupled, Comment.unapply)

    // def ? = (id.?, color.?, status, project.?).shaped.<>({ r => import r._; _1.map(_ => Task.tupled((_1.get, _2.get, _3.get, _4.get))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

  }

  override protected val testData = {
    List()
  }

}
