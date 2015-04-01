package controllers

import java.net.ConnectException
import javax.inject.{Inject, Singleton}

import akka.actor.ActorRef
import akka.pattern.{AskTimeoutException, ask}
import akka.util.Timeout
import dao.{LogDAO, ParticipationDAO, RaceResultDAO, RunnerDAO}
import models.Models.logFormat
import org.joda.time.DateTime
import org.slf4j.{Logger, LoggerFactory}
import play.api.Play.current
import play.api._
import play.api.libs.concurrent.Akka
import play.api.libs.json.Json
import play.api.mvc._
import services.FetcherCoordinatorActor._
import services.OgraphConstants.{COORDINATOR_TIMEOUT_SEC, EVENTOR_BASE_URL}
import services._

import scala.concurrent.duration._

@Singleton
class RunnerController @Inject() (eventorFetcher: EventorFetcher, logDAO: LogDAO, parserService: XmlParserService,
                                  resultDAO: RaceResultDAO, runnerDAO: RunnerDAO, participationsDAO: ParticipationDAO,
                                  searchService: RunnerSearchService, resultEnhancer: ResultEnhancerService,
                                  participationService: ParticipationService, racesService: RacesService)
  extends Controller {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[RunnerController])
  private final val RUNNER = "Runner"
  implicit val context = scala.concurrent.ExecutionContext.Implicits.global


  def fetchRaceResults(raceEventorIds: String, jobId: String) = Action.async {

    val toFetch = raceEventorIds.split("-").map{raceEvId =>
      EventorItem(createRaceResultUrl(raceEvId), raceEvId)}.distinct.toList

    (createCoordinatorActor(jobId) ? FetchThese(toFetch))(Timeout((COORDINATOR_TIMEOUT_SEC * toFetch.size) seconds))
      .mapTo[CurrentStatus].map(onSuccess) recover(onFailure)
  }

  def getJobStatus(jobId: String) = Action.async {
    val jobCoordinator = Akka.system.actorSelection(s"/user/cord_$jobId")

    (jobCoordinator ? GetStatus)(Timeout(COORDINATOR_TIMEOUT_SEC seconds))
      .mapTo[CurrentStatus].map(onSuccess) recover(onFailure)
  }

  private def createRaceResultUrl(raceEventorId: String): String = {
    s"$EVENTOR_BASE_URL/api/results/event?eventId=$raceEventorId"
  }

  private def createCoordinatorActor(jobId: String): ActorRef = {
    Akka.system.actorOf(FetcherCoordinatorActor.props(
      eventorFetcher, logDAO, parserService, resultDAO, runnerDAO, resultEnhancer), s"cord_$jobId")
  }

  def onFailure: PartialFunction[Throwable, Result] = {
    case e: AskTimeoutException =>
      logger.error("Too long time to complete, " + e.getClass + ": " + e.getMessage)
      RequestTimeout("Jobben med å hente løp svarte ikke tidsnok")
    case e: Exception =>
      logger.error("Something failed, reason is " + e.getClass + ": " + e.getMessage)
      InternalServerError("Uventet feil. " + e.getClass + ": " + e.getMessage)
  }

  def onSuccess: PartialFunction[CurrentStatus, Result] = {
    case CurrentStatus(fullStatus, false) =>
      logger.info("Fetched " + fullStatus.filter(_.status == "OK").size + "/" + fullStatus.size
        + " items successfully. Details: " + fullStatus)
      Ok(Json.toJson(fullStatus))
    case CurrentStatus(fullStatus, true) =>
      logger.warn("Eventor Unavailable! Fetched " + fullStatus.filter(_.status == "OK").size + "/" + fullStatus.size
        + " items successfully. Details: " + fullStatus)
      FailedDependency(Json.toJson(fullStatus))
  }

  /**
   * Dependencies
   * logDAO
   */
  def status(eventorId: String) = Action.async {
    logger.info(s"$eventorId -- Finding status")
    logDAO.fetchedRecently(eventorId, RUNNER).map { logs => Ok(Json.toJson(logs)) }
  }

  /**
   * Dependencies
   * resultDAO
   */
  def results(eventorId: String) = Action.async {
    resultDAO.get(eventorId).map(results => {
      logger.info(s"$eventorId -- found ${results.size} results")
      Ok(Json.toJson(results))
    })
  }

  /**
   * Dependencies
   * runnerDAO, searchService
   */
  def query(searchTerm: Option[String]) = Action.async {
    logger.info(s"Query for $searchTerm")
    val all = runnerDAO.getAll()
    if (searchTerm.isDefined) {

      all.map { runners =>
        val filtered = searchService.filterRunners(searchTerm.get, runners)
        logger.info(s"SEARCHING: found ${filtered.size} results for search term ${searchTerm.get}")
        Ok(Json.toJson(filtered))
      }
    } else {
      logger.info(s"SEARCHING: Returning all runners")
      all.map(runners => Ok(Json.toJson(runners)))
    }
  }

  /**
   *
   * Dependencies
   * logDAO, participationsDAO, participationService, eventorFetcher, parserService
   *
   * Start: 2011-1-1
   * want to ensure that I know about all races from start to today.
   *
   * fetch participations for this runner; pList
   * after processing it, I want to know:
   *  - which races have r participated in that I know about
   *  - which period do I not know about
   */
  def races(eventorId: String) = Action.async {
    logger.info(s"$eventorId -- Find which races runner has participated in")

    val missingRaces = racesService.findMissingRaces(eventorId, new DateTime)

    missingRaces.map { races =>
      logger.info(s"$eventorId -- Need to fetch ${races.size} races")
      Ok(Json.toJson(races))
    } recover getExceptionHandler("races", eventorId)
  }



  private def getExceptionHandler(resourceLabel: String, eventorId: String) = {
    val excHandler: PartialFunction[Throwable, Result] = {
      case e: SpecialExceptions.TooManyRequestsException => Results.Status(429)
      case e: SpecialExceptions.FetchedRecentlyException => BadRequest(s"Fetched $resourceLabel ($eventorId) recently. Must wait a bit")
      case e: SpecialExceptions.NotFoundException =>
        val msg = e.getMessage
        logger.warn(s"Got 404 from eventor when fetching $resourceLabel ($eventorId): $msg")
        NotFound
      case e: UnexpectedException =>
        val msg = e.getMessage
        logger.error(s"Something failed unexpectedly when fetching $resourceLabel ($eventorId): $msg")
        InternalServerError(e.getMessage)
      case e: ConnectException =>
        FailedDependency
    }
    excHandler
  }

}