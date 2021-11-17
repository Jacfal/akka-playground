package faulttolerance

import akka.actor.{Actor, ActorLogging, ActorSystem, PoisonPill, Props}

/**
  * Actor instance
  *  - has method
  *  - may have internal state
  *  Actor reference
  *  - created with actorOf
  *  - has mailbox and can receive messages
  *  - contains one actor instance
  *  - ! contains a UUID
  *  Actor path:
  *  - may or may not have an ActorRef inside
  */
object ActorLifecycle extends App {
  /*
    Actors lifecycle:
    - started       = create a new actorRef with UUID at a given path
    - suspended     = the actor ref will enqueue but NOT process more messages
    - resumed       = the actor ref will continue processing more messages
    - restarted     = suspend, swap the actor instance, resume -> !!! Internal state is destroyed on restart
    - stopped       = stopping frees the actor ref within a path (after stopping, another actor may be created at the same path)
   */
  val system = ActorSystem("lifecycleDemo")
  val parent = system.actorOf(Props[LifecycleActor], "parent")
  parent ! StartChild
  parent ! PoisonPill // stopping an actor stops all it's children too

  // restart test
  val supervisor = system.actorOf(Props[AnotherParent], "supervisor")
  supervisor ! FailChild
  supervisor ! CheckChild // log info will be printed, because supervision strategy
}

object StartChild

class LifecycleActor extends Actor with ActorLogging {
  override def preStart(): Unit = log.info("I am starting!")
  override def postStop(): Unit = log.info("I have stopped... :(")


  override def receive: Receive = {
    case StartChild =>
      context.actorOf(Props[LifecycleActor], "child")
  }
}

object Fail
object FailChild
object CheckChild
object Check

class AnotherParent extends Actor {
  private val child = context.actorOf(Props[AnotherChild], "supervisedChild")
  override def receive: Receive = {
    case FailChild => child ! Fail
    case CheckChild => child ! Check
  }
}

class AnotherChild extends Actor with ActorLogging {
  override def preStart(): Unit = log.info("supervised child started")
  override def postStop(): Unit = log.info("supervised child stopped")
  override def preRestart(reason: Throwable, message: Option[Any]): Unit =
    log.info(s"supervised child actor, pre-restarted because of ${reason.getMessage}")
  override def postRestart(reason: Throwable): Unit =
    log.info(s"supervised child actor, restarted because of ${reason.getMessage}")

  override def receive: Receive = {
    case Fail =>
      log.warning("Child will failing now!")
      throw new RuntimeException("Child failed...")
    case Check =>
      log.info("alive and kicking")
  }
}
