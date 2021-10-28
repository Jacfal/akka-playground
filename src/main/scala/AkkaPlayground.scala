import akka.actor.ActorSystem

object AkkaPlayground extends App {
  val actorSystem = ActorSystem("hello-akka")
  println(actorSystem.name)
}
