package controllers

import java.util.UUID
import javax.inject.Inject

import forms.{BoardForms, UserForms}
import models.Board
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
import org.webjars.play.WebJarsUtil
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc._
import play.libs.concurrent.HttpExecutionContext

import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class Board @Inject()(val config: Config,
                      override val messagesApi: MessagesApi,
                      val boardForms: BoardForms,
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

  def newBoard = Action { implicit request =>
    val formClient = config.getClients.findClient("FormClient").asInstanceOf[FormClient]
    // Ok(views.html.loginForm.render(formClient.getCallbackUrl))
    Ok(views.html.board.newForm(boardForms.newForm))
  }

  def handleNewBoard() = Action.async { implicit request =>
    logger.debug("handle signup================> ")
    boardForms.newForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.board.newForm(formWithErrors))),
      boardFormData => {
        println("--------------- password matched -------------------------------")
        // val salt = generateSalt
        val board = Board(UUID.randomUUID(), boardFormData.key, boardFormData.name, boardFormData.mobile_name, boardFormData.order, boardFormData.search_flag)
        for {
          id <- boardForms.boards.insert(board)
        } yield {
          println("board created id => " + id)
          if (id > 0) {
            Ok(views.html.board.newForm(boardForms.newForm))
          } else {
            Ok(views.html.board.newForm(boardForms.newForm))
          }
        }
      }
    )
  }

}