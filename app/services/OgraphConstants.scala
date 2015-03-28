package services

object OgraphConstants {

  val EVENTOR_BASE_URL = play.Play.application.configuration.getString("eventor.base.url")
  val EVENTOR_API_KEY = play.Play.application.configuration.getString("eventor.api.key")
  val EVENTOR_TIMEOUT_SEC: Int = play.Play.application.configuration.getInt("eventor.timeout.sec")
  val COORDINATOR_TIMEOUT_SEC: Long = play.Play.application.configuration.getLong("coordinator.timeout.sec")
}
