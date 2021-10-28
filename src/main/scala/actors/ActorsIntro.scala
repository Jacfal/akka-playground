package actors

import akka.actor.{Actor, ActorSystem}
import akka.actor.Props

object ActorsIntro extends App {
   // part 1 - actor systems
   val actorSystem = ActorSystem("firstActorSystem")
   println(actorSystem.name) 

   // creating actors
   // note #1 - actors are uniquely identified
   // note #2 - messages are asynchronous
   // note #3 - each actor may respond differently
   // note #4 - actors are (really) encapsulated

   val wordCounter = actorSystem.actorOf(Props[WordCountActor], "wordCounter")
   val wordCounter2 = actorSystem.actorOf(Props[WordCountActor], "wordCounter2")

   // communication with actor
   wordCounter ! "Hello to Akka" // complete asynchronous
   wordCounter2 ! "Another Hello!" // complete asynchronous

   // completely encapsulated. You can;t instantiate word counter by yourself -> new WordCountActor
   // accessing to WordCountActor is through ActorRef
}

// word count actor
class WordCountActor extends Actor {
  var totalWords = 0

  override def receive: PartialFunction[Any, Unit] = {
    case message: String => 
      totalWords += message.split(" ").length
      println(s"Received $message")
    case message => 
      println(s"I can't understand $message")
  }
}
