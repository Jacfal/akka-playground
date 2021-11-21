package faulttolerance

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorLogging, ActorSystem, OneForOneStrategy, Props}
import akka.pattern.{BackoffOpts, BackoffSupervisor}

import java.io.File
import scala.concurrent.duration._
import scala.io.Source
import scala.language.postfixOps

object BackoffSupervisorPattern extends App {
  case object ReadFile
  class FileBasedPersistentActor extends Actor with ActorLogging {
    var dataSource: Option[Source] = None

    override def preStart(): Unit = log.info("Persistent actor starting...")
    override def postStop(): Unit = log.info("Persistent actor has stopped")
    override def preRestart(reason: Throwable, message: Option[Any]): Unit =
      log.warning("Persistent actor restarting")


    override def receive: Receive = {
      case ReadFile =>
        dataSource match {
          case None =>
            val read = Source.fromFile(new File("src/main/resources/testfiles/important_invalid.txt"))
            log.info(s"I've just read data from file: ${read.getLines().toList}")
            dataSource = Some(read)
          case _ =>
            log.debug("Datasource already exists")
        }
    }
  }

  val system = ActorSystem("backoffSupervisorDemo")
//  val simpleActor = system.actorOf(Props[FileBasedPersistentActor])
//  simpleActor ! ReadFile

  val simpleSupervisorProps = BackoffSupervisor.props(
    BackoffOpts.onFailure(
      Props[FileBasedPersistentActor],
      "simpleBackoffActor",
      3 seconds, // then 6s, 12s, 24s, ...
      20 seconds,
      0.2
    )
  )

  // val simpleBackoffSupervisor = system.actorOf(simpleSupervisorProps, "simpleSupervisor")
  /*
    simpleSupervisor
      - child called simpleBackoffActor (props of type FileBasedPersistentActor)
      - supervision strategy is the default one (restarting in everything)
        - first attempt after 3 sec
        - next attempt is 2x the previous attempt
   */
  // simpleBackoffSupervisor ! ReadFile

  val stopSupervisorProps = BackoffSupervisor.props(
    BackoffOpts.onStop(
      Props[FileBasedPersistentActor],
      "stopBackoffActor",
      3 seconds,
      30 seconds,
      0.2
    ).withSupervisorStrategy(
      OneForOneStrategy() {
        case _ => Stop
      }
    )
  )

//  val simpleStopSupervisor = system.actorOf(stopSupervisorProps, "simpleStopSupervisor")
//  simpleStopSupervisor ! ReadFile

  class EagerFileBasedPersistenceActor extends FileBasedPersistentActor {
    override def preStart(): Unit = {
      log.info("Eager actor starting...")
      dataSource = Some(Source.fromFile(new File("src/main/resources/testfiles/important_invalid.txt")))
    }
  }

  val eagerActor = system.actorOf(Props[EagerFileBasedPersistenceActor])
  // actor initialization exception => STOP

  val repeatedSupervisorProps = BackoffSupervisor.props(
    BackoffOpts.onStop(
      Props[EagerFileBasedPersistenceActor],
      "eagerStop",
      1 second,
      30 seconds,
      0.2
     )
  )
  val repeatedSupervisor = system.actorOf(repeatedSupervisorProps, "eagerSupervisor")
  /*
   eagerSupervisor
    - child eagerActor
      - will die on start with ActorInitException
      - trigger the supervision strategy eagerSupervisor => STOP eagerActor
    - backoff will kick in after 1, 2, 4, 8,... seconds
   */
}
