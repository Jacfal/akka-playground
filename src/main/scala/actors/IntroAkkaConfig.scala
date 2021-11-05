package actors

import com.typesafe.config.ConfigFactory
import akka.actor._

object IntroAkkaConfig extends App {
  // 1 - inline configuration
  val configString = 
    """
    | akka {
    |  loglevel = "INFO"
    | }
    """.stripMargin

  val config = ConfigFactory.parseString(configString)
  val system = ActorSystem("configDemo", ConfigFactory.load(config))
  val simpleLogging = system.actorOf(Props[SimpleLogging], "simpleLogger")
  simpleLogging ! "Hello, logger!"

  // 2 - default configuration
  val defaultConfigFileSystem = ActorSystem("configDemo2") 
  val anotherSimpleLogging = defaultConfigFileSystem.actorOf(Props[SimpleLogging], "simpleLogger2")
  anotherSimpleLogging ! "Hello, with default config!"

  // 3 - separate config in the same file
  val specialConfig = ConfigFactory.load().getConfig("mySpecialConfig")
  val specialConfigSystem = ActorSystem("SpecialConfigDemo", specialConfig)
  val specialConfigActor = specialConfigSystem.actorOf(Props[SimpleLogging], "simpleLogger3")
  specialConfigActor ! "Hello, with multiple config!"

  // 4 - separate config in the another file
  val separateConfig = ConfigFactory.load("bonus/secret.conf")
  println(s"Separate config ${separateConfig.getString("akka.loglevel")}")
}

class SimpleLogging extends Actor with ActorLogging {
  override def receive: Receive = {
    case message => 
      log.debug(s"${self.path} Incoming debug message => $message")
      log.info(s"${self.path} Incoming info message => $message")
      log.warning(s"${self.path} Incoming warn message => $message")
      log.error(s"${self.path} Incoming err message => $message")
  }
}
