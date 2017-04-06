package models

import java.util.UUID
import javax.inject.Inject

import models.db.generic.{GenericCrud, HasId}
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile
import slick.jdbc.meta.MTable.getTables

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

case class BoardCategory(id: UUID,
                         board_id: Board,
                         name: String,
                         parentId: UUID) extends HasId {

  def patch(name: Option[String], parentId: Option[UUID]): BoardCategory =
    this.copy(name = name.getOrElse(this.name),
      parentId = parentId.getOrElse(this.parentId))

}

class BoardCategories @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends GenericCrud[BoardCategory] {

  /*
  val dbConfig = dbConfigProvider.get[JdbcProfile]
  // val dbConfig = dbConfigProvider.get[JdbcProfile]

  val db = dbConfig.db

  import dbConfig.driver.api._
  private val Tasks = TableQuery[TasksTable]
  */

  import driver.api._

  override type SpecificTable = BoardCategoriesTable
  override protected val query = TableQuery[SpecificTable]

  override protected val TableName = "BOARD_CATEGORIES"

  def findByParentId(parenttId: UUID): Future[List[BoardCategory]] =
    db.run(query.filter(_.parentId === parenttId).to[List].result)

  def partialUpdate(id: UUID, name: Option[String], parentId: Option[UUID]): Future[Int] = {
    val queryFilter = query.filter(_.id === id)

    val update = queryFilter.result.head.flatMap {task =>
      query.update(task.patch(name, parentId))
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

  def all(): Future[Seq[BoardCategory]] =
    db.run(query.result)

  def _deleteAllInParentt(parenttId: UUID): Future[Int] =
    db.run(query.filter(_.parentId === parenttId).delete)

  def findChildrenWithParents() = {
    val result = db.run((for {
      c <- query
      s <- c.subcategories
    } yield (c,s)).sortBy(_._1.name).result)

    result map {
      case categoryTuples => categoryTuples.groupBy(_._1).map{
        case (k,v) => (k,v.map(_._2))
      }
    }

  }

  protected class BoardCategoriesTable(tag: Tag) extends AbstractTable(tag, "BOARD_CATEGORIES") {

    def board = column[UUID]("BOARD_ID")
    def name = column[String]("NAME")
    def parentId = column[UUID]("PARENT")

    // def * = (id, color, status, project) <> (Board.tupled, Board.unapply)

    // def ? = (id.?, color.?, status, project.?).shaped.<>({ r => import r._; _1.map(_ => Task.tupled((_1.get, _2.get, _3.get, _4.get))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    def * = (id, name, parentId) <> (BoardCategory.tupled, BoardCategory.unapply)
    def boardFK = foreignKey("board_fk", board, query)(_.id)
    def categoryFK = foreignKey("category_fk", parentId, query)(_.id)
    def subcategories = TableQuery[BoardCategoriesTable].filter(_.id === parentId)
  }

  override protected val testData = List()

}
