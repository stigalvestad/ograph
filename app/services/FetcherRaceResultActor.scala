package services

import java.net.ConnectException
import java.util.concurrent.TimeoutException

import akka.actor.{Actor, ActorLogging, Props}
import dao.{LogDAO, RaceResultDAO, RunnerDAO}
import models.Models
import models.Models.{RaceResult, RetrievalLog}
import org.joda.time.DateTime
import play.api.libs.ws.WSResponse
import services.FetcherCoordinatorActor.{EventorItem, FetchFailed, FetchedOk}
import services.FetcherRaceResultActor.FetchRaceResult
import services.SpecialExceptions.{FetchedRecentlyException, NotFoundException}
import services.Throttler.{CanFetch, DoneFetching, EventorIsUnavailable, OkToFetch}

import scala.concurrent.Future

class FetcherRaceResultActor(eventorItem: EventorItem, eventorFetcher: EventorFetcher, logDAO: LogDAO, parserService: XmlParserService,
                   resultDAO: RaceResultDAO, runnerDAO: RunnerDAO,
                   resultEnhancer: ResultEnhancerService) extends Actor with ActorLogging {

  import context._
  val throttler = actorSelection("../../throttler")
  private final val RACE = "Race"

  override def receive: Receive = {
    case EventorIsUnavailable =>
      parent ! EventorIsUnavailable
    case FetchRaceResult =>
      log debug s"\t${eventorItem.id}?"
      throttler ! CanFetch(eventorItem)
    case OkToFetch =>
      log debug s"\t${eventorItem.id}..."
      fetchRaceResult
  }

  override def postStop() = log debug s"Stopped"

  private def fetchRaceResult = {

    val timeStamp = new DateTime()

    val result = for {
      shouldFetch <- shouldFetchRace(eventorItem.id)
      response <- fetchRaceWithPreLogging(timeStamp)
      log <- logEventorRequest(timeStamp, response)
      raceResults <- handleRaceResultResponse(response, log)
      saveOk <- saveResults(raceResults, log)
    } yield {
      parent ! FetchedOk(eventorItem)
      log
    }

    result.recover(getExceptionHandler)
  }

  private def logEventorRequest(timeStamp: DateTime, response: WSResponse): Future[Models.RetrievalLog] = {
    val ct = new CommonTasks
    val updatedLog: RetrievalLog = RetrievalLog(timeStamp, response.status, response.statusText, RACE,
      eventorItem.id, eventorItem.url, None, None, None)
    ct.logEventorRequest(response, updatedLog, logDAO)
  }

  private def respondOnFailure(msg: String): Unit = {
    throttler ! DoneFetching(eventorItem)
    parent ! FetchFailed(eventorItem, msg)
  }

  private def shouldFetchRace(raceEventorId: String): Future[Boolean] = {

    val ct = new CommonTasks

    logDAO.fetchedRecently(raceEventorId, RACE).map { logs =>
      logs.headOption match {
        case Some(logRace) =>
          if (ct.fetchedAndProcessedOk(logRace)) throw new SpecialExceptions.FetchedRecentlyException
          else true
        case None => true
      }
    }
  }

  private def saveResults(results: List[RaceResult], log: RetrievalLog) = {
    if (results.isEmpty) throw new SpecialExceptions.NotFoundException
    val resultSaves = results.map { res => resultDAO.upsert(res) }
    val runners = results.map { _.runner }.distinct
    val runnerSaves = runners.filter(_.eventorId.isDefined).map(runner => runnerDAO.upsert(runner))
    val allSaves = resultSaves ++ runnerSaves
    for {
      savesDone <- Future sequence allSaves.toList
      updatedLog <- logDAO.markLogOk(log)
    } yield updatedLog
  }

  private def fetchRaceWithPreLogging(timeStamp: DateTime) : Future[WSResponse] = {
    val retrievalLog = RetrievalLog(timeStamp, 0, "", RACE, eventorItem.id, eventorItem.url, None, None, None)
    for {
      l <- logDAO.insert(retrievalLog)
      response <- eventorFetcher.getResource(eventorItem.url)
    } yield {
      log debug s"\t\t\t${eventorItem.id}!"
      throttler ! DoneFetching(eventorItem)
      response
    }
  }

  private def handleRaceResultResponse(r: WSResponse, log: RetrievalLog): Future[List[RaceResult]] = {
    r.status match {
      case 200 => parserService.parseResults(r.xml).map(raceResults => resultEnhancer.enhanceResults(raceResults))
      case 404 => throw new SpecialExceptions.NotFoundException
      case _   => throw new Exception(s"Unexpected response (${log.statusCode}): ${log.statusText}")
    }
  }

  private def getExceptionHandler: PartialFunction[Throwable, Any] = {
    val excHandler: PartialFunction[Throwable, Any] = {
      case e: FetchedRecentlyException =>
        log debug s"Already fetched ${eventorItem.id}"
        parent ! FetchedOk //if it is already fetched, that's ok
      case e: ConnectException =>
        val msg = s"Eventor er muligens nede, svarer ikke."
        log warning msg
        respondOnFailure(msg)
        throttler ! EventorIsUnavailable
      case e: TimeoutException =>
        val msg = s"Det tok for lang tid å hente løpet fra Eventor"
        log warning msg
        respondOnFailure(msg)
        throttler ! EventorIsUnavailable
      case e: NotFoundException =>
        val msg = s"Løpet finnes ikke i Eventor ${eventorItem.id}"
        log warning msg
        respondOnFailure(msg)
      case e: Exception =>
        val msg = s"Uventet feil. ${e.getClass}: ${e.getMessage}"
        log error msg
        respondOnFailure(msg)
    }
    excHandler
  }

}

object FetcherRaceResultActor {
  def props(eventorResource: EventorItem, eventorFetcher: EventorFetcher, logDAO: LogDAO, parserService: XmlParserService,
            resultDAO: RaceResultDAO, runnerDAO: RunnerDAO,
            resultEnhancer: ResultEnhancerService) = Props(new FetcherRaceResultActor(eventorResource, eventorFetcher, logDAO, parserService, resultDAO, runnerDAO, resultEnhancer))
  case object FetchRaceResult
}
