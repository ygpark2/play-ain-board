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

case class UserMessage(id: UUID,
                   sender_id: UUID,
                   receiver_id: UUID,
                   msg_type: String,
                   title:String,
                   content: String,
                   reply_id: UUID,
                   datetime: Timestamp,
                   read_datetime: Timestamp) extends HasId {

  def patch(sender_id: Option[UUID],
            receiver_id: Option[UUID],
            msg_type: Option[String],
            title: Option[String],
            content: Option[String],
            reply_id: Option[UUID],
            datetime: Option[Timestamp],
            read_datetime: Option[Timestamp]): UserMessage =
    this.copy(
      sender_id = sender_id.getOrElse(this.sender_id),
      receiver_id = receiver_id.getOrElse(this.receiver_id),
      msg_type = msg_type.getOrElse(this.msg_type),
      title = title.getOrElse(this.title),
      content = content.getOrElse(this.content),
      reply_id = reply_id.getOrElse(this.reply_id),
      datetime = datetime.getOrElse(this.datetime),
      read_datetime = read_datetime.getOrElse(this.read_datetime)
    )

}

class UserMessages @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends GenericCrud[UserMessage] {

  import driver.api._

  override type SpecificTable = UserMessagesTable
  override protected val query = TableQuery[SpecificTable]

  override protected val TableName = "USER_MESSAGE"

  def findBySender(sender: UUID): Future[List[UserMessage]] =
    db.run(query.filter(_.sender_id === sender).to[List].result)

  def findByReceiver(receiver: UUID): Future[List[UserMessage]] =
    db.run(query.filter(_.receiver_id === receiver).to[List].result)

  def partialUpdate(id: UUID,
                    sender_id: Option[UUID],
                    receiver_id: Option[UUID],
                    msg_type: Option[String],
                    title: Option[String],
                    content: Option[String],
                    reply_id: Option[UUID],
                    datetime: Option[Timestamp],
                    read_datetime: Option[Timestamp]): Future[Int] = {
    val queryFilter = query.filter(_.id === id)
    val update = queryFilter.result.head.flatMap {task =>
      query.update(task.patch(sender_id, receiver_id, msg_type, title, content, reply_id, datetime, read_datetime))
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

  def all(): Future[Seq[UserMessage]] =
    db.run(query.result)

  protected class UserMessagesTable(tag: Tag) extends AbstractTable(tag, TableName) {

    def sender_id = column[UUID]("SENDER_ID")
    def receiver_id = column[UUID]("RECEIVER_ID")
    def msg_type = column[String]("MSG_TYPE")
    def title = column[String]("TITLE")
    def content = column[String]("CONTENT")
    def reply_id = column[UUID]("REPLY_ID")
    def datetime = column[Timestamp]("DATETIME")
    def read_datetime = column[Timestamp]("READ_DATETIME")

    def * = (id, sender_id, receiver_id, msg_type, title, content, reply_id, datetime, read_datetime) <> (UserMessage.tupled, UserMessage.unapply)

    // def ? = (id.?, color.?, status, project.?).shaped.<>({ r => import r._; _1.map(_ => Task.tupled((_1.get, _2.get, _3.get, _4.get))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

  }

  override protected val testData = List()

}
