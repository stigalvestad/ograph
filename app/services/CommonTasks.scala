package services

import models.Models.RetrievalLog
import dao.LogDAO
import scala.concurrent.Future
import java.io.File
import play.api.libs.Files
import play.api.libs.ws.WSResponse

class CommonTasks {
  
  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def fetchedAndProcessedOk(log: RetrievalLog): Boolean = log.statusCode == 200 && log.processedOk == Some(true)
  
  def logEventorRequest(r: WSResponse, log: RetrievalLog, logDAO: LogDAO): Future[RetrievalLog] = {
//    Files.writeFile(new File(s"/home/stig/code/ograph_project/downloaded_xml/${log.resourceType}-${log.eventorId}.xml"), r.body)
    logDAO.insert(log).map { i => log }
  }
  
}