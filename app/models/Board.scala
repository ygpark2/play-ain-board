package models

import java.util.UUID
import javax.inject.Inject

import models.db.generic.{GenericCrud, HasId}
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile
import slick.jdbc.meta.MTable.getTables

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

case class Board(id: UUID, color: String, status: BoardStatus, project: Long) extends HasId {

  def patch(color: Option[String], status: Option[BoardStatus], project: Option[Long]): Board =
    this.copy(color = color.getOrElse(this.color),
              status = status.getOrElse(this.status),
              project = project.getOrElse(this.project))

}

sealed trait BoardStatus
case object ReadyStatus extends BoardStatus
case object SetStatus extends BoardStatus
case object GoStatus extends BoardStatus


class Boards @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends GenericCrud[Board] {

  /*
  val dbConfig = dbConfigProvider.get[JdbcProfile]
  // val dbConfig = dbConfigProvider.get[JdbcProfile]

  val db = dbConfig.db

  import dbConfig.driver.api._
  private val Tasks = TableQuery[TasksTable]
  */

  import driver.api._

  override type SpecificTable = BoardsTable
  override protected val query = TableQuery[SpecificTable]

  override protected val TableName = "BOARDS"

  implicit val taskStatusColumnType = MappedColumnType.base[BoardStatus, String](
    {
      case ReadyStatus => "ready"
      case SetStatus => "set"
      case GoStatus => "go"
    },
    {
      case "ready" => ReadyStatus
      case "set" => SetStatus
      case "go" => GoStatus
    }
  )

  def findByColor(color: String): Future[Option[Board]] =
    db.run(query.filter(_.color === color).result.headOption)

  def findByProjectId(projectId: Long): Future[List[Board]] =
    db.run(query.filter(_.project === projectId).to[List].result)

  def findByReadyStatus: Future[List[Board]] =
    db.run(query.filter(_.status === (ReadyStatus: BoardStatus)).to[List].result)

  def partialUpdate(id: UUID, color: Option[String], status: Option[BoardStatus], project: Option[Long]): Future[Int] = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val queryFilter = query.filter(_.id === id)

    val update = queryFilter.result.head.flatMap {task =>
      query.update(task.patch(color, status, project))
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

  def all(): Future[Seq[Board]] =
    db.run(query.result)

  /*
  def insert(Task: Task): Future[Long] =
    db.run(query returning query.map(_.id) += Task)
  */

  def _deleteAllInProject(projectId: Long): Future[Int] =
    db.run(query.filter(_.project === projectId).delete)

  protected class BoardsTable(tag: Tag) extends AbstractTable(tag, "BOARD") {

    def color = column[String]("COLOR")
    def status = column[BoardStatus]("STATUS") // (taskStatusColumnType)
    def project = column[Long]("PROJECT")

    def * = (id, color, status, project) <> (Board.tupled, Board.unapply)

    // def ? = (id.?, color.?, status, project.?).shaped.<>({ r => import r._; _1.map(_ => Task.tupled((_1.get, _2.get, _3.get, _4.get))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

  }

  override protected val testData = List()

}
