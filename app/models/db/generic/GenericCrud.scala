package models.db.generic

import java.util.UUID

import models.Page
import play.api.db.slick.HasDatabaseConfigProvider
import slick.driver.JdbcProfile
import slick.jdbc.meta.MTable.getTables

import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * @author Ondrej Kinovic (ondrej@kinovic.cz)
  * @since 8.11.16.
  */
abstract class GenericCrud[EntityType <: HasId] extends HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._

  type SpecificTable <: AbstractTable
  protected val query: TableQuery[SpecificTable]

  protected val TableName = ""

  protected val testData: List[EntityType] = List()

  protected def idFilter(id: UUID) = query.filter(_.id === id)

  abstract class AbstractTable(tag: Tag, tableName: String) extends Table[EntityType](tag, tableName) {
    val id: Rep[UUID] = column[UUID]("id", O.SqlType("UUID"), O.PrimaryKey)
  }

  def find(): Future[Seq[EntityType]] = db.run(query.result)

  def find(id: UUID): Future[Option[EntityType]] = db.run(idFilter(id).result.headOption)

  /*
  def list(page: Int = 0, pageSize: Int = 10, keyword: Option[String])(implicit s: Session): Future[Page[EntityType]] = {
    val offset = pageSize * page
    val query = keyword match {
      case Some(k) if !k.trim.isEmpty =>
        users.filter(_.email like s"%$k%")
      case _ =>
        users
    }

    def pagesQuery = db.run {
      query.sortBy(_.id.desc).drop(offset).take(pageSize).result
    }
    for {
      totalRows <- count(keyword)
      pages <- pagesQuery
    } yield Page(pages, page, offset, totalRows)
  }
  */

  def delete(id: UUID): Future[Int] = db.run(idFilter(id).delete)

  def insert(item: EntityType): Future[Int] = db.run(query += item)

  def insert(items: List[EntityType]): Future[Option[Int]] = db.run(query ++= items)

  def update(entity: EntityType): Future[Int] = db.run(idFilter(entity.id).update(entity))

  def loadData(): Future[Option[Int]] = db.run(query ++= testData)

  def ensureSchemaDropped =
    db.run(
      getTables(TableName).headOption.flatMap {
        case Some(table) => query.schema.drop.map(_ => ())
        case None => DBIO.successful(())
      }
    )

  def ensureSchemaCreated =
    db.run(
      getTables(TableName).headOption.flatMap {
        case Some(table) => DBIO.successful(())
        case None => query.schema.create.map(_ => ())
      }
    )
}