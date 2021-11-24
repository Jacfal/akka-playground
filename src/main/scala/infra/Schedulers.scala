package infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Cancellable, PoisonPill, Props}

import scala.concurrent.duration._
import scala.language.postfixOps

object Schedulers extends App {
  val system = ActorSystem("TimersSchedulers")
  val simpleActor = system.actorOf(Props[SimpleActor])

  import system.dispatcher // has also execution context

  system.log.info("Scheduling reminder for simple actor")
  system.scheduler.scheduleOnce(1 second) {
    simpleActor ! "Hello"
  }

  val routine = system.scheduler.scheduleWithFixedDelay(1 second, 2 seconds) {
    () => simpleActor ! "Simple heartbeat"
  }

  system.scheduler.scheduleOnce(10 seconds) {
    simpleActor ! "cancelling"
    routine.cancel()
  }

  val shortLivedActor = system.actorOf(Props[ShortLivedActor])
  Thread.sleep(400)
  shortLivedActor ! "Are you here?"
  Thread.sleep(600)
  shortLivedActor ! "Are you still being here?"
  Thread.sleep(2000)
  shortLivedActor ! "Oh, no..."
}

class SimpleActor extends Actor with ActorLogging {
  override def receive: Receive = {
    case message => log.info(message.toString)
  }
}

class ShortLivedActor extends Actor with ActorLogging {
  def getCountDown: Cancellable = {
    context.system.scheduler.scheduleOnce(1 second) {
      log.warning("Time is up...")
      self ! PoisonPill
    }(context.dispatcher)
  }

  override def receive: Receive = withCountDown(getCountDown)
  def withCountDown(countDown: Cancellable): Receive = {
    case message =>
      countDown.cancel()
      log.info(s"Actor received message -> ${message.toString}")
      context.become(withCountDown(getCountDown))
  }
}

