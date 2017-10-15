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

case class PostUser(
                      userid: String,
                      username: String,
                      nickname: String,
                      email: String,
                      homepage: String
                   )

case class PostConf(
                     enable_secret: Boolean,
                     enable_html: Boolean,
                     enable_hide_comment: Boolean,
                     enable_notice: Boolean,
                     enable_receive_email: Boolean
                   )

case class PostInfo(
                     link_count: Int,
                     hit: Int,
                     like: Int,
                     dislike: Int,
                     ip_address: String,
                     blame_count: Int,
                     device: String,
                     attached_file_count: Int,
                     attached_image_count: Int
                   )

case class Post(id: UUID,
                board: UUID,
                category: Option[UUID],
                title: String,
                content: String,
                password: String,
                created_datetime: Timestamp,
                updated_datetime: Timestamp,
                updated_userid: String,
                comment_count: Int,
                comment_updated_datetime: Timestamp,
                postUser: PostUser,
                postConf: PostConf,
                postInfo: PostInfo,
                deleted: Boolean) extends HasId {

  def patch(board: Option[UUID], category: Option[UUID], title: Option[String], content: Option[String], password: Option[String],
            created_datetime: Option[Timestamp], updated_datetime: Option[Timestamp], updated_userid: Option[String], comment_count: Option[Int],
            comment_updated_datetime: Option[Timestamp], postUser: Option[PostUser], postConf: Option[PostConf], postInfo: Option[PostInfo],
            deleted: Option[Boolean]): Post =
    this.copy(
      board = board.getOrElse(this.board),
      category = category, // .getOrElse(this.category),
      title = title.getOrElse(this.title),
      content = content.getOrElse(this.content),
      password = password.getOrElse(this.password),
      created_datetime = created_datetime.getOrElse(this.created_datetime),
      updated_datetime = updated_datetime.getOrElse(this.updated_datetime),
      updated_userid = updated_userid.getOrElse(this.updated_userid),
      comment_count = comment_count.getOrElse(this.comment_count),
      comment_updated_datetime = comment_updated_datetime.getOrElse(this.comment_updated_datetime),
      postUser = postUser.getOrElse(this.postUser),
      postConf = postConf.getOrElse(this.postConf),
      postInfo = postInfo.getOrElse(this.postInfo),
      deleted = deleted.getOrElse(this.deleted)
    )

  def incHit(v : Int) : Post = this.copy(postInfo = postInfo.copy(hit = v))

  def incCommentCnt(v : Int) : Post = this.copy(comment_count = v, comment_updated_datetime = java.sql.Timestamp.valueOf(LocalDateTime.now()))
  // def withEmailConfirmed(v : Boolean) : User = this.copy(emailConfirmed = v)

}

sealed trait PostStatus
case object PendingStatus extends PostStatus
case object EditingStatus extends PostStatus
case object PublishingStatus extends PostStatus


class Posts @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends GenericCrud[Post] {

  import driver.api._

  override type SpecificTable = PostsTable
  override protected val query = TableQuery[SpecificTable]

  override protected val TableName = "POSTS"

  implicit val postStatusColumnType = MappedColumnType.base[PostStatus, String](
    {
      case PendingStatus => "pending"
      case EditingStatus => "editing"
      case PublishingStatus => "publishing"
    },
    {
      case "pending" => PendingStatus
      case "editing" => EditingStatus
      case "publishing" => PublishingStatus
    }
  )

  /** Count with a filter. */
  def count(filter: String): Future[Int] =
    db.run(query.filter { post => post.title.toLowerCase like filter.toLowerCase }.length.result)

  def findByCategory(category: UUID): Future[List[Post]] =
    db.run(query.filter(_.category === category).to[List].result)



  /*
  def findByBoard(board: UUID): Future[Seq[(Post, Board)]] = {
    val queryVal =
      (for {
        (postObj, boardObj) <- query joinLeft boards.getQuery() on (_.board === _.id)
        if postObj.board === board
      } yield (postObj, boardObj))
    val list = queryVal.result.map { rows => rows.collect { case (post, Some(board)) => (post, board) } }
    db.run(list) //  .filter(_.board === board).to[List].result)
  }


  def partialUpdate(id: UUID, color: Option[String], status: Option[PostStatus], board: Option[UUID]): Future[Int] = {
    val queryFilter = query.filter(_.id === id)

    val update = queryFilter.result.head.flatMap { post =>
      query.update(post.patch(color, status, board))
    }

    db.run(update)
  }

  /** Return a page of (Post,Board) */
  def list(board: UUID, page: Int = 0, pageSize: Int = 10, orderBy: Int = 1, filter: String = "%"): Future[Page[(Post, UUID, String, String)]] = {

    val offset = pageSize * page
    val queryVal =
      (for {
        (postObj, boardObj) <- query joinLeft boards.getQuery() on (_.board === _.id)
        if postObj.board === board
        // if computer.name.toLowerCase like filter.toLowerCase
      } yield (postObj, boardObj.map(_.id), boardObj.map(_.key), boardObj.map(_.name)))
        .drop(offset)
        .take(pageSize)

    for {
      totalRows <- count(filter)
      list = queryVal.result.map { rows => rows.collect { case (post, Some(id), Some(key), Some(name)) => (post, id, key, name) } }
      result <- db.run(list)
    } yield Page(result, page, offset, totalRows)
  }
   */

  def all(): Future[Seq[Post]] =
    db.run(query.result)

  def _deleteAllInBoard(board: UUID): Future[Int] =
    db.run(query.filter(_.board === board).delete)

  protected class PostsTable(tag: Tag) extends AbstractTable(tag, TableName) {

    val boardCategories: BoardCategories = new BoardCategories(dbConfigProvider)
    val boards: Boards = new Boards(dbConfigProvider)

    def board = column[UUID]("BAORD_ID")
    def category = column[Option[UUID]]("CATEGORY_ID")
    def title = column[String]("TITLE")
    def content = column[String]("CONTENT")
    def password = column[String]("PASSWORD")
    def created_datetime = column[Timestamp]("CREATED_DATETIME")
    def updated_datetime = column[Timestamp]("UPDATED_DATETIME")
    def updated_userid = column[String]("UPDATED_USERID")
    def comment_count = column[Int]("COMMENT_COUNT")
    def comment_updated_datetime = column[Timestamp]("COMMENT_UPDATED_DATETIME")

    def userid = column[String]("USER_ID")
    def username = column[String]("USERNAME")
    def nickname = column[String]("NICKNAME")
    def email = column[String]("EMAIL")
    def homepage = column[String]("HOMEPAGE")

    def enable_secret = column[Boolean]("ENABLE_SECRET")
    def enable_html = column[Boolean]("ENABLE_HTML")
    def enable_hide_comment = column[Boolean]("ENABLE_HIDE_COMMENT")
    def enable_notice = column[Boolean]("ENABLE_NOTICE")
    def enable_receive_email = column[Boolean]("ENABLE_RECEIVE_EMAIL")

    def link_count = column[Int]("LINK_COUNT")
    def hit = column[Int]("HIT")
    def like = column[Int]("LIKE")
    def dislike = column[Int]("DISLIKE")
    def ip_address = column[String]("IP_ADDRESS")
    def blame_count = column[Int]("BLAME_COUNT")
    def device = column[String]("DEVICE")
    def attached_file_count = column[Int]("ATTACHED_FILE_COUNT")
    def attached_image_count = column[Int]("ATTACHED_IMAGE_COUNT")

    def deleted = column[Boolean]("DELETED")

    val postUser = (userid, username, nickname, email, homepage) <> (PostUser.tupled, PostUser.unapply)

    val postConf = (enable_secret, enable_html, enable_hide_comment, enable_notice, enable_receive_email) <> (PostConf.tupled, PostConf.unapply)

    val postInfo = (link_count, hit, like, dislike, ip_address, blame_count, device, attached_file_count, attached_image_count) <> (PostInfo.tupled, PostInfo.unapply)

    def boardFK = foreignKey("board_fk", board, boards.getQuery())(_.id)
    def categoryFK = foreignKey("category_fk", category, boardCategories.getQuery())(_.id.?)

    val created_datetime_index = index("created_datetime_key", created_datetime, unique = true)

    def * = (id, board, category, title, content, password, created_datetime, updated_datetime, updated_userid, comment_count,
    comment_updated_datetime, postUser, postConf, postInfo, deleted) <> (Post.tupled, Post.unapply)

    // def ? = (id.?, color.?, status, project.?).shaped.<>({ r => import r._; _1.map(_ => Task.tupled((_1.get, _2.get, _3.get, _4.get))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

  }

  override protected val testData = {
    // java.sql.Timestamp.valueOf(LocalDateTime.now())
    val salt = generateSalt
    val postUser = PostUser("iowjie", "박기태", "남자에요", "abcd@test.com", "http://www.homepage.co.kr")
    val postConf = PostConf(true, false, false, false, false)
    val postInfo = PostInfo(0, 0, 0, 0, "123.123.12.34", 0, "ie", 0, 0)
    List(
      Post(UUID.randomUUID(), UUID.fromString("7f85f701-3cc5-4467-a802-4c436b97f0d1"), null,
        "test_title1", "test_content1", "password".bcrypt(salt),
        java.sql.Timestamp.valueOf(LocalDateTime.now()), java.sql.Timestamp.valueOf(LocalDateTime.now()), "test1", 0,
        java.sql.Timestamp.valueOf(LocalDateTime.now()), postUser, postConf, postInfo, false),
      Post(UUID.randomUUID(), UUID.fromString("7f85f701-3cc5-4467-a802-4c436b97f0d2"), null,
        "test_title2", "test_content2", "password".bcrypt(salt),
        java.sql.Timestamp.valueOf(LocalDateTime.now()), java.sql.Timestamp.valueOf(LocalDateTime.now()), "test2", 0,
        java.sql.Timestamp.valueOf(LocalDateTime.now()), postUser, postConf, postInfo, false)
    )
  }

}
