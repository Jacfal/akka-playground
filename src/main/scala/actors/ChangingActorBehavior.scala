package actors

import akka.actor._

object ChangingActorBehavior extends App {
  class FussyKid extends Actor {
    import FussyKid._
    import Mom._
    var state = Happy
    override def receive: Receive = {
      case Food(Vegetable) => state = Sad
      case Food(Chocolate) => state = Happy
      case Ask(_) =>
        if (state == Happy) sender ! KidAccept
        else sender ! KidReject
    }    
  }

  object FussyKid {
    case object KidAccept
    case object KidReject
    val Happy = "happy"
    val Sad = "sad"
  }

  class StatelessFussyKid extends Actor {
    import FussyKid._
    import Mom._

    override def receive: Receive /* partial any, unit */ = happyReceive

    def happyReceive: Receive = {
      case Food(Vegetable) => context.become(sadReceive) // change my receive handler to sadReceive
      case Food(Chocolate) => // stay happy
      case Ask(_) => sender ! KidAccept
    }
    def sadReceive: Receive = {
      case Food(Vegetable) => // stay sad
      case Food(Chocolate) => context.become(happyReceive) // change my receive handler to happy receive
      case Ask(_) => sender ! KidReject
    }
  }

  class Mom extends Actor {
    import Mom._
    import FussyKid._
    override def receive: Receive = {
      case MomStart(kidRef) => 
        // test our interaction
        kidRef ! Food(Vegetable)
        kidRef ! Ask("Are you happy?")
      case KidAccept => 
        println("Oh yeah, my kid is happy!")
      case KidReject => 
        println("Oh no, my kid is sad :(")
     }
  }

  object Mom {
    case class MomStart(kidReference: ActorRef)
    case class Food(food: String)
    case class Ask(message: String) // do you wan tto play?
    val Vegetable = "veggies"
    val Chocolate = "chocolate"
  }

  val system = ActorSystem("changingActorBehavior")
  val fussyKid = system.actorOf(Props[FussyKid])
  val statelessFussyKid = system.actorOf(Props[StatelessFussyKid])
  val mom = system.actorOf(Props[Mom])

  //mom ! Mom.MomStart(fussyKid)
  mom ! Mom.MomStart(statelessFussyKid)
}
