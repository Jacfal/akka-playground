package actors

import akka.actor._

object ChangingActorBehaviorExercises extends App {
  import StatelessCounterExercise._
  import StatelessCounter._
  import VotingSystemExercise._
  import VotingSystemExercise.Citizen._
  import VoteAggregator._
  
  val system = ActorSystem("ChangingActorBehaviorExercises")
  // 1. recreate the counter with context become and without any state
  val counter = system.actorOf(Props[StatelessCounter])
  counter ! Increment
  counter ! Increment
  counter ! Decrement
  counter ! Decrement
  counter ! Decrement
  counter ! Print // -1


  // 2. simplified voting system
  val james = system.actorOf(Props[Citizen])
  val alice = system.actorOf(Props[Citizen])
  val bob = system.actorOf(Props[Citizen])
  val tess = system.actorOf(Props[Citizen])

  alice ! Vote("True")
  bob ! Vote("True")
  james ! Vote("El Presidente")

  val voteAggregator = system.actorOf(Props[VoteAggregator])
  voteAggregator ! AggregateVotes(Set(alice, bob, james, tess))

  // TODO print status of votes
  // True: 2
  // El Presidente: 1 
}

object StatelessCounterExercise {
  class StatelessCounter extends Actor {
    import StatelessCounter._
    override def receive: Receive = opReceive(0)

    def opReceive(v: Int): Receive = {
      case Increment => context.become(opReceive(v + 1))
      case Decrement => context.become(opReceive(v - 1))
      case Print => println(s"Current value $v")
    }
  }

  object StatelessCounter {
    case object Increment
    case object Decrement
    case object Print
  }
}

object VotingSystemExercise {
  class Citizen extends Actor {
    import Citizen._ 
    import VoteAggregator._ 

    // citizen must have a state un/voted
    override def receive: Receive = { // no-voted
      case Vote(candidate: String) => context.become(voted(candidate))
      case VoteStatusRequest => sender ! VoteStatusReply(None)
    } 

    def voted(candidate: String): Receive = {
      case VoteStatusRequest => sender ! VoteStatusReply(Some(candidate))
    }
  }

  object Citizen {
    case class Vote(candidate: String)
    case object VoteAccepted
    case object VoteRejected
    case class VoteStatusReply(candidate: Option[String])
  }

  class VoteAggregator extends Actor {
    import Citizen._
    import VoteAggregator._
    
    override def receive: Receive = aggregateVotes

    def aggregateVotes: Receive = {
      case AggregateVotes(refs: Set[ActorRef]) => 
        refs.foreach(_ ! VoteStatusRequest)
        context.become(processVotes(refs, Map.empty))
    }

    def processVotes(awaitingFor: Set[ActorRef], votes: Map[String, Int]): Receive = {
      case VoteStatusReply(vote: Option[String]) => 
        val remainingVoters = awaitingFor - sender
        vote match {
          case None => 
            if (remainingVoters.isEmpty) println(s"Vote results ==> $votes")
            else context.become(processVotes(remainingVoters, votes))
          case Some(candidate) => 
            val numOfCandidateVotes = votes.getOrElse(candidate, 0)
            val updatedVotes = votes + (candidate -> (numOfCandidateVotes + 1))
            if (remainingVoters.isEmpty) println(s"Vote results ==> $updatedVotes")
            else context.become(processVotes(remainingVoters, updatedVotes))
        }
    }
  }

  object VoteAggregator {
    case class AggregateVotes(citizens: Set[ActorRef])
    case object VoteStatusRequest
  }
}
