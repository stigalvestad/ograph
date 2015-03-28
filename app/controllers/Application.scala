package controllers

import javax.inject.{Inject, Singleton}

import dao.RaceResultDAO
import models.Models.{Organisation, Race, RaceClass, RaceResult, Runner}
import org.joda.time.DateTime
import org.slf4j.{Logger, LoggerFactory}
import play.api.mvc._

@Singleton
class Application @Inject() (raceResultDAO: RaceResultDAO) extends Controller {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[Application])
  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def index = Action {
    logger.info("Serving index page...")
    Ok(views.html.index("bla bla"))
  }

  def testRaceResult = Action.async {
    val runner = Runner(Some("123"), "Stig", "Alvestad", None)
    val race = Race("68", "Hoestspretten", new DateTime(2014, 10, 3, 0, 0, 0, 0), 4, 6, Some("http://sdf.no"))
    val raceClass = RaceClass("345", race, "H17-18", Some(22), Some(6.5))
    val org = Organisation(Some("66"), "Ganddal IL")
    val result = RaceResult("423", runner, org, raceClass, Some(51 * 60 * 1000), "OK", Some(8), None)
    logger.info(s"Preparing to insert test row")
    raceResultDAO.upsert(result).map { i =>
      logger.info(s"Inserted successfully")
      Ok("Nof rows updated: " + i)
    }
  }
}