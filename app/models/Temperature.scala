package models

import java.sql.Timestamp
import java.time.LocalDateTime
import javax.inject.Inject

import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile

import scala.concurrent.Future

case class Temperature(id: Long, timestamp: Timestamp, temperature: Float)

class TemperatureRepo @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) {

  val dbConfig = dbConfigProvider.get[JdbcProfile]
  val db = dbConfig.db

  import dbConfig.driver.api._

  private val Temperatures = TableQuery[TemperatureTable]

  db.run(Temperatures.schema.create)

  /*
  lazy val allTables = Array(
  TableQuery[AcceptanceTable].schema,
  [... many more ...]
  TableQuery[UserTable].schema
).reduceLeft(_ ++ _)

/** Create all tables in database */
def create = {
  allTables.create
}

/** Delete all tables in database */
def drop = {
  allTables.drop
}

   */
  def all: Future[List[Temperature]] =
    db.run(Temperatures.to[List].result)

  def add(temp: Float): Future[Long] = {
    val temperature = Temperature(0, java.sql.Timestamp.valueOf(LocalDateTime.now()), temp)
    db.run(Temperatures returning Temperatures.map(_.id) += temperature)
  }

  private class TemperatureTable(tag: Tag) extends Table[Temperature](tag, "TEMPERATURES") {

    def id = column[Long]("ID", O.AutoInc, O.PrimaryKey)

    def timestamp = column[Timestamp]("TIMESTAMP")

    def temperature = column[Float]("temperature")

    def * = (id, timestamp, temperature) <> (Temperature.tupled, Temperature.unapply)

    def ? = (id.?, timestamp.?, temperature.?).shaped.<>({ r => import r._; _1.map(_ => Temperature.tupled((_1.get, _2.get, _3.get))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))
  }

}