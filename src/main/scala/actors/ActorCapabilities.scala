package actors

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props

case class SpecialMessage(sMessage: String)
case class SayHiTo(actorRef: ActorRef)
case class ForwardedMessage(message: String, actorRef: ActorRef)

/*
* quick notes:
*   -> every actor typ derives from Actor (receive: Receive function must be implemented) 
*   -> type Receive = PartialFunction[Any, Unit]
*/

object ActorCapabilities extends App {
  val actorSystem = ActorSystem("actorCapabilities")
  val simpleActor = actorSystem.actorOf(SimpleActor.param, "simpleActor")
  
  simpleActor ! "Hello, Actor!"
  
  // messages can be of any type
  // * messages must be immutable
  // * messages must be serializable
  // * in practice -> use case classes and case objects

  simpleActor ! 43
  simpleActor ! SpecialMessage("abracadabra")

  // actors have information about their context and about themselves
  // context.self == 'this' in OOP

  simpleActor ! SpecialMessage("avada kadevra")

  // actors communication
  val alice = actorSystem.actorOf(SimpleActor.param, "aliceActor")
  val bob = actorSystem.actorOf(SimpleActor.param, "bobActor")

  alice ! SayHiTo(bob)
  alice ! "Hi!" // dead letters example

  // forwarding messages between actors
  alice ! ForwardedMessage("Super secret message", bob)
}

class SimpleActor(someParam: String) extends Actor {
  println(s"Hello from simple actor with $someParam, everything is about context ${context.self}")

  override def receive: PartialFunction[Any, Unit] = {
    case "Hi!" => context.sender() ! "Hello, there!"
    case message: String => println(s"I have received $message")
    case number: Int => println(s"I have received the number $number")
    case SpecialMessage(message) if message.startsWith("avada") => self ! message // send message to yourself
    case SpecialMessage(message) => println(s"Some magic message here ${message}")
    case SayHiTo(ref) => ref ! s"Hi!"
    case ForwardedMessage(msg, ref) => ref forward msg
  }
}

object SimpleActor {
  def param = Props(new SimpleActor("lorem ipsum"))
}
