package actors

import akka.actor._

object ChildActors extends App {
  import Parent._

  // actors can create other actors by invoking context actor of
  val system = ActorSystem("ParentChildDemo")
  val parent = system.actorOf(Props[Parent], "parent")
  parent ! CreateChild("firstChild")
  parent ! TellChild("Hello, kid!")

  // actor hierarchies
  // parent -> child hierarchy 
  //        -> child2 (for example)

  /**
    * Guardian actors (top-level)
    * - /system = system guardian
    * - /user = user-level guardian
    * - / = root guardian
    */

    // actor selection
    val childSelection = system.actorSelection("/user/parent/firstChild")
    childSelection ! "Hello, from selection!"

    // !!! never pass mutable actor state, or the 'this' ref to child actor
}

class Parent extends Actor {  
  import Parent._

  override def receive: Receive = {
    case CreateChild(name) => 
      println(s"${self.path}, creating child")
      // create new actor
      val childRef = context.actorOf(Props[Child], name)
      context.become(withChild(childRef))
  }

  def withChild(childRef: ActorRef): Receive = {
    case TellChild(message) => childRef forward message 
  }
}

object Parent {
  case class CreateChild(name: String)
  case class TellChild(message: String)
}

class Child extends Actor {
  override def receive: Receive = {
    case message: String => println(s"${self.path}, receive message = $message")
  }
}
