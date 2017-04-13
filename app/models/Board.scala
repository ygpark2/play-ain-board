package models

import java.util.UUID
import javax.inject.Inject

import models.db.generic.{GenericCrud, HasId}
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile
import slick.jdbc.meta.MTable.getTables

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

case class Board(id: UUID, key: String, name: String, mobile_name: String, order: Int, search_flag: Boolean) extends HasId {

  def patch(key: Option[String], name: Option[String], mobile_name: Option[String], order: Option[Int], search_flag: Option[Boolean]): Board =
    this.copy(
      key = key.getOrElse(this.key),
      name = name.getOrElse(this.name),
      mobile_name = mobile_name.getOrElse(this.mobile_name),
      order = order.getOrElse(this.order),
      search_flag = search_flag.getOrElse(this.search_flag)
    )

}

class Boards @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends GenericCrud[Board] {

  import driver.api._

  val posts: Posts = new Posts(dbConfigProvider)

  override type SpecificTable = BoardsTable
  override protected val query = TableQuery[SpecificTable]

  override protected val TableName = "BOARDS"

  /** Count with a filter. */
  def count(filter: String): Future[Int] =
    db.run(query.filter { board => board.name.toLowerCase like filter.toLowerCase }.length.result)

  /** Count with a filter. */
  def postCount(key: String, filter: String): Future[Int] = {
    val queryVal =
      (for {
        (boardObj, postObj) <- query.filter(_.key === key) joinLeft posts.getQuery() on (_.id === _.board)
      } yield (boardObj, postObj))
    val list = queryVal.result.map { rows => rows.length }
    db.run(list)
  }

  def findById(id: UUID): Future[Option[Board]] =
    db.run(query.filter(_.id === id).result.headOption)

  def findByIdJoinPost(id: String): Future[Seq[(Board, Post)]] = {
    // val postsTable = posts.getQuery().filter(_.id === UUID.fromString(id))
    val queryVal =
      (for {
        (boardObj, postObj) <- query joinLeft posts.getQuery().filter(_.id === UUID.fromString(id)) on (_.id === _.board)
      } yield (boardObj, postObj))
    val list = queryVal.result.map { rows => rows.collect { case (board, Some(post)) => (board, post) } }
    db.run(list)
  }

  def findByName(name: String): Future[List[Board]] =
    db.run(query.filter(_.name like name).to[List].result)

  def findByMobileName(mobileName: String): Future[List[Board]] =
    db.run(query.filter(_.mobile_name like mobileName).to[List].result)

  def findByKey(key: String): Future[Option[Board]] =
    db.run(query.filter(_.key === key).result.headOption)

  def findByKeyJoinPost(key: String): Future[Seq[(Board, Post)]] = {
    val queryVal =
      (for {
        (boardObj, postObj) <- query joinLeft posts.getQuery() on (_.id === _.board)
        if boardObj.key === key
      } yield (boardObj, postObj))
    val list = queryVal.result.map { rows => rows.collect { case (post, Some(board)) => (post, board) } }
    db.run(list)
  }

  def partialUpdate(id: UUID, key: Option[String], name: Option[String], mobile_name: Option[String], order: Option[Int], search_flag: Option[Boolean]): Future[Int] = {
    val queryFilter = query.filter(_.id === id)

    val update = queryFilter.result.head.flatMap {task =>
      query.update(task.patch(key, name, mobile_name, order, search_flag))
    }

    db.run(update)
  }

  /** Return a page of (Post,Board) */
  def postList(key: String, page: Int = 0, pageSize: Int = 10, orderBy: Int = 1, filter: String = "%"): Future[Page[(Board, Post)]] = {

    val offset = pageSize * page
    val initialQuery =
      (for {
        (boardObj, postObj) <- query joinLeft posts.getQuery() on (_.id === _.board)
        if boardObj.key === key
        // if computer.name.toLowerCase like filter.toLowerCase
      } yield (boardObj, postObj))
      // } yield (postObj, boardObj.map(_.id), boardObj.map(_.key), boardObj.map(_.name)))

    val sortedQuery = orderBy match {
      case 1 => initialQuery.sortBy(_._2.map(_.title).desc)
      // case 2 => initialQuery.sortBy(_._2.map(_.postUser.).desc)
      // case 3 => initialQuery.sortBy(_._2.map(_.postInfo.?).desc)
      case 4 => initialQuery.sortBy(_._2.map(_.created_datetime).desc)
    }

    for {
      totalRows <- postCount(key, filter)
      list = sortedQuery.drop(offset).take(pageSize).result.map { rows => rows.collect { case (board, Some(post)) => (board, post) } }
      result <- db.run(list)
    } yield Page(result, page, offset, totalRows)
  }

  def all(): Future[Seq[Board]] =
    db.run(query.result)

  /*
  def _deleteAllInProject(projectId: Long): Future[Int] =
    db.run(query.filter(_.project === projectId).delete)
  */

  protected class BoardsTable(tag: Tag) extends AbstractTable(tag, "BOARD") {

    def key = column[String]("KEY")
    def name = column[String]("NAME")
    def mobile_name = column[String]("MOBILE_NAME")
    def order = column[Int]("ORDER")
    def search_flag = column[Boolean]("SEARCH_FLAG")

    val key_index = index("key_idx", key, unique = true)

    def * = (id, key, name, mobile_name, order, search_flag) <> (Board.tupled, Board.unapply)

    // def ? = (id.?, color.?, status, project.?).shaped.<>({ r => import r._; _1.map(_ => Task.tupled((_1.get, _2.get, _3.get, _4.get))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

  }

  override protected val testData =
    List(
      Board(UUID.fromString("7f85f701-3cc5-4467-a802-4c436b97f0d1"), "notice", "공지사항", "공지", 0, true),
      Board(UUID.fromString("7f85f701-3cc5-4467-a802-4c436b97f0d2"), "free", "자유게시판", "자유", 0, true)
    )

}
