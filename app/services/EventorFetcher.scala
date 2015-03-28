package services

import javax.inject.Singleton

import com.google.inject.ImplementedBy
import org.slf4j.{Logger, LoggerFactory}
import play.api.Play.current
import play.api.libs.ws.{WS, WSResponse}
import services.OgraphConstants.{EVENTOR_API_KEY, EVENTOR_TIMEOUT_SEC}

import scala.concurrent.Future

@ImplementedBy(classOf[EventorFetcherSimple])
trait EventorFetcher {
  def getResource(url: String): Future[WSResponse]
}

@Singleton
class EventorFetcherSimple extends EventorFetcher {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global
  private final val logger: Logger = LoggerFactory.getLogger(classOf[EventorFetcherSimple])

  def getResource(url: String): Future[WSResponse] = {
    logger.info(s"GET $url with timeout=$EVENTOR_TIMEOUT_SEC sec")
    WS.url(url).withHeaders("ApiKey" -> EVENTOR_API_KEY).withRequestTimeout(EVENTOR_TIMEOUT_SEC * 1000).get()
  }
  

}