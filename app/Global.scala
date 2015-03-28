import com.google.inject.{AbstractModule, Guice}
import play.api.libs.concurrent.Akka
import play.api.{Application, GlobalSettings, Logger}
import services.Throttler
import services.Throttler.Initialize

/**
 * Set up the Guice injector and provide the mechanism for return objects from the dependency graph.
 */
object Global extends GlobalSettings {

  val injector = Guice.createInjector(new AbstractModule {
    protected def configure() {
      // This explicit binding is not required when using @ImplementedBy
//      bind(classOf[WSClient]).to(classOf[WS.client])
    }
  })

  /**
   * Controllers must be resolved through the application context. There is a special method of GlobalSettings
   * that we can override to resolve a given controller. This resolution is required by the Play router.
   */
  override def getControllerInstance[A](controllerClass: Class[A]): A = injector.getInstance(controllerClass)

  override def onStart(app: Application) {
    Logger.info("Application has started")
    val throttlerActor = Akka.system(app).actorOf(Throttler.props, "throttler")
    throttlerActor ! Initialize
  }

  override def onStop(app: Application) {
    Logger.info("Application shutdown...")
  }
//  override def onHandlerNotFound(request: RequestHeader) = {
//    Redirect(controllers.routes.AppController.index())
//  }
}
