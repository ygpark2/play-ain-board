package models.db.generic

import java.util.UUID

/**
  * @author Ondrej Kinovic (ondrej@kinovic.cz)
  * @since 8.11.16.
  */
trait HasId {
  val id: UUID

}