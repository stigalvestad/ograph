package services

import models.Models.{Organisation, Race, RaceClass, RaceResult, Runner}
import org.joda.time.DateTime
import org.junit.runner._
import org.scalatest.concurrent.AsyncAssertions
import org.scalatest.time.{Seconds, Span}
import org.specs2.mutable._
import org.specs2.runner._

import scala.xml.XML

@RunWith(classOf[JUnitRunner])
class XmlParserServiceSpec extends Specification with AsyncAssertions {

  private val TEST_ROOT = "./eventor/testData/"
  private val ORGANISATIONS_SMALL = TEST_ROOT + "20120922_organisations_small.xml"
  private val RESULTS_SMALL = TEST_ROOT + "20120922_resultListList_small.xml"
  private val ENTRIES_SMALL = TEST_ROOT + "20121104_entries_small.xml"
  private val RESULTS_BIG = TEST_ROOT + "20141122_nordbergResults.xml"
  private val RESULTS_LUCASEN = TEST_ROOT + "20141204_Lucasen.xml"
  private val RESULTS_DOUBLE_STATUS_KAAS = TEST_ROOT + "twoPersonResultsInSameRace.xml"
  private val RACES_STIG = TEST_ROOT + "20150104_stig_starts.xml"
  private val RESULTS_CC_ARENDAL = TEST_ROOT + "20150104_arendal_cc.xml"
  private val parserService = new XmlParserServiceImpl

  private final val WAIT_TIME = Span(1.0, Seconds)

  "The xml parser service > parse organisations" should {
    "find organisations correctly" in {

      val xml = XML.loadFile(ORGANISATIONS_SMALL)
      val orgs = parserService.parseOrganisations(xml)
      val w = new Waiter

      orgs.map { organisations =>
        w {
          organisations must have size (4)

          val org1 = organisations(0)
          org1.name must beEqualTo("IOF")
          org1.eventorId must beEqualTo(Some(1))

          val org2 = organisations(1)
          org2.name must beEqualTo("Norges Orienteringsforbund")
          org2.eventorId must beEqualTo(Some(2))

          val org3 = organisations(2)
          org3.name must beEqualTo("Finnmark")
          org3.eventorId must beEqualTo(Some(6))

          val org4 = organisations(3)
          org4.name must beEqualTo("Alta ØL")
          org4.eventorId must beEqualTo(Some(21))
        }
        w.dismiss()
      }

      w.await(timeout(WAIT_TIME))
      orgs must not be null
    }
  }

  "The xml parser service > parse races" should {
    "find races correctly" in {

      val xml = XML.loadFile(RACES_STIG)
      val races = parserService.parseRaces(xml)
      val w = new Waiter

      races.map { rs =>
        w {

          val race1 = Race("486", "CRAFT-cup - Arendal", new DateTime(2011, 5, 29, 11, 0, 0, 0), 2, 9, Some("http://www.nmsprint2011.no"))
          rs(0) must beEqualTo(race1)

          val race2 = Race("484", "NM Sprint 2011 - Finaler", new DateTime(2011, 5, 28, 15, 0, 0, 0), 1, 9, Some("http://www.nmsprint2011.no"))
          rs(1) must beEqualTo(race2)

          val race3 = Race("763", "NM Sprint 2011 - Kvalifisering", new DateTime(2011, 5, 28, 10, 0, 0, 0), 1, 9, Some("http://www.nmsprint2011.no"))
          rs(2) must beEqualTo(race3)

          val race4 = Race("257", "Vestlands mesterskap", new DateTime(2011, 8, 27, 16, 0, 0, 0), 3, 5, Some("http://www.hilorientering.no"))
          rs(3) must beEqualTo(race4)

          rs must have size (4)
        }
        w.dismiss()
      }

      w.await(timeout(WAIT_TIME))
      races must not be null
    }
  }

  "The xml parser service > parse result list from an entire race" should {
    "find results correctly for arendal cc" in {

      val xml = XML.loadFile(RESULTS_CC_ARENDAL)
      val results2 = parserService.parseResults(xml)
      val w = new Waiter

      results2.map { results =>

        w {
          results must have size (4)

          val ccArendal = Race("486", "CRAFT-cup - Arendal", new DateTime(2011, 5, 29, 11, 0, 0, 0), 2, 9, Some("http://www.nmsprint2011.no"))
          val classH21 = RaceClass("1689", ccArendal, "H21", Some(88), None)

          val nordberg = Runner(Some("555"), "Anders", "Nordberg", Some(new DateTime(1978, 2, 15, 0, 0, 0, 0)))
          val vaja = Organisation(Some("409"), "Vaajakosken Terä")

          results(0) must beEqualTo(RaceResult("24209", nordberg, vaja, classH21, Some(1855000), "OK", Some(1), None))

          val lundanes = Runner(Some("1043"), "Olav", "Lundanes", Some(new DateTime(1987, 11, 15, 0, 0, 0, 0)))
          val halden = Organisation(Some("101"), "Halden SK")

          results(1) must beEqualTo(RaceResult("24213", lundanes, halden, classH21, Some(1884000), "OK", Some(2), None))

          val classD19_20 = RaceClass("1686", ccArendal, "D19-20", Some(26), None)
          val rognstad = Runner(Some("1159"), "Audhild Bakken", "Rognstad", Some(new DateTime(1991, 2, 15, 0, 0, 0, 0)))

          val lillehammer = Organisation(Some("202"), "Lillehammer OK")

          results(2) must beEqualTo(RaceResult("23959", rognstad, lillehammer, classD19_20, Some(27 * 60 * 1000 + 40 * 1000), "OK", Some(1), None))

          val haverstad = Runner(Some("2050"), "Maren Jansson", "Haverstad", None)
          val konnerud = Organisation(Some("185"), "Konnerud IL")

          results(3) must beEqualTo(RaceResult("23955", haverstad, konnerud, classD19_20, None, "Inactive", None, None))
        }
        w.dismiss()

      }

      w.await(timeout(WAIT_TIME))
      results2 must not be null
    }
  }

  "The xml parser service > parse result list" should {
    "find results correctly" in {

      val xml = XML.loadFile(RESULTS_SMALL)
      val results2 = parserService.parseResults(xml)
      val w = new Waiter

      results2.map { results =>

        w {
          results must have size (8)

          val ccArendal = Race("486", "CRAFT-cup - Arendal", new DateTime(2011, 5, 29, 11, 0, 0, 0), 2, 9, Some("http://www.nmsprint2011.no"))
          val classH21 = RaceClass("1689", ccArendal, "H21", Some(88), None)

          val nordberg = Runner(Some("555"), "Anders", "Nordberg", Some(new DateTime(1978, 2, 15, 0, 0, 0, 0)))
          val vaja = Organisation(Some("409"), "Vaajakosken Tera")

          results(0) must beEqualTo(RaceResult("24209", nordberg, vaja, classH21, Some(1855000), "OK", Some(1), None))

          val lundanes = Runner(Some("1043"), "Olav", "Lundanes", Some(new DateTime(1987, 11, 15, 0, 0, 0, 0)))
          val halden = Organisation(Some("101"), "Halden SK")

          results(1) must beEqualTo(RaceResult("24213", lundanes, halden, classH21, Some(1884000), "OK", Some(2), None))

          val weltzien = Runner(Some("1831"), "Audun", "Weltzien", Some(new DateTime(1983, 9, 15, 0, 0, 0, 0)))
          val tyrving = Organisation(Some("163"), "IL Tyrving")

          results(2) must beEqualTo(RaceResult("24207", weltzien, tyrving, classH21, Some(1902000), "OK", Some(3), None))

          val stig = Runner(Some("3351"), "Stig", "Alvestad", Some(new DateTime(1981, 8, 15, 0, 0, 0, 0)))
          val stigWithoutBirthDate = Runner(Some("3351"), "Stig", "Alvestad", None)
          val ganddal = Organisation(Some("88"), "Ganddal IL")

          results(3) must beEqualTo(RaceResult("24195", stig, ganddal, classH21, Some(2107000), "OK", Some(12), None))

          // next race
          val nmSprintFinals = Race("484", "NM Sprint 2011 - Finaler", new DateTime(2011, 5, 28, 15, 0, 0, 0), 1, 9, Some("http://www.nmsprint2011.no"))
          val classH21_sprint = RaceClass("2530", nmSprintFinals, "H21 Finale", Some(30), None)

          val kvaal = Runner(Some("716"), "Øystein Kvaal", "Østerbø", Some(new DateTime(1981, 7, 15, 0, 0, 0, 0)))
          val wing = Organisation(Some("379"), "Wing OK")

          results(4) must beEqualTo(RaceResult("25504", kvaal, wing, classH21_sprint, Some(921000), "OK", Some(1), None))

          val kaas = Runner(Some("2892"), "Carl Waaler", "Kaas", Some(new DateTime(1982, 7, 15, 0, 0, 0, 0)))
          val bsk = Organisation(Some("32"), "Bækkelagets SK")

          results(5) must beEqualTo(RaceResult("25505", kaas, bsk, classH21_sprint, Some(935000), "OK", Some(2), None))

          val lucasen = Runner(Some("479"), "Håvard", "Lucasen", Some(new DateTime(1982, 7, 15, 0, 0, 0, 0)))
          val aas = Organisation(Some("987"), "Ås-UMB Orientering")

          results(6) must beEqualTo(RaceResult("25506", lucasen, aas, classH21_sprint, Some(946000), "OK", Some(3), None))
          results(7) must beEqualTo(RaceResult("25533", stigWithoutBirthDate, ganddal, classH21_sprint, Some(1041000), "Disqualified", None, None))
        }
        w.dismiss()

      }

      w.await(timeout(WAIT_TIME))
      results2 must not be null
    }

    "find Nordbergs results correctly" in {

      val xml = XML.loadFile(RESULTS_BIG)
      val results = parserService.parseResults(xml)
      val w = new Waiter

      results.map(res => {
        w { res must have size (35) }
        w.dismiss()
      })

      w.await(timeout(WAIT_TIME))
      results must not be null
    }

    "find Lucasen's results correctly" in {

      val xml = XML.loadFile(RESULTS_LUCASEN)
      val results = parserService.parseResults(xml)
      val w = new Waiter

      results.map { res =>
        w {
          res must have size (24)

          res(0).raceClass.race.name must beEqualTo("NM ultralang")
          res(1).raceClass.race.name must beEqualTo("NM ultralang")
          res(2).raceClass.race.name must beEqualTo("NM ultralang")
          res(3).raceClass.race.name must beEqualTo("NM ultralang")

          res(4).raceClass.race.name must beEqualTo("NM natt")
          res(5).raceClass.race.name must beEqualTo("NM natt")
          res(6).raceClass.race.name must beEqualTo("NM natt")
          res(7).raceClass.race.name must beEqualTo("NM natt")
          res(8).raceClass.race.name must beEqualTo("NM natt")

          res(9).raceClass.race.name must beEqualTo("O-idol og Norgescup")
          res(10).raceClass.race.name must beEqualTo("O-idol og Norgescup")
          res(11).raceClass.race.name must beEqualTo("O-idol og Norgescup")
          res(12).raceClass.race.name must beEqualTo("O-idol og Norgescup")

          res(13).raceClass.race.name must beEqualTo("NM-uka 2014 mellomdistanse kvalifisering")
          res(14).raceClass.race.name must beEqualTo("NM-uka 2014 mellomdistanse kvalifisering")
          res(15).raceClass.race.name must beEqualTo("NM-uka 2014 mellomdistanse kvalifisering")

          res(16).raceClass.race.name must beEqualTo("NM-uka 2014 mellomdistanse junior og senior")
          res(17).raceClass.race.name must beEqualTo("NM-uka 2014 mellomdistanse junior og senior")
          res(18).raceClass.race.name must beEqualTo("NM-uka 2014 mellomdistanse junior og senior")
          res(19).raceClass.race.name must beEqualTo("NM-uka 2014 mellomdistanse junior og senior")

          res(20).raceClass.race.name must beEqualTo("Blodslitet eliteklasser")
          res(21).raceClass.race.name must beEqualTo("Blodslitet eliteklasser")
          res(22).raceClass.race.name must beEqualTo("Blodslitet eliteklasser")
          res(23).raceClass.race.name must beEqualTo("Blodslitet eliteklasser")

          res(0).raceClass.distance must beEqualTo(Some(23.6))
          res(4).raceClass.distance must beEqualTo(Some(10.53))
          res(9).raceClass.distance must beEqualTo(Some(4.82))
          res(13).raceClass.distance must beEqualTo(None)
          res(16).raceClass.distance must beEqualTo(Some(5.37))
          res(20).raceClass.distance must beEqualTo(Some(25.275))
        }
        w.dismiss()

      } recover {
        case e: Any => failure("crashed because of " + e.getMessage)
      }

      w.await(timeout(WAIT_TIME))
      results must not be null
    }

    "when kaas has both disq and ok in the same race, include both, let the problem be fixed later" in {

      val xml = XML.loadFile(RESULTS_DOUBLE_STATUS_KAAS)
      val results = parserService.parseResults(xml)
      val w = new Waiter

      results.map { res =>
        w {
          res must have size (5)

          val nmNatt = Race("3334", "NM natt", new DateTime(2014, 9, 5, 0, 0, 0, 0), 1, 11, Some("http://www.nm2014.net/"))
          val classH21 = RaceClass("39473", nmNatt, "H 21-", Some(60), Some(10.53))

          val kaasOk = Runner(None, "Carl Godager", "Kaas", None)
          val kaasDisq = Runner(Some("2892"), "Carl Godager", "Kaas", Some(new DateTime(1982, 7, 15, 0, 0, 0, 0)))
          val bsk = Organisation(Some("32"), "Bækkelagets SK")

          res(0) must beEqualTo(RaceResult("671297", kaasOk, bsk, classH21, Some(82 * 1000 * 60 + 46 * 1000), "OK", Some(1), None))
          res(3) must beEqualTo(RaceResult("622283", kaasDisq, bsk, classH21, Some(82 * 1000 * 60 + 46 * 1000), "Cancelled", None, None))
        }
        w.dismiss()
      } recover {
        case e: Any => failure("crashed because of " + e.getMessage)
      }

      w.await(timeout(WAIT_TIME))
      results must not be null
    }


  }
  "Use of the xpath-like syntax" should {
    "find all elements with z and extract the text" in {

      val baz2 = <all><a><z x="1">arne</z></a><a><z x="2">joar</z></a><a><z x="3">brit</z></a></all>
      (baz2 \\ "z").map(_.text) must beEqualTo(List("arne", "joar", "brit"))
    }

    "find all elements with z and get their attributes" in {

      val baz = <a><z x="1"/><b><z x="2"/><c><z x="3"/></c><z x="4"/></b></a>
      val res = (baz \\ "z").map(_ \ "@x").mkString
      res must beEqualTo("1234")
    }
  }

  "The xml parser service > parse entry list" should {
    "find distinct entries correctly" in {

      val xml = XML.loadFile(ENTRIES_SMALL)
      val res = parserService.parseEntries(xml)
      val w = new Waiter

      res.map { results =>
        w {
          results must have size (3)

          val wennemo = Runner(Some("311"), "Oda", "Wennemo", Some(new DateTime(1992, 3, 15, 0, 0, 0, 0)))
          val kvaal = Runner(Some("716"), "Øystein Kvaal", "Østerbø", Some(new DateTime(1981, 7, 15, 0, 0, 0, 0)))
          val alfi = Runner(Some("3292"), "Alf Johan", "Lima", Some(new DateTime(1986, 10, 15, 0, 0, 0, 0)))

          results(0) must beEqualTo(wennemo)
          results(1) must beEqualTo(kvaal)
          results(2) must beEqualTo(alfi)
        }
        w.dismiss()
      }

      w.await(timeout(WAIT_TIME))
      res must not be null
    }
  }

}