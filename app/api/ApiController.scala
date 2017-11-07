package api

import javax.inject.Inject

import api.Api.Sorting._
import api.ApiError._
// import dao.UserDAO
import models.Page
import play.api.i18n.I18nSupport
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/*
* Controller for the API
*/
trait ApiController extends BaseController with I18nSupport {

  // @Inject val userDao: UserDAO
  //  @Inject val messagesAction: MessagesActionBuilder

  //////////////////////////////////////////////////////////////////////
  // Implicit transformation utilities

  implicit def objectToJson[T](o: T)(implicit tjs: Writes[T]): JsValue = Json.toJson(o)
  implicit def result2FutureResult(r: ApiResult): Future[ApiResult] = Future.successful(r)

  //////////////////////////////////////////////////////////////////////
  // Custom Actions

  def ApiAction(action: ApiRequest[Unit] => Future[ApiResult]) = ApiActionWithParser(parse.empty)(action)
  def ApiActionWithBody(action: ApiRequest[JsValue] => Future[ApiResult]) = ApiActionWithParser(parse.json)(action)

  def SecuredApiAction(action: SecuredApiRequest[Unit] => Future[ApiResult]) = SecuredApiActionWithParser(parse.empty)(action)
  def SecuredApiActionWithBody(action: SecuredApiRequest[JsValue] => Future[ApiResult]) = SecuredApiActionWithParser(parse.json)(action)

  def UserAwareApiAction(action: UserAwareApiRequest[Unit] => Future[ApiResult]) = UserAwareApiActionWithParser(parse.empty)(action)
  def UserAwareApiActionWithBody(action: UserAwareApiRequest[JsValue] => Future[ApiResult]) = UserAwareApiActionWithParser(parse.json)(action)

  // Creates an Action checking that the Request has all the common necessary headers with their correct values (X-Api-Key, Date)
  private def ApiActionCommon[A](parser: BodyParser[A])(action: ApiRequest[A] => Future[ApiResult]) = Action.async(parser) { implicit request =>
    implicit val apiRequest = ApiRequest(request)
    implicit val lang = apiRequest.lang
    val futureApiResult: Future[ApiResult] = action(apiRequest)
    futureApiResult.map {
      case error: ApiError => error.saveLog.toResult
      case response: ApiResponse => response.toResult
    }
  }

  // Basic Api Action
  private def ApiActionWithParser[A](parser: BodyParser[A])(action: ApiRequest[A] => Future[ApiResult]) = ApiActionCommon(parser) { apiRequest =>
    action(apiRequest)
  }

  // Secured Api Action that requires authentication. It checks the Request has the correct X-Auth-Token heaader
  private def SecuredApiActionWithParser[A](parser: BodyParser[A])(action: SecuredApiRequest[A] => Future[ApiResult]) = ApiActionCommon(parser) { apiRequest =>
    implicit val messages = apiRequest.messages

    apiRequest.tokenOpt match {
      case None => errorTokenNotFound
      case Some(token) => errorTokenUnknown
      /*
        userDao.findByAuthToken(token).flatMap {
        case None => errorTokenUnknown
        case Some(user) if user.isExpired => {
          userDao.deleteAuthToken(user.id)
          errorTokenExpired
        }
        case Some(user) => action(SecuredApiRequest(apiRequest.request, token, user.id))
      }
      */
    }
  }

  // User Aware Api Action that requires authentication. It checks the Request has the correct X-Auth-Token heaader
  private def UserAwareApiActionWithParser[A](parser: BodyParser[A])(action: UserAwareApiRequest[A] => Future[ApiResult]) = ApiActionCommon(parser) { (apiRequest) =>
    implicit val messages = apiRequest.messages

    apiRequest.tokenOpt match {
      case None => errorTokenNotFound
      case Some(token) => errorTokenUnknown
        /*
        userDao.findByAuthToken(token).flatMap {
        case None => errorTokenUnknown
        case Some(user) if user.isExpired => {
          userDao.deleteAuthToken(user.id)
          errorTokenExpired
        }
        case Some(user) => action(UserAwareApiRequest(apiRequest.request, Some(token), Some(user.id)))
      }
        */
    }
  }

  //////////////////////////////////////////////////////////////////////
  // Auxiliar methods to create ApiResults from writable JSON objects

  def ok[A](obj: A, headers: (String, String)*)(implicit w: Writes[A]): Future[ApiResult] = Future.successful(ApiResponse.ok(obj, headers: _*))
  def ok[A](futObj: Future[A], headers: (String, String)*)(implicit w: Writes[A]): Future[ApiResult] = futObj.map(obj => ApiResponse.ok(obj, headers: _*))

  private def itemOrError[A](opt: Option[A], headers: (String, String)*)(implicit w: Writes[A], req: RequestHeader): ApiResult = opt match {
    case Some(i) => ApiResponse.ok(i, headers: _*)
    case None => ApiError.errorItemNotFound
  }
  def maybeItem[A](opt: Option[A], headers: (String, String)*)(implicit w: Writes[A], req: RequestHeader): Future[ApiResult] = Future.successful(itemOrError(opt, headers: _*))
  def maybeItem[A](futOpt: Future[Option[A]], headers: (String, String)*)(implicit w: Writes[A], req: RequestHeader): Future[ApiResult] = futOpt.map(opt => itemOrError(opt, headers: _*))

  def page[A](p: Page[A], headers: (String, String)*)(implicit w: Writes[A]): Future[ApiResult] = Future.successful(ApiResponse.ok(p.items, p, headers: _*))
  def page[A](futP: Future[Page[A]], headers: (String, String)*)(implicit w: Writes[A]): Future[ApiResult] = futP.map(p => ApiResponse.ok(p.items, p, headers: _*))

  def sortedPage[A](
                     sortBy: Option[String],
                     allowedFields: Seq[String],
                     default: String,
                     name: String = "sort",
                     headers: Seq[(String, String)] = Seq()
                   )(p: Seq[(String, Boolean)] => Future[Page[A]])(implicit w: Writes[A], req: RequestHeader): Future[ApiResult] = {
    processSortByParam(sortBy, allowedFields, default, name).fold(
      error => error,
      sortFields => page(p(sortFields), headers: _*)
    )
  }

  def created[A](obj: A, headers: (String, String)*)(implicit w: Writes[A]): Future[ApiResult] = Future.successful(ApiResponse.created(obj, headers: _*))
  def created[A](futObj: Future[A], headers: (String, String)*)(implicit w: Writes[A]): Future[ApiResult] = futObj.map(obj => ApiResponse.created(obj, headers: _*))
  def created(headers: (String, String)*): Future[ApiResult] = Future.successful(ApiResponse.created(headers: _*))

  def accepted[A](obj: A, headers: (String, String)*)(implicit w: Writes[A]): Future[ApiResult] = Future.successful(ApiResponse.accepted(obj, headers: _*))
  def accepted[A](futObj: Future[A], headers: (String, String)*)(implicit w: Writes[A]): Future[ApiResult] = futObj.map(obj => ApiResponse.accepted(obj, headers: _*))
  def accepted(headers: (String, String)*): Future[ApiResult] = Future.successful(ApiResponse.accepted(headers: _*))

  def noContent(headers: (String, String)*): Future[ApiResult] = Future.successful(ApiResponse.noContent(headers: _*))

  //////////////////////////////////////////////////////////////////////
  // More auxiliar methods

  // Reads an object from an ApiRequest[JsValue] handling a possible malformed error
  def readFromRequest[T](f: T => Future[ApiResult])(implicit request: ApiRequest[JsValue], rds: Reads[T], req: RequestHeader): Future[ApiResult] = {
    request.body.validate[T].fold(
      errors => errorBodyMalformed(errors),
      readValue => f(readValue)
    )
  }

  /*
	* Process a "sort" URL GET param with a specific format. Returns the corresponding description as a list of pairs field-order,
	* where field is the field to sort by, and order indicates if the sorting has an ascendent or descendent order.
	* The input format is a string with a list of sorting fields separated by commas and with preference order. Each field has a
	* sign that indicates if the sorting has an ascendent or descendent order.
	* Example: "-done,order,+id"  Seq(("done", DESC), ("priority", ASC), ("id", ASC))   where ASC=false and DESC=true
	*
	* Params:
	*  - sortBy: optional String with the input sorting description.
	*  - allowedFields: a list of available allowed fields to sort.
	*  - default: String with the default input sorting description.
	*  - name: the name of the param.
	*/
  def processSortByParam(sortBy: Option[String], allowedFields: Seq[String], default: String, name: String = "sort")(implicit req: RequestHeader): Either[ApiError, Seq[(String, Boolean)]] = {
    val signedFieldPattern = """([+-]?)(\w+)""".r
    val fieldsWithOrder = signedFieldPattern.findAllIn(sortBy.getOrElse(default)).toList.map {
      case signedFieldPattern("-", field) => (field, DESC)
      case signedFieldPattern(_, field) => (field, ASC)
    }
    // Checks if every field is within the available allowed ones
    if ((fieldsWithOrder.map(_._1) diff allowedFields).isEmpty)
      Right(fieldsWithOrder)
    else
      Left(errorParamMalformed(name))
  }
}