package api

import api.Api._
import play.api.libs.json._
import play.api.mvc._

/*
 * Wrapped Request with additional information for the API (headers: Api Key, Date, Auth-Token, ...)
 */
trait ApiRequestHeader[R <: RequestHeader] {
  val request: R
  val tokenOpt: Option[String] = request.headers.get(HEADER_AUTHORIZATION)

  def method: String = request.method
  def maybeBody: Option[String] = None
}

case class ApiRequestHeaderImpl(request: RequestHeader) extends ApiRequestHeader[RequestHeader]

/*
 * ApiRequestHeader for requests that don't require authentication
 */
class ApiRequest[A](val request: Request[A]) extends WrappedRequest[A](request) with ApiRequestHeader[Request[A]] {
  override def method = request.method
  override def maybeBody: Option[String] = request.body match {
    case body: JsValue => Some(Json.prettyPrint(body))
    case body: String => if (body.length > 0) Some(body) else None
    case body => Some(body.toString)
  }
}

object ApiRequest {
  def apply[A](request: Request[A]): ApiRequest[A] = new ApiRequest[A](request)
}

/*
 * ApiRequest for user aware requests
 */
case class UserAwareApiRequest[A](override val request: Request[A], token: Option[String], userId: Option[String]) extends ApiRequest[A](request) {
  def isLogged = userId.isDefined
}

/*
 * ApiRequest for authenticated requests
 */
case class SecuredApiRequest[A](override val request: Request[A], token: String, userId: String) extends ApiRequest[A](request)
