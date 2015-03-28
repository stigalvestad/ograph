package controllers

import javax.inject.Singleton

import models.Models.{Race, RetrievalLog, logFormat}
import org.joda.time.DateTime
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.Future

@Singleton
class FakeController extends Controller {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[FakeController])
  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  private final val RACE = "Race"
  private final val RUNNER = "Runner"
  private final val RUNNER_RACES = "Runner_Races"

  private var existingEventorReqCount = 0
  private val MAX_SIMULTANEOUS_EVENTOR_REQS = 4

  def races(eventorId: Long) = Action.async {
    val r = 1 to 20
    val races = r.map { x => Race(x+"", RACE + x, new DateTime(), 1, 2, None) }
    Future {
      Ok(Json.toJson(races))
    }
  }

  def fetchRaceResults(raceEventorId: String) = Action.async {
    logger.info(s"$raceEventorId | Count: $existingEventorReqCount")
    if (existingEventorReqCount >= MAX_SIMULTANEOUS_EVENTOR_REQS) {
      logger.info(s"$raceEventorId | Too many! $existingEventorReqCount")
      Future {
        Results.Status(429)
      }
    } else {
      logger.info(s"$raceEventorId | Less than limit, continued. $existingEventorReqCount")
      existingEventorReqCount = existingEventorReqCount + 1
      Future {
        val y = 3000 + (Math.random() * 3000).toInt
        Thread.sleep(y)
        existingEventorReqCount = existingEventorReqCount - 1
        Ok(Json.toJson(RetrievalLog(new DateTime, 200, "OK", RACE, raceEventorId, (s"vg.no/$y"), None, None, Some(true))))
      }
    }
  }

}