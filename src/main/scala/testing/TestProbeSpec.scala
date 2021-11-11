package testing

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

class TestProbeSpec extends TestKit(ActorSystem("TestProbSpec"))
  with ImplicitSender
  with AnyWordSpecLike
  with BeforeAndAfterAll {
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import TestProbeSpec._

  "A master actor" should {
    "register worker" in {
      val master = system.actorOf(Props[Master])
      val worker = TestProbe("worker")

      master ! Register(worker.ref)
      expectMsg(RegistrationAck)
    }

    "send the work to the worker actor" in {
      val master = system.actorOf(Props[Master])
      val worker = TestProbe("worker")

      master ! Register(worker.ref)
      expectMsg(RegistrationAck)

      val workloadString = "Hello, testing kit!"
      master ! Work(workloadString)

      // the interaction between the master and the slave actor
      worker.expectMsg(WorkerWork(workloadString, testActor))
      worker.reply(WorkCompleted(3, testActor))

      expectMsg(Report(3)) // test actor receive Report(3)
    }
  }
}

object TestProbeSpec {
  // word counting actor hierarchy master-slave
  //  - master sends the slave the piece of work
  //  - slave process the work and replies to master
  //  - master aggregates the result
  // master sends the total count to the original sender

  case class Register(workerRef: ActorRef)
  case class Work(test: String)
  case class WorkerWork(text: String, originalSender: ActorRef)
  case class WorkCompleted(totalWordCount: Int, originalSender: ActorRef)
  case class Report(totalCount: Int)

  case object RegistrationAck

  class Master extends Actor {
    override def receive: Receive = {
      case Register(workerRef) =>
        sender ! RegistrationAck
        context.become(online(workerRef, 0))
      case _ =>
    }

    def online(workerRef: ActorRef, totalWordCount: Int): Receive = {
      case Work(text) => workerRef ! WorkerWork(text, sender)
      case WorkCompleted(count, originalWorkerRef) =>
        val newTotalWordCount = totalWordCount + count
        originalWorkerRef ! Report(newTotalWordCount)
        context.become(online(workerRef, newTotalWordCount))
    }
  }
}
