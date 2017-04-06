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

case class Message(id: UUID,
                   sender_id: UUID,
                   receiver_id: UUID,
                   msg_type: String,
                   title:String,
                   content: String,
                   reply_id: UUID,
                   datetime: Timestamp,
                   read_datetime: Timestamp) extends HasId {

  def patch(name: Option[String], mobile_name: Option[String], order: Option[Int], search_flag: Option[Boolean]): Message =
    this.copy(name = name.getOrElse(this.name),
      mobile_name = mobile_name.getOrElse(this.mobile_name),
      order = order.getOrElse(this.order),
      search_flag = search_flag.getOrElse(this.search_flag))

}

class Messages @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends GenericCrud[Message] {

  import driver.api._

  override type SpecificTable = MessagesTable
  override protected val query = TableQuery[SpecificTable]

  override protected val TableName = "BOARDS"

  def findByName(name: String): Future[List[Message]] =
    db.run(query.filter(_.name like name).to[List].result)

  def findByMobileName(mobileName: String): Future[List[Message]] =
    db.run(query.filter(_.mobile_name like mobileName).to[List].result)

  def partialUpdate(id: UUID, name: Option[String], mobile_name: Option[String], order: Option[Int], search_flag: Option[Boolean]): Future[Int] = {
    val queryFilter = query.filter(_.id === id)

    val update = queryFilter.result.head.flatMap {task =>
      query.update(task.patch(name, mobile_name, order, search_flag))
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

  def all(): Future[Seq[Message]] =
    db.run(query.result)

  /*
  def _deleteAllInProject(projectId: Long): Future[Int] =
    db.run(query.filter(_.project === projectId).delete)
  */

  protected class MessagesTable(tag: Tag) extends AbstractTable(tag, "MESSAGES") {

    def name = column[String]("NAME")
    def mobile_name = column[String]("MOBILE_NAME")
    def order = column[Int]("ORDER")
    def search_flag = column[Boolean]("SEARCH_FLAG")

    def * = (id, name, mobile_name, order, search_flag) <> (Message.tupled, Message.unapply)

    // def ? = (id.?, color.?, status, project.?).shaped.<>({ r => import r._; _1.map(_ => Task.tupled((_1.get, _2.get, _3.get, _4.get))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

  }

  override protected val testData = List()

}
