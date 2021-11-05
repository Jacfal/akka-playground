package actors

import akka.actor._
import akka.event.Logging

object ActorLoggingDemo extends App {
  val system = ActorSystem("actorLogging")
  val explicitLogger = system.actorOf(Props[SimpleActorWithExplicitLogger])
  val actorLogger = system.actorOf(Props[ActorWithLogging])

  explicitLogger ! "Hey!"
  actorLogger ! "Another Hey!"
}

class SimpleActorWithExplicitLogger extends Actor {
  val logger = Logging(context.system, this)

  override def receive: Receive = {
    case message => 
      logger.info(s"Info message, $message")
  }
}

class ActorWithLogging extends Actor with ActorLogging {
  override def receive: Receive = {
    case message => 
      log.warning(s"Warn! $message")
  }


}
