
package config.data

import javax.inject.Inject

import models._

import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

trait Database {
  def create(): Unit
  def drop(): Unit
}

class TestDatabase @Inject() (
                               users: Users,
                               comments: Comments,
                               posts: Posts,
                               boards: Boards,
                               boardCategories: BoardCategories
                             ) extends Database {

  initialize() // running initialization in constructor

  def initialize() = {
    println("Setup database with test data here")
  }

  def create() = Seq(boards, boardCategories, posts, comments, users).foreach(t => t.ensureSchemaCreated)

  def drop() = ()
}

class DevDatabase @Inject() (
                              users: Users,
                              comments: Comments,
                              posts: Posts,
                              boards: Boards,
                              boardCategories: BoardCategories
                            ) extends Database {

  initialize() // running initialization in constructor

  def initialize() = {
    println("Setup database with dev data here")
    create()
  }

  def create() = Seq(boards, boardCategories, posts, comments, users).foreach(t => {
    println("================== created table start ===================")
    t.ensureSchemaCreated.andThen {
      case Failure(t) => println(t)
      case Success(v) => t.loadData()
    }
    println("================== created table end ===================")
  })

  def drop() = ()
}

class ProdDatabase @Inject() (
                               users: Users,
                               comments: Comments,
                               posts: Posts,
                               boards: Boards,
                               boardCategories: BoardCategories
                             ) extends Database {

  initialize() // running initialization in constructor

  def initialize() = {
    println("Setup database with prod data here")
  }

  def create() = (
    // Seq(Favourites, Movies).map(_.ddl).reduce(_ ++ _).create
  )

  def drop() = ()
}