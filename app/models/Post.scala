package models

import java.sql.Timestamp
import java.util.UUID
import javax.inject.Inject

import models.db.generic.{GenericCrud, HasId}
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile
import slick.jdbc.meta.MTable.getTables

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
                     attached_image_count: Int,
                   )

case class Post(id: UUID,
                board_id: Board,
                category: BoardCategory,
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

  def patch(color: Option[String], status: Option[PostStatus], board: Option[UUID]): Post =
    this.copy(color = color.getOrElse(this.color),
              status = status.getOrElse(this.status),
              board = board.getOrElse(this.board))

}

sealed trait PostStatus
case object PendingStatus extends PostStatus
case object EditingStatus extends PostStatus
case object PublishingStatus extends PostStatus


class Posts @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends GenericCrud[Post] {

  /*
  val dbConfig = dbConfigProvider.get[JdbcProfile]
  // val dbConfig = dbConfigProvider.get[JdbcProfile]

  val db = dbConfig.db

  import dbConfig.driver.api._
  private val Tasks = TableQuery[TasksTable]
  */

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

  def findByColor(color: String): Future[Option[Post]] =
    db.run(query.filter(_.color === color).result.headOption)

  def findByProjectId(boardId: UUID): Future[List[Post]] =
    db.run(query.filter(_.board === boardId).to[List].result)

  def findByReadyStatus: Future[List[Post]] =
    db.run(query.filter(_.status === (PublishingStatus: PostStatus)).to[List].result)

  def partialUpdate(id: UUID, color: Option[String], status: Option[PostStatus], board: Option[UUID]): Future[Int] = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val queryFilter = query.filter(_.id === id)

    val update = queryFilter.result.head.flatMap { post =>
      query.update(post.patch(color, status, board))
    }

    db.run(update)
  }

  /*
    /** Return a page of (Computer,Company) */
  def list(page: Int = 0, pageSize: Int = 10, orderBy: Int = 1, filter: String = "%"): Future[Page[(Computer, Company)]] = {

    val offset = pageSize * page
    val query =
      (for {
        (computer, company) <- computers joinLeft companies on (_.companyId === _.id)
        if computer.name.toLowerCase like filter.toLowerCase
      } yield (computer, company.map(_.id), company.map(_.name)))
        .drop(offset)
        .take(pageSize)

    for {
      totalRows <- count(filter)
      list = query.result.map { rows => rows.collect { case (computer, id, Some(name)) => (computer, Company(id, name)) } }
      result <- db.run(list)
    } yield Page(result, page, offset, totalRows)
  }
   */

  def all(): Future[Seq[Post]] =
    db.run(query.result)

  /*
  def insert(Task: Task): Future[Long] =
    db.run(query returning query.map(_.id) += Task)
  */

  def _deleteAllInProject(boardId: UUID): Future[Int] =
    db.run(query.filter(_.board === boardId).delete)


  protected class PostsTable(tag: Tag) extends AbstractTable(tag, "POST") {

    def color = column[String]("COLOR")
    def status = column[PostStatus]("STATUS") // (taskStatusColumnType)
    def board = column[UUID]("BOARD")

    def * = (id, color, status, board) <> (Post.tupled, Post.unapply)

    // def ? = (id.?, color.?, status, project.?).shaped.<>({ r => import r._; _1.map(_ => Task.tupled((_1.get, _2.get, _3.get, _4.get))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

  }

  override protected val testData = List()

}
