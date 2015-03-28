package services

import models.Models.Runner
import org.junit.runner._
import org.specs2.mutable._
import org.specs2.runner._

@RunWith(classOf[JUnitRunner])
class RunnerSearchServiceSpec extends Specification {

  private val STIG_ALVESTAD = Runner(Some("1"), "Stig", "Alvestad", Option.empty)
  private val OLAV_LUNDANES = Runner(Some("2"), "Olav", "Lundanes", Option.empty)
  private val IVAR_LUNDANES = Runner(Some("3"), "Ivar", "Lundanes", Option.empty)
  private val JOAR_LUNDAMO = Runner(Some("4"), "Joar", "Lundamo", Option.empty)
  private val IVAR_ALVESTAD = Runner(Some("5"), "Ivar", "Alvestad", Option.empty)
  private val ARE_LUND = Runner(Some("6"), "Are", "Lund", Option.empty)
  private val ANE_LIND = Runner(Some("7"), "Ane", "Lind", Option.empty)
  private val runners = List(
      STIG_ALVESTAD, 
      OLAV_LUNDANES, 
      IVAR_LUNDANES,  
      JOAR_LUNDAMO, 
      IVAR_ALVESTAD, 
      ARE_LUND, 
      ANE_LIND)
  private val searchService = new RunnerSearchServiceImpl()

  "Runner search service > filter runners" should {
    "when exact match on full name, only show those" in {
      val res = searchService.filterRunners("Stig Alvestad", runners)
      res.size must beEqualTo(1)
      res(0) must beEqualTo(STIG_ALVESTAD)
    }
    
    "when no exact matches, use levenshtein, sort by levenshtein distance" in {
      val res = searchService.filterRunners("Are Lunde", runners)
      res.size must beEqualTo(2)
      res(0) must beEqualTo(ARE_LUND)
      res(1) must beEqualTo(ANE_LIND)
    }
    
    "when no matches using full name, but full match on last name, only show those" in {
      var res = searchService.filterRunners("Lundanes", runners)
      res.size must beEqualTo(2)
      res(0) must beEqualTo(OLAV_LUNDANES) 
      res(1) must beEqualTo(IVAR_LUNDANES) 
    }
    
    "when no matches using full name, but full match on first name, only show those" in {
      var res = searchService.filterRunners("Ivar ", runners)
      res.size must beEqualTo(2)
      res(0) must beEqualTo(IVAR_LUNDANES) 
      res(1) must beEqualTo(IVAR_ALVESTAD) 
    }
    
    "when no matches using full name, try last name" in {
      var res = searchService.filterRunners("Lundan", runners)
      res.size must beEqualTo(4)
      res(0) must beEqualTo(OLAV_LUNDANES)
      res(1) must beEqualTo(IVAR_LUNDANES)
      res(2) must beEqualTo(JOAR_LUNDAMO)
      res(3) must beEqualTo(ARE_LUND)
      
    }
    
        "when searching for lundanesh, get 2 results" in {
      var res = searchService.filterRunners("Lundanesh", runners)
      res.size must beEqualTo(2)
      res(0) must beEqualTo(OLAV_LUNDANES)
      res(1) must beEqualTo(IVAR_LUNDANES)
      
    }
  }


}