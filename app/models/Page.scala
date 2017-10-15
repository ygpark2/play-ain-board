package models

import java.util.Date

case class Page[A](items: Seq[A], page: Int, offset: Long, total: Long) {
  lazy val prev = Option(page - 1).filter(_ >= 0)
  lazy val next = Option(page + 1).filter(_ => (offset + items.size) < total)
  private val pageSize = if (page > 0) (offset / page).toInt else items.size

  val totalPage = if (total > 0 && pageSize > 0) {
    if ((total % pageSize) > 0) {
      (total / pageSize + 1).toInt
    } else {
      (total / pageSize).toInt
    }
  } else {
    1
  }
}
