package faulttolerance

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Kill, PoisonPill, Props, Terminated}

object StartingStoppingActors extends App {
  import Parent._
  val system = ActorSystem("StoppingActorsDemo")
  val parent = system.actorOf(Props[Parent], "parent")
  parent ! StartChild("james")
  val child = system.actorSelection("/user/parent/james")
  child ! "Hello!"
  parent ! StopChild("james")

  //(1 to 50).foreach(_ => child ! "Are you living?") // actors don't stop immediately... they need some time

  // stopping app using special message
  val anotherChild = system.actorOf(Props[Child])
  anotherChild ! "Hey..."
  anotherChild ! PoisonPill // something like sigterm
  anotherChild ! "...Joe!"

  val justAnotherChild = system.actorOf(Props[Child])
  justAnotherChild ! "Are you ready..."
  justAnotherChild ! Kill // something like sigkill
  justAnotherChild ! "...to party?"

  // watcher
  val watcher = system.actorOf(Props[Watcher], "watcher")
  watcher ! StartChild("underWatch")
  val watched = system.actorSelection("/user/watcher/underWatch")
  watched ! "I see you...!"
  watched ! PoisonPill
}

class Parent extends Actor with ActorLogging {
  import Parent._

  override def receive: Receive = withChildren(Map.empty)

  def withChildren(children: Map[String, ActorRef]): Receive = {
    case StartChild(name) =>
      log.info(s"Starting child $name")
      context.become(withChildren(children + (name -> context.actorOf(Props[Child], name))))
    case StopChild(name) =>
      log.warning(s"Stopping child $name")
      children.get(name)
        .foreach(childRef => context.stop(childRef))
    case Stop =>
      log.info("Stopping myself...")
      context.stop(self)
    case message =>
      log.info(s"Received ${message.toString}")
  }
}

object Parent {
  case class StartChild(name: String)
  case class StopChild(name: String)
  case object Stop // stop itself
}

class Child extends Actor with ActorLogging {
  override def receive: Receive = {
    case message => log.info(message.toString)
  }
}

class Watcher extends Actor with ActorLogging {
  import Parent._

  override def receive: Receive = {
    case StartChild(name) =>
      val child = context.actorOf(Props[Child], name)
      log.info(s"Starting and watching child $name")
      context.watch(child)
    case Terminated(ref) => // when actor under watch dies, this message will occurs
      log.info(s"The ref $ref has been stopped")
  }
}
