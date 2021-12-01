package patterns

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Stash}

object StashDemo extends App {
  /*
    Resource actor
    - open => it can receive read/write requests to the resource
    - otherwise, it will postpone all read/write requests until the state is open

    Resource actor is closed (at init)
      - Open => switch to the open state
      - Read, write messages are POSTPONED

    Resource actor is open
       read, write are handled
       - close => switch to the closed state
   */

  val system = ActorSystem("StashDemo")
  val resourceActor = system.actorOf(Props[ResourceActor])

  resourceActor ! Read
  resourceActor ! Open
  resourceActor ! Open
  resourceActor ! Write("Hello")
  resourceActor ! Close
  resourceActor ! Write("From")
  resourceActor ! Read
  resourceActor ! Read
  resourceActor ! Write("the other side")
  resourceActor ! Read

  // Things to be careful about
  //  * potential memory bounds on stash
  //  * potential mailbox bounds when unstash
  //  * no stashing twice!
  // * the stash trait overrides preRestart so must be mixed-in last
}

case object Open
case object Close
case object Read
case class Write(data: String)

class ResourceActor extends Actor with ActorLogging with Stash {
  private var secretData: String = ""

  override def receive: Receive = closed
  def closed: Receive = {
    case Open =>
      log.info("Opening resource")
      unstashAll()
      context.become(open)
    case message =>
      log.info(s"Stashing message ${message.toString}")
      stash()
  }

  def open: Receive = {
    case Read =>
      // do some computation
      log.info(s"I have read it! $secretData")
    case Write(data) =>
      log.info(s"I'm writing secret data => $data")
      secretData = data
    case Close =>
      log.info("Closing resource")
      unstashAll()
      context.become(closed)
    case message =>
      log.info(s"Stashing message ${message.toString}")
      stash()
  }
}
