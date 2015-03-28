package services

import javax.inject.Singleton

import com.google.inject.ImplementedBy
import models.Models.{Organisation, Race, RaceClass, RaceResult, Runner}
import org.joda.time.{DateTime, Interval}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.Future
import scala.xml.{Elem, Node}

@ImplementedBy(classOf[XmlParserServiceImpl])
trait XmlParserService {
  def parseOrganisations(xml: Elem): Future[List[Organisation]]
  def parseEntries(xml: Elem): Future[List[Runner]]
  def parseResults(xml: Elem): Future[List[RaceResult]]
  def parseRaces(xml: Elem): Future[List[Race]]
}

@Singleton
class XmlParserServiceImpl extends XmlParserService {

  //TODO use a different execution context?
  implicit val context = scala.concurrent.ExecutionContext.Implicits.global
  private final val logger: Logger = LoggerFactory.getLogger(classOf[XmlParserServiceImpl])

  def parseEntries(xml: Elem): Future[List[Runner]] = {
    Future {
      val res = for {
        personNode <- (xml \\ "Person")
      } yield parseRunner(personNode)
      res.toList.distinct
    }
  }

  def parseRaces(xml: Elem): Future[List[Race]] = {

    val randomNum = Math.random() * 100;
    Future {
      logger.debug(s"Starting: Xml parsing of races #$randomNum")
      val races = for {
        raceNode <- (xml \ "StartList")
        if (acceptRace(raceNode))
        race <- parseRace(raceNode)
      } yield race

      logger.debug(s"Finished: Xml parsing of races #$randomNum")
      races.toList
    }
  }

  def parseOrganisations(xml: Elem): Future[List[Organisation]] = {
    Future {
      (xml \ "Organisation")
        .map(parseOrganisation _)
        .toList
    }
  }

  def parseResults(xml: Elem): Future[List[RaceResult]] = {
    val randomNum = Math.random() * 100;
    logger.debug(s"Xml parsing of race results requested #$randomNum")
    Future {
      logger.debug(s"Starting: Xml parsing of race results #$randomNum")
      val raceResults = for {
        raceNode <- (xml \\ "ResultList")
        if (acceptRace(raceNode))
        race <- parseRace(raceNode)
        classResultNode <- (raceNode \ "ClassResult")
      } yield parseRaceResult(classResultNode, race)

      logger.debug(s"Finished: Xml parsing of race results #$randomNum")
      raceResults.flatten.toList
    }
  }

  private def acceptRace(raceNode: Node): Boolean = {
    val eventForm = ((raceNode \ "Event").head \ "@eventForm").head.text
    eventForm equals "IndSingleDay"
  }

  private def parseRaceResult(classResultNode: Node, race: Race): List[RaceResult] = {
    val raceClass = parseRaceClass(classResultNode, race)
    val res = (classResultNode \ "PersonResult").map {
      personResultNode =>
        val runner = parseRunner(personResultNode)
        val organisation = parseOrganisation((personResultNode \ "Organisation").head)
        val rank = (personResultNode \\ "ResultPosition").headOption.map { _.text.toInt }
        val raceStatus = ((personResultNode \\ "CompetitorStatus").head \ "@value").head.text
        val duration = parseTime(personResultNode)
        val resultId = (personResultNode \\ "ResultId").head.text
        RaceResult(resultId, runner, organisation, raceClass, duration, raceStatus, rank, None)
    }
    res.toList
  }

  private def parseTime(personResultNode: Node): Option[Long] = {
    parseTimeUsingEventorTime(personResultNode) match {
      case Some(t) => Some(t)
      case None =>
        // fallback to calculate time using start and finish time if available
        val startTime = parseDateTime((personResultNode \ "Result").head, "StartTime")
        val finishTime = parseDateTime((personResultNode \ "Result").head, "FinishTime")
        if (!startTime.isDefined || !finishTime.isDefined || startTime.get.isAfter(finishTime.get)) None
        else Some(new Interval(startTime.get, finishTime.get).toDurationMillis())
    }
  }

  private def parseTimeUsingEventorTime(personResultNode: Node): Option[Long] = {
    ((personResultNode \ "Result").head \ "Time").headOption match {
      case Some(time) => {
        val minAndSecs = time.text.split(":")
        if (minAndSecs.length != 2) throw new RuntimeException("Unknown time format: " + time.text)
        Some((minAndSecs(0).toLong * 60 + minAndSecs(1).toLong) * 1000)
      }
      case None => None
    }
  }

  private def parseRunner(personResultNode: Node): Runner = {
    val firstName = (personResultNode \\ "Given").head.text
    val lastName = (personResultNode \\ "Family").head.text
    val personEventorId = (personResultNode \\ "PersonId").headOption.map { _.text }
    try {
      val personBirthMonth = (personResultNode \\ "BirthDate").headOption.map { node =>
        val dateAsString = (node \ "Date").head.text
        DateTime.parse(dateAsString + "T00:00:00") // this will throw if date is invalid
        DateTime.parse(dateAsString.substring(0, 8) + "15T00:00:00") // dont store the day, always set to 15th that month
      }
      Runner(personEventorId, firstName, lastName, personBirthMonth)
    } catch {
      case e: Exception => Runner(personEventorId, firstName, lastName, None)
    }
  }

  private def parseRace(raceNode: Node): List[Race] = {
    val eventNode = (raceNode \ "Event").head
    val raceName = (eventNode \ "Name").head.text
    val eventId = (eventNode \ "EventId").head.text
    val classificationId = (eventNode \ "EventClassificationId").head.text.toInt
    val statusId = (eventNode \ "EventStatusId").head.text.toInt
    val url = (eventNode \ "WebURL").headOption.map { _.text }

    val startDate = parseDateTime(eventNode, "StartDate")

    Race(eventId, raceName, startDate.get, classificationId, statusId, url) :: Nil
  }

  def parseDateTime(parentNode: Node, dateName: String): Option[DateTime] = {
    (parentNode \ dateName).headOption match {
      case Some(dateTimeNode) => {
        val dateTimeDateMaybe = (dateTimeNode \ "Date").headOption
        dateTimeDateMaybe match {
          case Some(dtDate) => {
            val clock = (dateTimeNode \ "Clock").head.text
            Some(DateTime.parse(dtDate.text + "T" + clock))
          }
          case None => None
        }
      }
      case None => None
    }

  }

  private def parseRaceClass(classResultNode: Node, race: Race): RaceClass = {
    val res = (classResultNode \ "EventClass").map { eventClassNode =>
      val eventClassId = (eventClassNode \ "EventClassId").head.text
      val className = (eventClassNode \ "Name").head.text
      val classRaceInfoNode = (eventClassNode \ "ClassRaceInfo").head
      val nofStarts = (classRaceInfoNode \ "@noOfStarts").headOption.map(_.text.toInt)
      val distance = (classRaceInfoNode \ "CourseLength").headOption.map {
        value => parseCourseLength(value.text) / 1000.0
      }
      RaceClass(eventClassId, race, className, nofStarts, if (distance == Some(0)) None else distance)
    }
    res.head
  }

  private def parseCourseLength(courseLengthString: String): Double = {
    val firstPart = courseLengthString.split(" ")(0)
    if (firstPart.trim().isEmpty()) 0
    else firstPart.toDouble
  }

  private def parseOrganisation(orgNode: Node): Organisation = {
    val name = (orgNode \ "Name").head.text
    val eventorId = (orgNode \ "OrganisationId").headOption.map { _.text }
    Organisation(eventorId, name)
  }
}

