package services

import models.Models.{Organisation, Race, RaceClass, RaceResult, Runner}
import org.joda.time.DateTime
import org.junit.runner._
import org.specs2.mutable._
import org.specs2.runner._

@RunWith(classOf[JUnitRunner])
class ResultEnhancerServiceSpec extends Specification {

  private val STIG_ALVESTAD = Runner(Some("1"), "Stig", "Alvestad", None)
  private val STIG_ALVESTAD_WITHOUT_EV_ID = Runner(None, "Stig", "Alvestad", None)
  private val OLAV_LUNDANES = Runner(Some("2"), "Olav", "Lundanes", None)
  private val IVAR_LUNDANES = Runner(Some("3"), "Ivar", "Lundanes", None)
  private val JOAR_LUNDAMO = Runner(Some("4"), "Joar", "Lundamo", None)
  private val CLUB_A = Organisation(Some("1"), "Club A")
  private val RACE_NC_LONG = Race("8", "NC Long", new DateTime(), 1, 1, Some(""))
  private val CLASS_H21 = RaceClass("9", RACE_NC_LONG, "H21", Some(21), Some(6.5))
  private val CLASS_H35 = RaceClass("10", RACE_NC_LONG, "H35", None, Some(5.5))
  
  private val enhancer = new ResultEnhancerServiceImpl()

  "Result enhancer service > enhance results" should {
    "when no results, return empty" in {
      val results = List()
      val enhanced = enhancer.enhanceResults(results)
      enhanced.size must beEqualTo(0)
    }
    
    "when one result, ranked 1, return one with timeBehind = 0" in {
      val results = List(createResultH21(STIG_ALVESTAD, Some(1000 * 60 * 33.toLong), "OK", Some(1)))
      
      val enhanced = enhancer.enhanceResults(results)
      enhanced.size must beEqualTo(1)
      enhanced(0).timeBehind must beEqualTo(Some(0))
    }
    
    "when #1 and #2, #2 should show time behind winner" in {
      val results = List(
          createResultH21(STIG_ALVESTAD, Some(1000 * 60 * 33.toLong), "OK", Some(1)), 
          createResultH21(IVAR_LUNDANES, Some(1000 * 60 * 34.toLong), "OK", Some(2)))
      
      val enhanced = enhancer.enhanceResults(results)
      enhanced.size must beEqualTo(2)
      enhanced(0).timeBehind must beEqualTo(Some(0))
      enhanced(1).timeBehind must beEqualTo(Some(60 * 1000))
    }

    "when #1 and #2 and #3, #3 has better time than winner, don't calc time behind for #3." in {
      val results = List(
        createResultH21(STIG_ALVESTAD, Some(1000 * 60 * 33.toLong), "OK", Some(1)),
        createResultH21(IVAR_LUNDANES, Some(1000 * 60 * 34.toLong), "OK", Some(2)),
        createResultH21(OLAV_LUNDANES, Some(1000 * 60 * 31.toLong), "OK", Some(3)))

      val enhanced = enhancer.enhanceResults(results)
      enhanced.size must beEqualTo(3)
      enhanced(0).timeBehind must beEqualTo(Some(0))
      enhanced(1).timeBehind must beEqualTo(Some(60 * 1000))
      enhanced(2).timeBehind must beEqualTo(None)
    }

    "when #1 is missing, don't calc time behind for other runners." in {
      val results = List(
        createResultH21(IVAR_LUNDANES, Some(1000 * 60 * 34.toLong), "OK", Some(2)),
        createResultH21(OLAV_LUNDANES, Some(1000 * 60 * 31.toLong), "OK", Some(3)))

      val enhanced = enhancer.enhanceResults(results)
      enhanced.size must beEqualTo(2)
      enhanced(0).timeBehind must beEqualTo(None)
      enhanced(1).timeBehind must beEqualTo(None)
    }
    
    "when runner did not have status OK, do not calculate time behind" in {
      val results = List(
          createResultH21(STIG_ALVESTAD, Some(1000 * 60 * 33.toLong), "OK", Some(1)), 
          createResultH21(IVAR_LUNDANES, Some(1000 * 60 * 34.toLong), "DidNotFinish", Some(2)))
      
      val enhanced = enhancer.enhanceResults(results)
      enhanced.size must beEqualTo(2)
      enhanced(0).timeBehind must beEqualTo(Some(0))
      enhanced(1).timeBehind must beEqualTo(None)
    }

    "when no runners have status OK, do not calculate time behind" in {
      val results = List(
        createResultH21(STIG_ALVESTAD, Some(1000 * 60 * 33.toLong), "DidNotFinish", None),
        createResultH21(IVAR_LUNDANES, Some(1000 * 60 * 34.toLong), "DidNotFinish", None))

      val enhanced = enhancer.enhanceResults(results)
      enhanced.size must beEqualTo(2)
      enhanced(0) must beEqualTo(results(0))
      enhanced(1) must beEqualTo(results(1))
    }

    "when there are two results for the same runner, one disq and one ok, use the ok result and discard the other" in {
      val results = List(
        createResultH21(STIG_ALVESTAD_WITHOUT_EV_ID, Some(1000 * 60 * 33.toLong), "OK", Some(1)),
        createResultH21(IVAR_LUNDANES, Some(1000 * 60 * 34.toLong), "OK", Some(2)),
        createResultH21(STIG_ALVESTAD, Some(1000 * 60 * 33.toLong), "Cancelled", None))

      val enhanced = enhancer.enhanceResults(results)
      enhanced.size must beEqualTo(2)
      enhanced(0).runner must beEqualTo(STIG_ALVESTAD)
      enhanced(0).raceStatus must beEqualTo("OK")
      enhanced(0).duration must beEqualTo(Some(1000 * 60 * 33.toLong))
      enhanced(0).timeBehind must beEqualTo(Some(0))
      enhanced(1).runner must beEqualTo(IVAR_LUNDANES)
      enhanced(1).timeBehind must beEqualTo(Some(1000 * 60.toLong))
    }
    
    "when results from two different race classes, treat each race class separately" in {
      val results = List(
          createResultH21(STIG_ALVESTAD, Some(1000 * 60 * 33.toLong), "OK", Some(1)), 
          createResultH21(IVAR_LUNDANES, Some(1000 * 60 * 34.toLong), "OK", Some(2)),
          createResultH35(JOAR_LUNDAMO, Some(1000 * 60 * 32.toLong), "OK", Some(1)), 
          createResultH35(OLAV_LUNDANES, Some(1000 * 60 * 35.toLong), "OK", Some(3)))
      
      val enhanced = enhancer.enhanceResults(results)
      enhanced.size must beEqualTo(4)
      enhanced(0).timeBehind must beEqualTo(Some(0))
      enhanced(0).raceClass.nofStarts must beEqualTo(Some(21))
      enhanced(1).timeBehind must beEqualTo(Some(60 * 1000))
      enhanced(1).raceClass.nofStarts must beEqualTo(Some(21))
      enhanced(2).timeBehind must beEqualTo(Some(0))
      enhanced(2).raceClass.nofStarts must beEqualTo(Some(2))
      enhanced(3).timeBehind must beEqualTo(Some(3 * 60 * 1000))
      enhanced(3).raceClass.nofStarts must beEqualTo(Some(2))
    }
  }

  def createResultH21(runner: Runner, duration: Some[Long], status: String, rank: Option[Int]) = {
    RaceResult("9", runner, CLUB_A, CLASS_H21, duration, status, rank, None)
  }
  
  def createResultH35(runner: Runner, duration: Some[Long], status: String, rank: Some[Int]) = {
    RaceResult("9", runner, CLUB_A, CLASS_H35, duration, status, rank, None)
  }
}