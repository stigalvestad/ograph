package services

import javax.inject.{ Singleton, Inject }
import com.google.inject.ImplementedBy
import scala.concurrent.Future
import models.Models.Participation
import org.joda.time.Interval
import org.joda.time.DateTime
import models.Models.Race

@ImplementedBy(classOf[ParticipationServiceImpl])
trait ParticipationService {
  def findRaces(participation: List[Participation]): List[Race]
  def findUnknownPeriod(participation: List[Participation], now: DateTime): Interval
  
}

@Singleton
class ParticipationServiceImpl extends ParticipationService {

  private final val _2011_1_1 = new DateTime(2011, 1, 1, 0, 0, 0, 0)

  def findRaces(participation: List[Participation]): List[Race] = {
    participation.map(p => p.races).flatMap(l => l).distinct    
  }
  
  def findUnknownPeriod(participation: List[Participation], now: DateTime): Interval = {
    val atStartOfDay = new DateTime(now.getYear, now.getMonthOfYear, now.getDayOfMonth, 0, 0, 0, 0)
    if (participation.isEmpty) new Interval(_2011_1_1, atStartOfDay)
    else {
      val mostRecentP = participation.maxBy(_.to.getMillis)
      new Interval(mostRecentP.to.minusDays(20), atStartOfDay)
    }
  }
  
}