package models

import org.joda.time.DateTime

object Models {
  import play.api.libs.json.Json

  // Generates Writes and Reads thanks to Json Macros
  implicit val logFormat = Json.format[RetrievalLog]
  implicit val orgFormat = Json.format[Organisation]
  implicit val runnerFormat = Json.format[Runner]
  implicit val raceFormat = Json.format[Race]
  implicit val participationFormat = Json.format[Participation]
  implicit val raceClassFormat = Json.format[RaceClass]
  implicit val raceResultFormat = Json.format[RaceResult]

  case class RetrievalLog(timeStamp: DateTime, statusCode: Int, statusText: String, resourceType: String, eventorId: String, url: String,
      from: Option[DateTime], to: Option[DateTime], processedOk: Option[Boolean])
      
  case class Organisation(eventorId: Option[String], name: String)

  case class Runner(eventorId: Option[String], firstName: String, lastName: String, birthMonth: Option[DateTime])
  
  case class Participation(runnerEventorId: String, from: DateTime, to: DateTime, races: List[Race])

  case class Race(eventorId: String, name: String, raceDate: DateTime, classificationId: Int, statusId: Int, url: Option[String])

  case class RaceClass(eventorId: String, race: Race, name: String, nofStarts: Option[Int], distance: Option[Double])

  case class RaceResult(resultId: String, runner: Runner, organisation: Organisation, raceClass: RaceClass,
                        duration: Option[Long], 
                        raceStatus: String, // OK Disqualified Cancelled 
                        rank: Option[Int], timeBehind: Option[Long])
                        
}