package services

import java.lang.System.currentTimeMillis

import akka.actor._
import services.FetcherCoordinatorActor.EventorItem
import services.Throttler._

import scala.concurrent.duration._

class Throttler extends Actor with ActorLogging {

  import context._

  var fetchItems: Map[ActorRef, FetchItem] = Map()
  val REQUEST_LIMIT = 3
  val ACTIVE = "ACTIVE"
  val QUEUED = "QUEUED"

  def idle: Receive = {
    case Initialize =>
      log info s"Initialized."
      become(active)
    case _ =>
      log info s"Is idle, will not do anything. Try again later."
      sender ! EventorIsUnavailable
  }

  def active: Receive = {
    case EventorIsUnavailable =>
      log warning s"Eventor is unavailable! Cleaning fetch-items-queue and moving to idle state."
      system.scheduler.scheduleOnce(10 seconds, self, Initialize)
      fetchItems.map(entry => entry._1 ! EventorIsUnavailable)
      fetchItems = Map()
      become(idle)
    case CanFetch(eventorItem) =>
      if (canProceed){
        fetchItems += (sender -> FetchItem(ACTIVE, eventorItem, currentTimeMillis))
//        logStatus(s"\t\t+${eventorItem.id}")
        sender ! OkToFetch
      }
      else {
        fetchItems += (sender -> FetchItem(QUEUED, eventorItem, currentTimeMillis))
//        logStatus(s"\t\t|${eventorItem.id}")
      }
      watch(sender)

    case DoneFetching(eventorItem) =>
      fetchItems -= sender
//      logStatus(s"\t\t-${eventorItem.id} ")
      unwatch(sender)
      proceedWithNextItem

    case Terminated(fetcher) =>
//      logStatus(s"X $fetcher")
      fetchItems -= fetcher
      if (canProceed){
        proceedWithNextItem
      }
  }

  override def receive: Receive = idle

  def canProceed: Boolean = {
    getItemsWithStatus(ACTIVE).size < REQUEST_LIMIT
  }

  def logStatus(prefix: String): Unit = {
    val activeResources = getItemsWithStatus(ACTIVE).values.toList.sortBy(_.updated).map(_.eventorItem.id).mkString(",")
    val queuedResources = getItemsWithStatus(QUEUED).values.toList.sortBy(_.updated).map(_.eventorItem.id).mkString(",")
    log.info(s"$prefix\t#active: $activeResources #queued: $queuedResources")
  }

  private def proceedWithNextItem: Unit = {
    if (!getItemsWithStatus(QUEUED).isEmpty) {
      val itemWaitedLongest = getItemsWithStatus(QUEUED).minBy(item => item._2.updated)
      fetchItems += (itemWaitedLongest._1 -> FetchItem(ACTIVE, itemWaitedLongest._2.eventorItem, currentTimeMillis))
      logStatus(s"\t\t-${itemWaitedLongest._2.eventorItem.id} -> \t+${itemWaitedLongest._2.eventorItem.id}")
      itemWaitedLongest._1 ! OkToFetch
    }
  }

  def getItemsWithStatus(status: String) = {
    fetchItems.filter(item => item._2.status == status)
  }

  override def postStop() = log info s"Stopped"
}

object Throttler {
  val props = Props[Throttler]
  case object Initialize
  case object EventorIsUnavailable
  case class CanFetch(eventorItem: EventorItem)
  case object OkToFetch
  case class DoneFetching(eventorItem: EventorItem)
  case class FetchItem(status: String, eventorItem: EventorItem, updated: Long)
}
