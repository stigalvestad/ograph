package dao

import scala.concurrent.Future

import com.google.inject.ImplementedBy

import models.Models.RaceResult
import models.Models.raceResultFormat
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.BSONFormats.BSONDocumentFormat
import play.modules.reactivemongo.json.ImplicitBSONHandlers
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api.Cursor
import reactivemongo.bson.{BSONString, BSONDocument, BSONLong}
import reactivemongo.bson.Producer.nameValue2Producer
import reactivemongo.core.commands.FindAndModify
import reactivemongo.core.commands.Update
import org.slf4j.{ LoggerFactory, Logger }

@ImplementedBy(classOf[RaceResultDAOImpl])
trait RaceResultDAO {
  def upsert(raceResult: RaceResult): Future[Int]
  def get(eventorId: String): Future[List[RaceResult]]
}

class RaceResultDAOImpl extends RaceResultDAO {

  private val RACE_RESULTS = "race_results"
  private val DO_UPSERT = true
  def db = ReactiveMongoPlugin.db
  def raceResultCollection: JSONCollection = db.collection[JSONCollection](RACE_RESULTS)
  val logger: Logger = LoggerFactory.getLogger(classOf[RaceResultDAOImpl])

  def upsert(raceResult: RaceResult): Future[Int] = {

    val queryDoc = BSONDocument("resultId" -> BSONString(raceResult.resultId))

    val raceResultJsObject = raceResultFormat.writes(raceResult)
    val updateDoc = ImplicitBSONHandlers.JsObjectWriter.write(raceResultJsObject)
    val modifyUpdate = Update(updateDoc, false)

    val findAndModify = FindAndModify(RACE_RESULTS, queryDoc, modifyUpdate, DO_UPSERT)
    val result = ReactiveMongoPlugin.db.command(findAndModify)
    result.map(docOpt => docOpt.map(d => Json.toJson(d))).map {
      _ match {
        case Some(oldObj) => 1
        case None         => 0
      }
    }
  }

  def get(eventorId: String): Future[List[RaceResult]] = {
    logger.info("Get results for runner " + eventorId)
    val cursor: Cursor[RaceResult] = raceResultCollection.
      find(Json.obj("runner.eventorId" -> eventorId)).
      sort(Json.obj("created" -> -1)).
      cursor[RaceResult]

    cursor.collect[List]()
  }

}