package infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Terminated}
import akka.routing.{ActorRefRoutee, FromConfig, RoundRobinGroup, RoundRobinPool, RoundRobinRoutingLogic, Router}
import com.typesafe.config.ConfigFactory

object Routers extends App {
  val system = ActorSystem("routers", ConfigFactory.load().getConfig("routersDemo"))
  val driver = system.actorOf(Props[Driver])

  // sending test messages
  for (_ <- 1 to 10) {
    driver ! "Hail to all workers!"
  }

  // router actor with it's own children
  val poolMaster = system.actorOf(RoundRobinPool(5).props(Props[Worker]))
  for (_ <- 1 to 10) {
    poolMaster ! "Hail to pool workers!"
  }

  // from config
  val poolMaster2 = system.actorOf(FromConfig.props(Props[Worker]), "poolMaster2")
  for (_ <- 1 to 10) {
    poolMaster2 ! "Hail to pool workers from config!"
  }

  // router with actors created elsewhere (group router)
  val workerList = (1 to 5).map(i => system.actorOf(Props[Worker], s"worker_$i"))
  val workerPaths = workerList.map(workerRef => workerRef.path.toString)
  val groupDriver = system.actorOf(RoundRobinGroup(workerPaths).props())
  for (_ <- 1 to 10) {
    groupDriver ! "Hail to pool workers from group router!"
  }

  // PoisonPill and Kill are NOT routed!
  // AddRoutes, Remove, get are handled only by the routing actor
}

/**
  * #1 manually created routed
  */
class Driver extends Actor {
  // 5 actor routees based off worker actors
  private val workers = for (i <- 1 to 5) yield {
    val worker = context.actorOf(Props[Worker], s"worker-$i")
    context.watch(worker)
    ActorRefRoutee(worker)
  }

  /*
    Supported routing logic:
      - round robin
      - random
      - smallest mailbox (send message to actor with lowest count of messages in the queue)
      - broadcast (send message to all routers)
      - scatter-gather-list
      - tail-chopping
      - consistent-hashing
   */
  private val router = Router(RoundRobinRoutingLogic(), workers)

  override def receive: Receive = onMessage(router)

  private def onMessage(router: Router): Receive = {
    case Terminated(ref) =>
      val newWorker = context.actorOf(Props[Worker])
      context.watch(newWorker)
      context.become(onMessage(router
        .removeRoutee(ref)
        .addRoutee(newWorker)))

    case message =>
      // route the messages
      router.route(message, sender())

  }
}

class Worker extends Actor with ActorLogging {
  override def receive: Receive = {
    case message => log.info(f"Worker received message => $message")
  }
}
