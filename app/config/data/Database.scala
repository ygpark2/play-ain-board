
package config.data

import javax.inject.Inject

import models.{Boards, Posts, Users}

trait Database {
  def create(): Unit
  def drop(): Unit
}

class TestDatabase @Inject() (
                               users: Users,
                               posts: Posts,
                               boards: Boards
                             ) extends Database {

  initialize() // running initialization in constructor

  def initialize() = {
    println("Setup database with test data here")
  }

  def create() = Seq(users, posts, boards).foreach(t => t.ensureSchemaCreated)

  def drop() = ()
}

class DevDatabase @Inject() (
                              users: Users,
                              posts: Posts,
                              boards: Boards
                            ) extends Database {

  initialize() // running initialization in constructor

  def initialize() = {
    println("Setup database with dev data here")
    create()
  }

  def create() = Seq(users, posts, boards).foreach(t => {
    println("================== created table start ===================")
    t.ensureSchemaCreated
    t.loadData()
    println("================== created table end ===================")
  })

  def drop() = ()
}

class ProdDatabase @Inject() (
                               users: Users,
                               posts: Posts,
                               boards: Boards
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