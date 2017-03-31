package models

import java.sql.Timestamp
import java.time.{LocalDateTime, OffsetDateTime}
import java.util.UUID

import scala.collection.immutable.ListMap

case class Common (
  createdAt: Timestamp = java.sql.Timestamp.valueOf(LocalDateTime.now()),
  updatedAt: Timestamp = java.sql.Timestamp.valueOf(LocalDateTime.now()) // new Timestamp(System.currentTimeMillis)
)