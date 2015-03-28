package services

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import dao.{RunnerDAO, RaceResultDAO, LogDAO}
import play.api.libs.json.Json
import services.FetcherRaceResultActor.FetchRaceResult
import services.FetcherCoordinatorActor._
import services.Throttler.EventorIsUnavailable

class FetcherCoordinatorActor(eventorFetcher: EventorFetcher, logDAO: LogDAO, parserService: XmlParserService,
                              resultDAO: RaceResultDAO, runnerDAO: RunnerDAO,
                              resultEnhancer: ResultEnhancerService) extends Actor with ActorLogging {

  var fetchersStatus: Map[ActorRef, FetchStatus] = Map()
  var myParent: Option[ActorRef] = None

  override def receive: Receive = {
    case EventorIsUnavailable =>
      stopMyselfEventorUnavailable
    case GetStatus =>
      log.info(s"GetStatus msg arrived")
      sender ! CurrentStatus(fetchersStatus.values.toList)
    case FetchThese(eventorItems) =>
      myParent = Option(sender())
      log.warning(s"Asked to coordinate fetching these: ${eventorItems.map(_.id + ",")}")
      eventorItems.map(eventorItem => {
        val fetcher = context.actorOf(FetcherRaceResultActor.props(eventorItem,
          eventorFetcher, logDAO, parserService, resultDAO, runnerDAO, resultEnhancer), s"fetcher_rr_${eventorItem.id}")
        fetchersStatus += (fetcher -> FetchStatus(eventorItem, "PENDING", ""))
        fetcher ! FetchRaceResult
      })
    case FetchedOk(eventorItem) =>
      log.debug(s"\t\t${eventorItem.id} OK")
      updateStatus(eventorItem, "OK", "")
      if (hasFinished) {
        log.debug(s"\t\tAll finished")
        stopMyself()
      }
    case FetchFailed(eventorItem, msg) =>
      updateStatus(eventorItem, "FAILED", msg)
      log.debug(s"\t\t${eventorItem.id} FAILED: $msg")
      if (hasFinished){
        log.debug(s"\t\tAll finished")
        stopMyself()
      }
  }

  override def postRestart(reason: Throwable) = log warning s"----------- Restarted -----------"

  def stopMyselfEventorUnavailable: Unit = stopMyself(true)

  def stopMyself(eventorIsUnavailable: Boolean = false): Unit = {
    context stop self
    log debug s"Stopped Coordinator"
    myParent.get ! CurrentStatus(fetchersStatus.values.toList, eventorIsUnavailable)
  }

  def hasFinished: Boolean = {
    fetchersStatus.foldLeft(true)((acc, entry) => acc && entry._2.status != "PENDING")
  }

  def updateStatus(eventorItem: EventorItem, theStatus: String, msg: String): Unit = {
    val correctEntry = fetchersStatus.find(entry => entry._2.eventorItem == eventorItem).get
    fetchersStatus += (correctEntry._1 -> FetchStatus(eventorItem, theStatus, msg))
  }

}

object FetcherCoordinatorActor {
  def props(eventorFetcher: EventorFetcher, logDAO: LogDAO, parserService: XmlParserService,
            resultDAO: RaceResultDAO, runnerDAO: RunnerDAO,
            resultEnhancer: ResultEnhancerService): Props =
    Props(new FetcherCoordinatorActor(eventorFetcher, logDAO, parserService, resultDAO, runnerDAO, resultEnhancer))
  case class FetchThese(resourceList: List[EventorItem])
  case object GetStatus
  case class EventorItem(url: String, id: String)
  case class FetchedOk(eventorItem: EventorItem)
  case class FetchFailed(eventorItem: EventorItem, msg: String)
  case class FetchStatus(eventorItem: EventorItem, status: String, msg: String)
  case class CurrentStatus(fetchStatus: List[FetchStatus], eventorIsUnavailable: Boolean = false)

  implicit val eventorItemFormat = Json.format[EventorItem]
  implicit val fetchStatusFormat = Json.format[FetchStatus]
  implicit val finishedFormat = Json.format[CurrentStatus]
}
