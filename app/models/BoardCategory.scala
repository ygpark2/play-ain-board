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
                         board: UUID,
                         name: String,
                         parentId: Option[UUID]) extends HasId {

  def patch(board: Option[UUID], name: Option[String], parentId: Option[UUID]): BoardCategory =
    this.copy(
      board = board.getOrElse(this.board),
      name = name.getOrElse(this.name),
      parentId = parentId // .getOrElse(this.parentId)
    )

}

class BoardCategories @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends GenericCrud[BoardCategory] {

  import driver.api._

  override type SpecificTable = BoardCategoriesTable
  override protected val query = TableQuery[SpecificTable]

  override protected val TableName = "BOARD_CATEGORIES"

  def findByParentId(parenttId: UUID): Future[List[BoardCategory]] =
    db.run(query.filter(_.parentId === parenttId).to[List].result)

  def partialUpdate(id: UUID, board: Option[UUID], name: Option[String], parentId: Option[UUID]): Future[Int] = {
    val queryFilter = query.filter(_.id === id)

    val update = queryFilter.result.head.flatMap {task =>
      query.update(task.patch(board, name, parentId))
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

  def _deleteAllInParent(parentId: UUID): Future[Int] =
    db.run(query.filter(_.parentId === parentId).delete)

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

    val boards: Boards = new Boards(dbConfigProvider)

    def boardId = column[UUID]("BOARD_ID")
    def name = column[String]("NAME")
    def parentId = column[Option[UUID]]("PARENT", O.Default(None))

    def boardFK = foreignKey("board_fk", boardId, boards.getQuery())(_.id)
    def categoryFK = foreignKey("category_fk", parentId, query)(_.id.?)
    def subcategories = TableQuery[BoardCategoriesTable].filter(_.id === parentId)

    // def ? = (id.?, color.?, status, project.?).shaped.<>({ r => import r._; _1.map(_ => Task.tupled((_1.get, _2.get, _3.get, _4.get))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    def * = (id, boardId, name, parentId) <> (BoardCategory.tupled, BoardCategory.unapply)

  }

  override protected val testData = List(
    BoardCategory(UUID.fromString("3f909c47-a46f-45ac-9ecc-de553317cd7a"), UUID.fromString("7f85f701-3cc5-4467-a802-4c436b97f0d1"), "test_category1_1", None),
    BoardCategory(UUID.fromString("3f909c47-a46f-45ac-9ecc-de553317cd7b"), UUID.fromString("7f85f701-3cc5-4467-a802-4c436b97f0d1"), "test_category1_2", Some(UUID.fromString("3f909c47-a46f-45ac-9ecc-de553317cd7a"))),
    BoardCategory(UUID.fromString("3f909c47-a46f-45ac-9ecc-de553317cd7c"), UUID.fromString("7f85f701-3cc5-4467-a802-4c436b97f0d1"), "test_category1_3", Some(UUID.fromString("3f909c47-a46f-45ac-9ecc-de553317cd7b"))),
    BoardCategory(UUID.fromString("3f909c47-a46f-45ac-9ecc-de553317cd7d"), UUID.fromString("7f85f701-3cc5-4467-a802-4c436b97f0d2"), "test_category2_1", None),
    BoardCategory(UUID.fromString("3f909c47-a46f-45ac-9ecc-de553317cd7e"), UUID.fromString("7f85f701-3cc5-4467-a802-4c436b97f0d2"), "test_category2_2", Some(UUID.fromString("3f909c47-a46f-45ac-9ecc-de553317cd7d"))),
    BoardCategory(UUID.fromString("3f909c47-a46f-45ac-9ecc-de553317cd7f"), UUID.fromString("7f85f701-3cc5-4467-a802-4c436b97f0d2"), "test_category2_3", Some(UUID.fromString("3f909c47-a46f-45ac-9ecc-de553317cd7e")))
  )

}
