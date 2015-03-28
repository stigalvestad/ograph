package services

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.google.inject.ImplementedBy

import javax.inject.Singleton
import models.Models.Runner

@ImplementedBy(classOf[RunnerSearchServiceImpl])
trait RunnerSearchService {
  def filterRunners(query: String, runners: List[Runner]): List[Runner]
}

@Singleton
class RunnerSearchServiceImpl extends RunnerSearchService {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[RunnerSearchServiceImpl])

  def filterRunners(query: String, runners: List[Runner]): List[Runner] = {

    val trimmedQuery = query.trim().toLowerCase()
    if (trimmedQuery.split(" ").length > 1) {
      searchUsingFullName(runners, trimmedQuery)
    } else searchFirstAndLastNameIndividually(runners, trimmedQuery)
  }

  private def searchFirstAndLastNameIndividually(runners: List[Runner], trimmedQuery: String): List[Runner] = {
    
    def getFirstName(r: Runner) = r.firstName.toLowerCase()
    def getLastName(r: Runner) = r.lastName.toLowerCase()
    
    def exactMatches(r: Runner, q: String, runnerToString: Runner => String): Boolean = {
      runnerToString(r).equals(trimmedQuery)
    }

    val exactMatchesFirstName = runners.filter (exactMatches(_, trimmedQuery, getFirstName))
    val exactMatchesLastName = runners.filter (exactMatches(_, trimmedQuery, getLastName))
    if (!exactMatchesFirstName.isEmpty || !exactMatchesLastName.isEmpty) return exactMatchesFirstName ++ exactMatchesLastName

    val closeMatchesFirstName = findCloseMatches(runners, trimmedQuery, getFirstName)
    val closeMatchesLastName = findCloseMatches(runners, trimmedQuery, getLastName)
    (closeMatchesFirstName ++ closeMatchesLastName).sortBy(_._1).map(_._2)
  }

  private def findCloseMatches(runners: List[Runner], trimmedQuery: String, runnerMapFunc: Runner => String): List[(Int, Runner)] = {
    runners.map { r =>
      val name = runnerMapFunc(r)
      val l = Levenshtein.distance(name, trimmedQuery.toLowerCase())
      (l, r)
    }
      .filter { lAndR =>
        val treshold = trimmedQuery.length() / 3
        lAndR._1 <= treshold
      }
      .sortBy(_._1)

  }

  private def searchUsingFullName(runners: List[Runner], trimmedQuery: String): List[Runner] = {
    val exactMatches = runners.filter { r =>
      val fullName = getFullName(r)
      fullName.equals(trimmedQuery)
    }
    if (!exactMatches.isEmpty) return exactMatches
    val closeMatchesFullName = findCloseMatches(runners, trimmedQuery, getFullName)
    if (!closeMatchesFullName.isEmpty) return closeMatchesFullName.map(_._2)
    List()
  }

  private def getFullName(runner: Runner) = {
    val fullName = runner.firstName + " " + runner.lastName
    fullName.toLowerCase()
  }

}

