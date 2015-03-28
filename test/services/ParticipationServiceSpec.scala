package services

import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import org.joda.time.DateTime
import models.Models.Participation
import org.joda.time.Interval
import models.Models.Race

@RunWith(classOf[JUnitRunner])
class ParticipationServiceSpec extends Specification {

  private val service = new ParticipationServiceImpl

  val _2011_1_1 = new DateTime(2011, 1, 1, 0, 0, 0, 0)
  val _2014_1_10 = new DateTime(2014, 1, 10, 0, 0, 0, 0)
  val _2014_1_30 = new DateTime(2014, 1, 30, 0, 0, 0, 0)
  val _2014_2_11_MIDNIGHT = new DateTime(2014, 2, 11, 0, 0, 0, 0)
  
  val ccArendal = Race("486", "CRAFT-cup - Arendal", new DateTime(2011, 5, 29, 11, 0, 0, 0), 2, 9, Some("http://www.nmsprint2011.no"))
  val ccStavanger = Race("487", "CRAFT-cup - Stavanger", new DateTime(2012, 5, 29, 11, 0, 0, 0), 2, 9, None)
  val ccBergen = Race("487", "CRAFT-cup - Bergen", new DateTime(2013, 5, 29, 11, 0, 0, 0), 2, 9, None)

  "Participation Service > find race ids" should {
    "when no participations, return empty" in {
      val ps = List()
      val raceIds = service.findRaces(ps)
      raceIds.size must beEqualTo(0)
    }

    "when participations include duplicates, remove duplicates" in {
      val ps123 = Participation("123", new DateTime, new DateTime, List(ccArendal, ccStavanger))
      val ps345 = Participation("345", new DateTime, new DateTime, List(ccStavanger, ccBergen))
      val ps = List(ps123, ps345)

      val raceIds = service.findRaces(ps)

      raceIds.size must beEqualTo(3)
    }
  }

  "Participation Service > find unknown period" should {
    "when participations is empty, return full interval" in {

      val now = new DateTime(2014, 2, 11, 14, 34, 0, 0)

      val interval = service.findUnknownPeriod(List(), now)

      interval must beEqualTo(new Interval(_2011_1_1, _2014_2_11_MIDNIGHT))
    }

    "when participations has a period which ends at 2014-01-30, return a interval which starts 20 days before that" in {

      val now = new DateTime(2014, 2, 11, 14, 34, 0, 0)
      
      val interval = service.findUnknownPeriod(List(Participation("123", _2011_1_1, _2014_1_30, List())), now)

      interval must beEqualTo(new Interval(_2014_1_10, _2014_2_11_MIDNIGHT))
    }
  }
}