package infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.dispatch.{PriorityGenerator, UnboundedPriorityMailbox}
import com.typesafe.config.Config

object Mailboxes extends App {
  class SupportTicketPriorityMailbox(settings: ActorSystem.Settings, config: Config)
    extends UnboundedPriorityMailbox(
      PriorityGenerator {
        // takes partial function of any kind, must return int (priority)
        case message: String if message.startsWith("P[0]") => 0 // highest priority
        case message: String if message.startsWith("P[1]") => 1
        case message: String if message.startsWith("P[2]") => 2
        case message: String if message.startsWith("P[3]") => 3
        case _ => 4 // lowest priority
      })

  val system = ActorSystem("mailboxDemo")

  // custom priority mailbox (P0 -> most important)
  // step 1. mailbox definition
  // step 2. make it known in the config
  // step 3. attach dispatcher to an actor

  val supportTicketLogger = system.actorOf(Props[JustAnotherSimpleActor].withDispatcher("support-ticket-dispatcher"))

  supportTicketLogger ! "P[3] prioritize"
  supportTicketLogger ! "P[2] from"
  supportTicketLogger ! "P[0] Hello"
  supportTicketLogger ! "P[1] world"
}

class JustAnotherSimpleActor extends Actor with ActorLogging {
  override def receive: Receive = {
    case message =>
      log.info(s"Incoming! ${message.toString}")
  }
}
