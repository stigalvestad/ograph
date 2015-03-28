package dao

import javax.inject.Singleton
import com.google.inject.ImplementedBy
import models.Models.Participation
import models.Models.participationFormat
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.modules.reactivemongo.json.collection.JSONCollection
import scala.concurrent.Future
import reactivemongo.api.Cursor
import play.api.libs.json.Json

@ImplementedBy(classOf[ParticipationDAOImpl])
trait ParticipationDAO {
  def insert(participation: Participation): Future[Int]
  def get(runnerEventorId: String): Future[List[Participation]]
}

@Singleton
class ParticipationDAOImpl extends ParticipationDAO {

  def db = ReactiveMongoPlugin.db
  def pCollection: JSONCollection = db.collection[JSONCollection]("participation")
  
  def insert(participation: Participation): Future[Int] = {
    pCollection.insert(participation).map { lastError =>
      lastError.ok match {
        case true  => lastError.updated
        case false => throw new RuntimeException("Problem with mongodb: " + lastError.errMsg)
      }

    }
  }

  def get(runnerEventorId: String): Future[List[Participation]] = {
    val cursor: Cursor[Participation] = pCollection.
      find(Json.obj("runnerEventorId" -> runnerEventorId)).
      sort(Json.obj("timeStamp" -> -1)).
      cursor[Participation]

    cursor.collect[List]()
  }

}