package controllers

import javax.inject.Inject

import models.TemperatureRepo
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{Action, Controller}

import scala.concurrent.Future

class TemperatureApp @Inject()(temperatureRepo: TemperatureRepo) extends Controller {

  /*
  def showTemperatures = Action.async { implicit rs =>
    temperatureRepo.all.map(temperatures => Ok(views.html.temperatures(temperatures)))
  }

  def add(temperature: Float) = Action.async { implicit rs =>
    temperatureRepo.add(temperature)
    Future(Redirect(routes.TemperatureApp.showTemperatures()))
  }
  */

}