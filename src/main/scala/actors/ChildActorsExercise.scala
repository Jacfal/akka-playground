package actors

import akka.actor._

object ChildActorsExercise extends App {
  import WordCounterMaster._
  // Distributed word counting
  
  /*
    create WordCounterMaster
    send Initialize(10) to wordCounterMaster
    send "Hello, Akka" to wordCOunterMaster
      word counter master send WordCountTask("...") to one of its children
        children replies with WordCounterReply(3) to the master
      master replies with 3 to the sender
  */

  val system = ActorSystem("childActorsExercise")
  val testActor = system.actorOf(Props[TestingActor], "testActor")
  testActor ! ("init", 5)
  /* 1 */ testActor ! "Hello, world!"
  /* 2 */ testActor ! "Greeting my Akka!"
  /* 3 */ testActor ! """
  | One ring to rule them all, 
  | one ring to find them, 
  | One ring to bring them all, 
  | and in the darkness bind them; 
  | In the Land of Mordor where the shadows lie.
  """.stripMargin
  /* 4 */ testActor ! "This is end of all hope"
  /* 5 */ testActor ! "Lorem ipsum, ipsum lorem"
  /* 6 */ testActor ! "ich bin ein berliner!"
  /* 7 */ testActor ! """"
    | Shows like BoJack Horseman, 
    | Fleabag and Veep show how modern TV comedies have embraced pain and grief,
    |  writes Jennifer Keishin Armstrong.
    """.stripMargin
}

class WordCounterMaster extends Actor with ActorLogging {
  import WordCounterMaster._
  
  override def receive: Receive = {
    case Initialize(nChildren) => 
      log.info(s"Creating $nChildren workers")
      val workersRefs = for {
        n <- 1 to nChildren
      } yield(context.actorOf(Props[WordCounterWorker], s"worker-$n"))
      context.become(withWorkers(workersRefs, 0, Map.empty))
  }

  def withWorkers(workers: Seq[ActorRef], currentTaskId: Int, requestMap: Map[Int, ActorRef]): Receive = {
    case WordCountReply(id, text, count) => 
      val originalSender = requestMap(id)
      originalSender ! count
      context.become(withWorkers(workers, currentTaskId, requestMap - id))
    case text: String =>
      val newRequestMap = requestMap + (currentTaskId -> sender()) // sender = originalSender
      workers.head forward WordCountTask(currentTaskId, text)
      context.become(withWorkers(workers.tail :+ workers.head, currentTaskId + 1, newRequestMap)) // round-robin
  }
}

object WordCounterMaster {
  case class Initialize(nChildren: Int)
  case class WordCountTask(id: Int, text: String)
  case class WordCountReply(id: Int, message: String, count: Int)
}

class WordCounterWorker extends Actor with ActorLogging {
  import WordCounterMaster._

  override def receive: Receive = {
    case WordCountTask(id, text) => 
      log.debug(s"${self.path} Processing task...")
      val count = text.split(" ").length
      context.parent ! WordCountReply(id, text, count)
  }

}

class TestingActor extends Actor {
  import WordCounterMaster._
  val wordCounterMaster = context.actorOf(Props[WordCounterMaster], "wordCounterMaster")

  override def receive: Receive = {
    case ("init", numOf: Int) => 
      wordCounterMaster ! Initialize(numOf)
    case text: String => wordCounterMaster ! text
    case count: Int => 
      println(s"Receive reply... $count")
  }

}
