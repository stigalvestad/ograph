package services

import dao.{LogDAO, ParticipationDAO}
import helper.ObjectMother
import helper.ObjectMother._
import models.Models.{Race, Participation, RetrievalLog}
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.concurrent.AsyncAssertions
import org.scalatest.time.{Seconds, Span}
import org.specs2.mock.Mockito
import org.specs2.mock.mockito.ArgumentCapture
import org.specs2.mutable
import org.specs2.runner.JUnitRunner
import play.api.libs.ws.WSResponse

import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.xml.Elem

@RunWith(classOf[JUnitRunner])
class RacesServiceSpec extends mutable.Specification with AsyncAssertions with Mockito {

  private final val WAIT_TIME = Span(1.0, Seconds)

  val race10 = Race("10", "Race 10", _2014_2_6, 1, 2, None)
  val race8 = Race("8", "Race 8", _2013_7_6, 1, 2, None)
  val race9 = Race("9", "Race 9", _2014_1_10, 1, 2, None)

  val twoRaces: List[Race] = List(race8, race9)

  "find missing races" should {
    "when races have been fetched recently, return empty list" in  {
      val eventorFetcher = mock[EventorFetcher]
      val logDAO = mock[LogDAO]
      val parserService = mock[XmlParserService]
      val participationDAO = mock[ParticipationDAO]
      val participationService = new ParticipationServiceImpl()

      participationDAO.get("123") returns Future {
        List(Participation("123", _2011_1_1, _2014_1_10, twoRaces))
      }

      logDAO.fetched("Race") returns Future {
        List(
          RetrievalLog(new DateTime, 200, "OK", "Races", "8", "", None, None, Some(true)),
          RetrievalLog(new DateTime, 200, "OK", "Races", "9", "", None, None, Some(true)))
      }

      val today = _2014_2_6
      logDAO.fetchedRecently("123", "Runner_Races") returns Future {
        List(
          RetrievalLog(new DateTime, 200, "OK", "Races", "9", "", None, Some(today), Some(true)))
      }

      val service = new RacesService(eventorFetcher, logDAO, parserService, participationDAO, participationService)

      val w = new Waiter

      val res = service.findMissingRaces("123", today)

      res.map { ok =>
        w {
          ok must beEqualTo(List())
        }
        w.dismiss()
      } recover {
        case NonFatal(e) =>
          println("Something failed")
      }

      w.await(timeout(WAIT_TIME))
      res must not be null

    }

    "when races have been not been fetched recently, check period since last time" in  {
      val eventorFetcher = mock[EventorFetcher]
      val logDAO = mock[LogDAO]
      val parserService = mock[XmlParserService]
      val participationDAO = mock[ParticipationDAO]
      val participationService = new ParticipationServiceImpl()

      participationDAO.get("123") returns Future {
        List(Participation("123", _2011_1_1, _2014_1_10, twoRaces))
      }

      participationDAO.insert(any[Participation]) returns Future {2}

      logDAO.fetched("Race") returns Future {
        List(
          RetrievalLog(new DateTime, 200, "OK", "Races", "8", "", None, None, Some(true)),
          RetrievalLog(new DateTime, 200, "OK", "Races", "9", "", None, None, Some(true)))
      }

      logDAO.fetchedRecently("123", "Runner_Races") returns Future {
        List(
          RetrievalLog(new DateTime, 200, "OK", "Races", "9", "", None, Some(_2014_1_10), Some(true)))
      }

      logDAO.insert(any[RetrievalLog]) returns Future (1)

      logDAO.markLogOk(any[RetrievalLog]) returns Future(
        RetrievalLog(new DateTime, 200, "OK", "Runner_Races", "123",
          "https://eventor.orientering.no/api/starts/person?personId=123&fromDate=2013-12-21&toDate=2015-02-06",
          Some(_2013_12_21), Some(_2014_2_6), Some(true)))

      val xmlElem = mock[Elem]
      eventorFetcher.getResource(any[String]) returns Future {
        setupOkResponse(xmlElem)
      }

      val listOfOneNewAndOneExistingRace = List(race10, race9)

      parserService.parseRaces(xmlElem) returns Future(listOfOneNewAndOneExistingRace)

      val service = new RacesService(eventorFetcher, logDAO, parserService, participationDAO, participationService)

      // invoke method to test
      val res = service.findMissingRaces("123", _2014_2_6)

      val w = new Waiter
      res.map { ok =>
        w {
          val urlAskForRaces = "https://eventor.orientering.no/api/starts/person?personId=123&fromDate=2013-12-21&toDate=2014-02-06"

          // verify participation dao
          there was one(participationDAO).get("123")
          there was one(participationDAO).insert(Participation("123", _2013_12_21, _2014_2_6, List(race10, race9)))
          there was noMoreCallsTo(participationDAO)

          // verify eventor service
          there was one(eventorFetcher).getResource(urlAskForRaces)
          there was noMoreCallsTo(eventorFetcher)

          // verify eventor parser service
          there was one(parserService).parseRaces(xmlElem)
          there was noMoreCallsTo(parserService)

          // verify log interaction
          val logCaptor = new ArgumentCapture[RetrievalLog]

          there was one(logDAO).fetched("Race")
          there was one(logDAO).fetchedRecently("123", "Runner_Races")
          there was one(logDAO).insert(logCaptor)

          val capturedLog = logCaptor.value
          capturedLog.url mustEqual urlAskForRaces
          capturedLog.eventorId mustEqual 123
          capturedLog.from mustEqual Some(_2013_12_21)
          capturedLog.to mustEqual Some(_2014_2_6)
          capturedLog.statusCode mustEqual 200
          capturedLog.statusText mustEqual "OK"
          capturedLog.processedOk mustEqual None

          there was one(logDAO).markLogOk(capturedLog)
          there was noMoreCallsTo(logDAO)

          // verify result
          ok must beEqualTo(List(Race("10", "Race 10", _2014_2_6, 1, 2, None)))
        }
        w.dismiss()
      } recover {
        case NonFatal(e) =>
          println("!! Something failed\n")
      }

      w.await(timeout(WAIT_TIME))
      res must not be null

    }

  }

  private def setupOkResponse(xmlElem: Elem): WSResponse = {
    val wsResponse = mock[WSResponse]
    wsResponse.status returns 200
    wsResponse.statusText returns "OK"
    wsResponse.xml returns xmlElem
    wsResponse
  }
}
