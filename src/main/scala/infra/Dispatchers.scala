package infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

object Dispatchers extends App {
  // dispatchers controls how messages being send and handled
  val system = ActorSystem("DispatcherDemo")

  // method 1 - programmatic
  val actors = for(i <- 1 to 10) yield {
    system.actorOf(Props[CounterActor].withDispatcher("myDispatcher"), s"counter_$i")
  }
  val r = new Random()
  for (i <- 1 to 100) {
    actors(r.nextInt(10)) ! i
  }

  // method 2 - from config
  val someActor = system.actorOf(Props[CounterActor], "someActor")

  // dispatchers implement execution context trait
  val dbActor = system.actorOf(Props[DBActor], "dbActor")
  dbActor ! "Want to save something to DB"
}

class CounterActor extends Actor with ActorLogging {
  var count = 0
  override def receive: Receive = {
    case message =>
      count += 1
      log.info(s"[$count] ${message.toString}")
  }
}

class DBActor extends Actor with ActorLogging {
  //implicit val ec: ExecutionContext = context.dispatcher
  implicit val ec: ExecutionContext = context.system.dispatchers.lookup("myDispatcher")

  // when you are using blocking op at your code, you can use separate dispatcher for handling it

  override def receive: Receive = {
    case message => Future { // futures inside actors are not recommended!
      // wait on a resource
      Thread.sleep(1000)
      log.info(s"Success: $message")
    }
  }
}
