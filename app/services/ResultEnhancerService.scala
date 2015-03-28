package services

import javax.inject.Singleton

import com.google.inject.ImplementedBy
import models.Models.{RaceClass, RaceResult}

@ImplementedBy(classOf[ResultEnhancerServiceImpl])
trait ResultEnhancerService {
  def enhanceResults(results: List[RaceResult]): List[RaceResult]
}

@Singleton
class ResultEnhancerServiceImpl extends ResultEnhancerService {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def enhanceResults(results: List[RaceResult]): List[RaceResult] = {

    // enhance: merge duplicates
    val resultsWithoutDuplicates: List[RaceResult] = mergeDuplicates(results)

    // enhance: set nofStarts where missing
    val resultsByRaceClassWithNofStarts = resultsWithoutDuplicates.groupBy(_.raceClass).flatMap { resultsByRaceClass =>
      if (resultsByRaceClass._1.nofStarts.isEmpty){
        val estimatedNofStarts = Some(resultsByRaceClass._2.size)
        resultsByRaceClass._2.map{r =>
          r.copy(raceClass = r.raceClass.copy(nofStarts = estimatedNofStarts))
        }
      }
      else resultsByRaceClass._2
    }.toList

    // enhance: calculate time behind winner for each runner
    resultsByRaceClassWithNofStarts.groupBy(_.raceClass).flatMap { resultsByRaceClass =>
      val resultsWithRank: List[RaceResult] = resultsByRaceClass._2.filter(r => r.rank.isDefined && r.raceStatus == "OK")
      if (resultsWithRank.isEmpty){
        resultsByRaceClass._2
      }
      else {
        val winner = resultsWithRank.minBy { _.rank }
        resultsByRaceClass._2.map { r =>
          val timeBehind = getTimeBehind(winner, r)
          r.copy(timeBehind = timeBehind)
        }
      }
    }.toList.sortBy( r => (r.raceClass.race.name, r.raceClass.name, r.rank, r.runner.lastName) )
  }

  private def mergeDuplicates(results: List[RaceResult]): List[RaceResult] = {
    val resultsByPersonAndClass = results.groupBy(rr => rr.runner.firstName + rr.runner.lastName + rr.raceClass)
    val resultsWithoutDuplicates: List[RaceResult] = resultsByPersonAndClass.flatMap { duplicates =>
      if (duplicates._2.size > 1) {
        val merged = duplicates._2.reduce { (rr1, rr2) =>
          val runner = if (rr1.runner.eventorId.isDefined) rr1.runner else rr2.runner
          val duration = if (rr1.duration.isDefined) rr1.duration else rr2.duration
          val rank = if (rr1.rank.isDefined) rr1.rank else rr2.rank
          val raceStatus = if (rr1.raceStatus == "OK") rr1.raceStatus else rr2.raceStatus
          RaceResult(rr1.resultId, runner, rr1.organisation, rr1.raceClass, duration, raceStatus, rank, None)
        }
        List(merged)
      }
      else duplicates._2.toList
    }.toList
    resultsWithoutDuplicates
  }

  private def getTimeBehind(winner: RaceResult, runnerResult: RaceResult): Option[Long] = {
    if (runnerResult.raceStatus.equals("OK")
      && runnerResult.duration.isDefined
      && winner.duration.isDefined
      && winner.rank.getOrElse(0) == 1){

      val timeBehind = runnerResult.duration.get - winner.duration.get
      if (timeBehind >= 0) Some(timeBehind)
      else None
    }
    else None
  }
}