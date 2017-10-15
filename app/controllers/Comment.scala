package controllers

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

import com.github.t3hnar.bcrypt._
import forms.{CommentForms, NewCommentFormData, NewPostFormData, PostForms}
import models._
import org.pac4j.core.config.Config
import org.pac4j.core.profile._
import org.pac4j.play.PlayWebContext
import org.pac4j.play.scala._
import org.pac4j.play.store.PlaySessionStore
import org.webjars.play.WebJarsUtil
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import play.libs.concurrent.HttpExecutionContext

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

class Comment @Inject()(val config: Config,
                        override val messagesApi: MessagesApi,
                        val commentForms: CommentForms,
                        val boards: Boards,
                        val playSessionStore: PlaySessionStore,
                        implicit val webJarsUtil: WebJarsUtil,
                        val cc: ControllerComponents,
                        val ec: HttpExecutionContext) extends AbstractController(cc) with Security[CommonProfile] with I18nSupport {

  private def getProfiles(implicit request: RequestHeader): List[CommonProfile] = {
    val webContext = new PlayWebContext(request, playSessionStore)
    val profileManager = new ProfileManager[CommonProfile](webContext)
    val profiles = profileManager.getAll(true)
    asScalaBuffer(profiles).toList
  }

  def handleNewComment(board_key: String, post_id: String) = Secure("FormClient") { profiles =>
    Action.async { implicit request =>
      val bpInfo = Await.result(boards.findByIdJoinPost(post_id), Duration.Inf)
      val commentResult: Future[Result] = bpInfo.head match {
        case (b, p) =>
          commentForms.newForm.bindFromRequest.fold(
            formWithErrors => {
              val comments = Await.result(commentForms.comments.findByPost(post_id), Duration.Inf)
              Future.successful( BadRequest(views.html.board.post.view(Some(profiles), formWithErrors, b, p, comments)) )
            },
            commentFormData => {
              val salt = generateSalt
              val commentUser = CommentUser("userid", commentFormData.username, "nickname", commentFormData.email, commentFormData.homepage)

              val commentInfo = CommentInfo(0, 0, request.remoteAddress, 0, request.headers.get("User-Agent").getOrElse("") )

              val comment = Comment(UUID.randomUUID(), b.id, p.id, commentFormData.content, commentFormData.password.bcrypt(salt),
                commentFormData.enable_secret, commentFormData.enable_html, java.sql.Timestamp.valueOf(LocalDateTime.now()),
                java.sql.Timestamp.valueOf(LocalDateTime.now()), "", commentUser, commentInfo, false)

              val cc = p.comment_count + 1
              commentForms.comments.insert(comment).andThen {
                case Failure(cf) => -1
                case Success(cv) => {
                  boards.posts.update(p.incCommentCnt(cc)).andThen {
                    case Failure(pt) => -1
                    case Success(pv) => 1
                  }
                }
              }.map(v => {
                if (v > 0) {
                  Redirect(routes.Post.viewPost(board_key, post_id))
                } else {
                  Ok(views.html.error.notFound(request))
                }
              })
            }
          )
        case _ => Future.successful( Ok(views.html.error.notFound(request)) )
      }
      commentResult
    }
  }

  /*
  def editComment(board_key: String, post_id: String, id: String) = Secure("FormClient") { profiles =>
    Action { request =>
      val comment = Await.result(commentForms.comments.find(UUID.fromString(id)), Duration.Inf)
      comment match {
        case Some(c) => {
          val formData = NewCommentFormData(c.content, c.password, c.commentUser.username, c.commentUser.email, c.commentUser.homepage,
            c.enable_secret, c.enable_html)
          val filledNewForm = commentForms.newForm.fill(formData)
          Ok(views.html.board.post.view(Some(profiles), filledNewForm, b, p))
        }
        case _ => {
          Ok(views.html.error.notFound(request))
        }
      }
    }
  }
  */

  def handleEditComment(board_key: String, post_id: String, id: String) = Secure("FormClient") { profiles =>
    Action.async { implicit request =>

      val bpInfo = Await.result(boards.findByIdJoinPost(post_id), Duration.Inf)
      val commentResult: Future[Result] = bpInfo.head match {
        case (b, p) =>
          val comments = Await.result(commentForms.comments.findByPost(id), Duration.Inf)
          commentForms.newForm.bindFromRequest.fold(
            formWithErrors => Future.successful( BadRequest(views.html.board.post.view(Some(profiles), formWithErrors, b, p, comments)) ),
            commentFormData => {
              val comment = Await.result(commentForms.comments.find(UUID.fromString(id)), Duration.Inf)
              comment match {
                case Some(c) => {
                  val salt = generateSalt
                  val commentUser = CommentUser(c.commentUser.userid, commentFormData.username, c.commentUser.nickname, commentFormData.email, commentFormData.homepage)

                  val commentInfo = CommentInfo(c.commentInfo.like, c.commentInfo.dislike, c.commentInfo.ip_address, c.commentInfo.blame_count, c.commentInfo.device)

                  val comment = Comment(UUID.fromString(id), b.id, p.id, commentFormData.content, commentFormData.password.bcrypt(salt),
                    commentFormData.enable_secret, commentFormData.enable_html, c.created_datetime, java.sql.Timestamp.valueOf(LocalDateTime.now()),
                    c.updated_userid, commentUser, commentInfo, c.deleted)
                  for {
                    id <- commentForms.comments.update(comment)
                  } yield {
                    if (id > 0) {
                      Redirect(routes.Post.viewPost(board_key, post_id))
                    } else {
                      Ok(views.html.error.notFound(request))
                    }
                  }
                }
                case _ => {
                  Future.successful( Ok(views.html.error.notFound(request)) )
                }
              }
            }
          )
        case _ => Future.successful( Ok(views.html.error.notFound(request)) )
      }
      commentResult
    }
  }

  def deleteComment(board_key: String, post_id: String, id: String) = Secure("FormClient") { profiles =>
    Action { implicit request =>
      val board = Await.result(boards.findByIdJoinPost(id), Duration.Inf)
      board.head match {
        case (b, p) => {
          commentForms.comments.delete(UUID.fromString(id))
          Redirect(routes.Post.viewPost(board_key, post_id))
        }
        case _ => {
          Ok(views.html.error.notFound(request))
        }
      }
    }
  }

}