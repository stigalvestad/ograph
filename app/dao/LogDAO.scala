package dao

import javax.inject.Singleton

import com.google.inject.ImplementedBy
import models.Models.{RetrievalLog, logFormat}
import org.joda.time.DateTime
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api.Cursor

import scala.concurrent.Future

@ImplementedBy(classOf[LogDAOImpl])
trait LogDAO {
  def insert(retrievalLog: RetrievalLog): Future[Int]
  def fetchedRecently(eventorId: String, resourceType: String): Future[List[RetrievalLog]]
  def fetchedRecently(resourceType: String, now: DateTime): Future[List[RetrievalLog]]
  def fetched(resourceType: String): Future[List[RetrievalLog]]
  def markLogOk(log: RetrievalLog): Future[RetrievalLog]
}

@Singleton
class LogDAOImpl extends LogDAO {

  def db = ReactiveMongoPlugin.db
  def logCollection: JSONCollection = db.collection[JSONCollection]("retrieval_log")
  private def RECENT_TIME = 1000 * 60
  
  def markLogOk(log: RetrievalLog): Future[RetrievalLog] = {
    val newLog = RetrievalLog(log.timeStamp, log.statusCode, log.statusText, log.resourceType, log.eventorId, log.url,
        log.from, log.to, Some(true))
    insert(newLog).map { _ => log } //TODO why do I return the old log?
  }

  def insert(retLog: RetrievalLog): Future[Int] = {
    logCollection.insert(retLog).map { lastError =>
      lastError.ok match {
        case true  => lastError.updated
        case false => throw new RuntimeException("Problem with mongodb: " + lastError.errMsg)
      }

    }
  }
  
  def fetchedRecently(resourceType: String, now: DateTime): Future[List[RetrievalLog]] = {
    val cursor: Cursor[RetrievalLog] = logCollection.
      find(Json.obj(
          "resourceType" -> resourceType, 
          "timeStamp" -> Json.obj("$gt" -> now.minusMillis(RECENT_TIME)))).
      sort(Json.obj("timeStamp" -> -1)).
      cursor[RetrievalLog]

    cursor.collect[List]()
  }


  def fetchedRecently(eventorId: String, resourceType: String): Future[List[RetrievalLog]] = {
    val cursor: Cursor[RetrievalLog] = logCollection.
      find(Json.obj("eventorId" -> eventorId, "resourceType" -> resourceType)).
      sort(Json.obj("timeStamp" -> -1)).
      cursor[RetrievalLog]

    cursor.collect[List]()
  }

  def fetched(resourceType: String): Future[List[RetrievalLog]] = {
    val cursor: Cursor[RetrievalLog] = logCollection.
      find(Json.obj("resourceType" -> resourceType)).
      sort(Json.obj("timeStamp" -> -1)).
      cursor[RetrievalLog]

    cursor.collect[List]()
  }

}