package dao

import scala.concurrent.Future

import com.google.inject.ImplementedBy

import models.Models.Runner
import models.Models.runnerFormat
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.BSONFormats.BSONDocumentFormat
import play.modules.reactivemongo.json.ImplicitBSONHandlers
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api.Cursor
import reactivemongo.bson.BSONDocument
import reactivemongo.bson.BSONLong
import reactivemongo.bson.BSONString
import reactivemongo.bson.Producer.nameValue2Producer
import reactivemongo.core.commands.FindAndModify
import reactivemongo.core.commands.Update

@ImplementedBy(classOf[RunnerDAOImpl])
trait RunnerDAO {
  def upsert(runner: Runner): Future[Int]
  def get(eventorId: Long): Future[Option[Runner]]
  def getAll(): Future[List[Runner]]
}

class RunnerDAOImpl extends RunnerDAO {

  def db = ReactiveMongoPlugin.db
  private val RUNNERS = "runners"
  private val DO_UPSERT = true
  def RunnerCollection: JSONCollection = db.collection[JSONCollection](RUNNERS)

  def upsert(runner: Runner): Future[Int] = {

    val queryDoc = runner.eventorId match {
      case Some(eventorId) => BSONDocument(
          "eventorId" -> BSONString(eventorId))
      case None => BSONDocument(
          "firstName" -> BSONString(runner.firstName), 
          "lastName" -> BSONString(runner.lastName))
    }

    val runnerJsObject = runnerFormat.writes(runner)
    val updateDoc = ImplicitBSONHandlers.JsObjectWriter.write(runnerJsObject)
    val modifyUpdate = Update(updateDoc, false)

    val findAndModify = FindAndModify(RUNNERS, queryDoc, modifyUpdate, DO_UPSERT)
    val result = ReactiveMongoPlugin.db.command(findAndModify)
    result.map(docOpt => docOpt.map(d => Json.toJson(d))).map {
      _ match {
        case Some(oldObj) => 1
        case None         => 0
      }
    }
  }

  def get(eventorId: Long): Future[Option[Runner]] = {
    val cursor: Cursor[Runner] = RunnerCollection.
      find(Json.obj("runner.eventorId" -> eventorId)).
      sort(Json.obj("created" -> -1)).
      cursor[Runner]

    cursor.headOption
  }

  def getAll(): Future[List[Runner]] = {
    val cursor: Cursor[Runner] = RunnerCollection.
      find(Json.obj()).
      sort(Json.obj("created" -> -1)).
      cursor[Runner]

    cursor.collect[List]()
  }

}