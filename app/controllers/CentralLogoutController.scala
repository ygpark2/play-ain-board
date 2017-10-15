package controllers;

import org.pac4j.play.LogoutController

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global
// import play.api.mvc.Action;
import play.api.mvc._

class CentralLogoutController extends LogoutController {

    setDefaultUrl("http://localhost:9000/?defaulturlafterlogoutafteridp")
    setLocalLogout(false)
    setCentralLogout(true)
    setLogoutUrlPattern("http://localhost:9000/.*")

}