package controllers

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

import forms.{NewPostFormData, PostForms, UserForms}
import models._
import org.pac4j.core.client.{Clients, IndirectClient}
import org.pac4j.core.config.Config
import org.pac4j.core.context.Pac4jConstants
import org.pac4j.core.credentials.Credentials
import org.pac4j.core.profile._
import org.pac4j.core.util.CommonHelper
import org.pac4j.http.client.indirect.FormClient
import org.pac4j.jwt.config.signature.SecretSignatureConfiguration
import org.pac4j.jwt.profile.JwtGenerator
import org.pac4j.play.PlayWebContext
import org.pac4j.play.scala._
import org.pac4j.play.store.PlaySessionStore
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc._
import play.libs.concurrent.HttpExecutionContext
import com.github.t3hnar.bcrypt._

import scala.collection.JavaConversions._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

class Post @Inject()(val config: Config,
                     val messagesApi: MessagesApi,
                     val postForms: PostForms,
                     val boards: Boards,
                     val playSessionStore: PlaySessionStore,
                     implicit val webJarAssets: WebJarAssets,
                     override val ec: HttpExecutionContext) extends Controller with Security[CommonProfile] with I18nSupport {

  private def getProfiles(implicit request: RequestHeader): List[CommonProfile] = {
    val webContext = new PlayWebContext(request, playSessionStore)
    val profileManager = new ProfileManager[CommonProfile](webContext)
    val profiles = profileManager.getAll(true)
    asScalaBuffer(profiles).toList
  }

  def newPost(board_key: String) = Secure("FormClient") { profiles =>
    Action { request =>
      val board = Await.result(boards.findByKey(board_key), Duration.Inf)
      board match {
        case Some(b) => {
          Ok(views.html.board.post.newForm(Some(profiles), postForms.newForm, b))
        }
        case _ => {
          Ok(views.html.error.notFound(request))
        }
      }
    }
  }

  def handleNewPost(board_key: String) = Secure("FormClient") { profiles =>
    Action.async { implicit request =>
      println("-----------------------------------------")
      logger.debug("star handle post ================> ")
      val board = Await.result(boards.findByKey(board_key), Duration.Inf)
      val defaultBoard = Board(UUID.randomUUID(), "", "", "", 0, false)
      val boardVal = board.getOrElse(defaultBoard)

      postForms.newForm.bindFromRequest.fold(
        formWithErrors => Future.successful(BadRequest(views.html.board.post.newForm(Some(profiles), formWithErrors, boardVal))),
        postFormData => {
          val salt = generateSalt
          println("--------------- password matched -------------------------------")
          val postUser = PostUser("userid", postFormData.username, "nickname", postFormData.email, postFormData.homepage)

          val postConf = PostConf(postFormData.enable_secret, postFormData.enable_html, false, postFormData.enable_notice, false)
          /* enable_secret: Boolean,
             enable_html: Boolean,
             enable_hide_comment: Boolean,
             enable_notice: Boolean,
             enable_receive_email: Boolean */

          val postInfo = PostInfo(0, 0, 0, 0, "", 0, "", 0, 0)
          /*  link_count: Int,
              hit: Int,
              like: Int
              dislike: Int,
              ip_address: String,
              blame_count: Int,
              device: String,
              attached_file_count: Int,
              attached_image_count: Int */
          val post = Post(UUID.randomUUID(), board.get.id, null, postFormData.title, postFormData.content, postFormData.password.bcrypt(salt),
            java.sql.Timestamp.valueOf(LocalDateTime.now()), java.sql.Timestamp.valueOf(LocalDateTime.now()), "", 0,
            java.sql.Timestamp.valueOf(LocalDateTime.now()), postUser, postConf, postInfo, false)

          for {
            id <- postForms.posts.insert(post)
          } yield {
            println("created id => " + id)
            if (id > 0) {
              Redirect(routes.Post.listPost(board_key))
            } else {
              Ok(views.html.board.post.newForm(Some(profiles), postForms.newForm, boardVal))
            }
          }
        }
      )
    }
  }

  def editPost(board_key: String, id: String) = Secure("FormClient") { profiles =>
    Action { request =>
      val board = Await.result(boards.findByIdJoinPost(id), Duration.Inf)
      board.head match {
        case (b, p) => {
          val formData = NewPostFormData(p.title, p.content, p.password, p.postUser.username, p.postUser.email, p.postUser.homepage,
            p.postConf.enable_secret, p.postConf.enable_html, p.postConf.enable_notice)
          val filledNewForm = postForms.newForm.fill(formData)
          Ok(views.html.board.post.editForm(Some(profiles), filledNewForm, b, p))
        }
        case _ => {
          Ok(views.html.error.notFound(request))
        }
      }
    }
  }

  def handleEditPost(board_key: String, id: String) = Secure("FormClient") { profiles =>
    Action.async { implicit request =>
      val boardList = Await.result(boards.findByIdJoinPost(id), Duration.Inf)
      if (boardList.length > 0) {
        val (board, post) = boardList.head

        postForms.newForm.bindFromRequest.fold(
          formWithErrors => Future.successful(BadRequest(views.html.board.post.newForm(Some(profiles), formWithErrors, board))),
          postFormData => {
            val copyPostUser = post.postUser.copy(
              email = postFormData.email,
              homepage = postFormData.homepage,
              username = postFormData.username
            )
            val copyPostConf = post.postConf.copy(
              enable_html = postFormData.enable_html,
              enable_notice = postFormData.enable_notice,
              enable_secret = postFormData.enable_secret
            )
            val copyPost = post.copy(
              title = postFormData.title,
              content = postFormData.content,
              password = postFormData.password,
              updated_datetime = java.sql.Timestamp.valueOf(LocalDateTime.now()),
              postUser = copyPostUser,
              postConf = copyPostConf
            )

            for {
              id <- postForms.posts.update(copyPost)
            } yield {
              if (id > 0) {
                Redirect(routes.Post.listPost(board_key))
              } else {
                Ok(views.html.board.post.newForm(Some(profiles), postForms.newForm, board))
              }
            }
          }
        )
      } else {
        Future.successful(Ok(views.html.error.notFound(request)))
      }

    }
  }

  def deletePost(board_key: String, id: String) = Secure("FormClient") { profiles =>
    Action { request =>
      val board = Await.result(boards.findByIdJoinPost(id), Duration.Inf)
      board.head match {
        case (b, p) => {
          postForms.posts.delete(UUID.fromString(id))
          Redirect(routes.Post.listPost(board_key))
        }
        case _ => {
          Ok(views.html.error.notFound(request))
        }
      }
    }
  }

  def viewPost(board_key: String, id: String) = Secure("AnonymousClient, FormClient") { profiles =>
    Action { request =>
      val board = Await.result(boards.findByIdJoinPost(id), Duration.Inf)
      board.head match {
        case (b, p) => {
          // increase hit count
          profiles.foreach(println)
          val hit = p.postInfo.hit + 1
          postForms.posts.update(p.incHit(hit))
          Ok(views.html.board.post.view(Some(profiles), p, b))
        }
        case _ => {
          Ok(views.html.error.notFound(request))
        }
      }
    }
  }

  def listPost(board_key: String, page: Int, orderBy: Int, filter: String) = Secure("AnonymousClient, FormClient") { profiles =>
    Action.async { implicit request =>
      val boardList = boards.postList(board_key, page = page, orderBy = orderBy, filter = ("%" + filter + "%"))
      boardList.map(ps => {
        Ok(views.html.board.post.list(Some(profiles), ps, orderBy, filter))
      })
    }
  }

}