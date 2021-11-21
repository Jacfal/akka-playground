package faulttolerance

import akka.actor.SupervisorStrategy.{Escalate, Restart, Resume, Stop}
import akka.actor.{Actor, ActorRef, ActorSystem, OneForOneStrategy, Props, Terminated}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

class SupervisionSpec extends TestKit(ActorSystem("supervisionSpec"))
  with ImplicitSender
  with AnyWordSpecLike
  with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import SupervisionSpec._

  "A supervisor" should {
    "react properly on occurred situations" in {
      val supervisor = system.actorOf(Props[Supervisor], "supervisor")
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      child ! "I love akka"
      child ! Report
      expectMsg(3)

      child ! "This is seriously long message and I hope, that I'll see some exception at the end of this test" // should resume
      child ! Report
      expectMsg(3)

      child ! "" // should restart
      child ! Report
      expectMsg(0)

      watch(child)
      child ! 2397 // should escalate
      val terminatedMessage = expectMsgType[Terminated]
      assert(terminatedMessage.actor == child)

//      watch(child)
//      child ! "error message" // should stop
//      val terminatedMessage = expectMsgType[Terminated]
//      assert(terminatedMessage.actor == child)
    }
  }
}

object SupervisionSpec {
  object Report

  class Supervisor extends Actor {
    override val supervisorStrategy: OneForOneStrategy = OneForOneStrategy() {
      case _: NullPointerException => Restart
      case _: IllegalArgumentException => Stop
      case _: RuntimeException => Resume
      case _: Exception => Escalate
    }

    override def receive: Receive = {
      case props: Props =>
        val childRef = context.actorOf(props)
        sender ! childRef
    }
  }

  class FussyWordCounter extends Actor {
    var words = 0

    override def receive: Receive = {
      case Report =>
        sender ! words
      case "" =>
        throw new NullPointerException("sentence is empty")
      case sentence: String =>
        if (sentence.length > 20)
          new RuntimeException("sentence is too big")
        else if (!Character.isUpperCase(sentence(0)))
          throw new IllegalArgumentException("Sentence must start with uppercase")
        else
          words += sentence.split(" ").length
      case _ =>
        throw new Exception("Can receive only strings or report command")
    }
  }
}
