package services

import javax.inject.{Inject, Singleton}

import dao.{LogDAO, ParticipationDAO}
import models.Models.{Participation, Race, RetrievalLog}
import org.joda.time.{DateTime, Interval}
import org.joda.time.format.DateTimeFormat
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.ws.WSResponse
import services.OgraphConstants.EVENTOR_BASE_URL

import scala.concurrent.Future

@Singleton
class RacesService @Inject() (
    eventorFetcher: EventorFetcher, 
    logDAO: LogDAO, 
    parserService: XmlParserService,
    participationsDAO: ParticipationDAO,
    participationService: ParticipationService) {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  private final val logger: Logger = LoggerFactory.getLogger(classOf[RacesService])
  private final val YMD_FORMAT = DateTimeFormat.forPattern("yyyy-MM-dd")

  private final val RACE = "Race"
  private final val RUNNER_RACES = "Runner_Races"

  def findMissingRaces(eventorId: String, now: DateTime): Future[List[Race]] = {
    for {
      participationInfo <- findParticipationInfo(eventorId, now)
      racesAlreadyFetched <- logDAO.fetched(RACE)
      newParticipation <- findNewParticipation(eventorId, participationInfo)
    } yield {
      val races = participationInfo.participations ++ newParticipation.races
      logger.info(s"$eventorId -- Finished: found ${races.size} races including newest information")
      findMissingRaces(races, racesAlreadyFetched)
    }
  }
  
  private def findMissingRaces(races: List[Race], racesAlreadyFetched: List[RetrievalLog]): List[Race] = {
    val ct = new CommonTasks
    val racesFetchedSuccessfully = racesAlreadyFetched.filter { l => ct.fetchedAndProcessedOk(l) }.map { l => l.eventorId }
    val missingRaces = races.filter { r => !racesFetchedSuccessfully.contains(r.eventorId) }
    logger.info(s"Missing ${missingRaces.size} / ${races.size} races")
    missingRaces
  }

  case class ParticipationInfo(unknownPeriod: Interval, participations: List[Race])

  private def findNewParticipation(eventorId: String, pInfo: ParticipationInfo): Future[Participation] = {
    val url = makeRacesUrl(eventorId, pInfo)
    val from = pInfo.unknownPeriod.getStart
    val to = pInfo.unknownPeriod.getEnd
    val ct = new CommonTasks

    val participationsF = for {
      ok <- ensureRaceListShouldBeFetched(eventorId, pInfo)
      response <- eventorFetcher.getResource(url)
      log <- ct.logEventorRequest(response, 
          RetrievalLog(new DateTime(), response.status, response.statusText, RUNNER_RACES, eventorId, url, Some(from), Some(to), None), logDAO)
      races <- handleRacesResponse(response, log)
      stored <- participationsDAO.insert(Participation(eventorId, from, to, races))
      updatedLog <- logDAO.markLogOk(log)
    } yield Participation(eventorId, from, to, races)

    participationsF.map { p => p } recover {
      case e: SpecialExceptions.FetchedRecentlyException =>
        logger.info(s"$eventorId -- Has updated race list, don't need to ask eventor")
        Participation(eventorId, pInfo.unknownPeriod.getStart, pInfo.unknownPeriod.getEnd, List())
    }
  }
  
  private def handleRacesResponse(r: WSResponse, log: RetrievalLog): Future[List[Race]] = {
    r.status match {
      case 200 => {
        parserService.parseRaces(r.xml)
      }
      case 404 => throw new SpecialExceptions.NotFoundException
      case _   => throw new SpecialExceptions.UnexpectedException(s"Eventor returned unexpected error (${log.statusCode}): ${log.statusText}")
    }
  }

  private def makeRacesUrl(eventorId: String, pInfo: ParticipationInfo): String = {
    val from = pInfo.unknownPeriod.getStart
    val to = pInfo.unknownPeriod.getEnd
    val fetchFrom = YMD_FORMAT.print(from)
    val fetchTo = YMD_FORMAT.print(to)
    (s"${EVENTOR_BASE_URL}/api/starts/person?personId=$eventorId&fromDate=$fetchFrom&toDate=$fetchTo")
  }

  private def ensureRaceListShouldBeFetched(eventorId: String, pInfo: ParticipationInfo): Future[Boolean] = {
    logDAO.fetchedRecently(eventorId, RUNNER_RACES).map { logged =>
      val askedBefore = logged.exists(log => log.to.getOrElse(new DateTime) == pInfo.unknownPeriod.getEnd)
      if (askedBefore) throw new SpecialExceptions.FetchedRecentlyException
      else false
    }
  }
  

  private def findParticipationInfo(eventorId: String, now: DateTime): Future[ParticipationInfo] = {
    participationsDAO.get(eventorId).map { ps =>
      val participatedInRaceIds = participationService.findRaces(ps)
      val unknownPeriod = participationService.findUnknownPeriod(ps, now)
      logger.info(s"$eventorId -- has participated in ${participatedInRaceIds.size} races that I know of. Don't know about $unknownPeriod")
      ParticipationInfo(unknownPeriod, participatedInRaceIds)
    }
  }
}