package patterns

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Cancellable, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps

class FiniteStateMachineSpec extends TestKit(ActorSystem("FiniteStateMachine"))
  with ImplicitSender
  with AnyWordSpecLike
  with BeforeAndAfterAll {

  import FiniteStateMachineSpec._

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  "A vending machine" should {
    "error when not initialized" in {
      val vendingMachine = system.actorOf(Props[VendingMachineActor])
      vendingMachine ! RequestProduct("beer")
      expectMsg(VendingError(VendingMachineMessages.INIT_ERROR))
    }

    "report a product not available" in {
      val vendingMachine = system.actorOf(Props[VendingMachineActor])
      vendingMachine ! Initialize(Map("beer" -> (1, 4)))
      vendingMachine ! RequestProduct("coke")
      expectMsg(VendingError(VendingMachineMessages.NOT_AVAILABLE_ERROR))
    }

    "throw a timeout if i don't insert money" in {
      val vendingMachine = system.actorOf(Props[VendingMachineActor])
      vendingMachine ! Initialize(Map("beer" -> (1, 4)))
      vendingMachine ! RequestProduct("beer")
      expectMsg(Instruction("Please insert 4 dollar(s)"))

      within(1.5 seconds) {
        expectMsg(VendingError(VendingMachineMessages.TIMEOUT_ERROR))
      }
    }

    "handle reception of partial money" in {
      val vendingMachine = system.actorOf(Props[VendingMachineActor])
      vendingMachine ! Initialize(Map("beer" -> (1, 4)))
      vendingMachine ! RequestProduct("beer")
      expectMsg(Instruction("Please insert 4 dollar(s)"))

      vendingMachine ! ReceiveMoney(1)
      expectMsg(Instruction(s"Please insert 3 dollar(s)"))

      within(1.5 seconds) {
        expectMsg(VendingError(VendingMachineMessages.TIMEOUT_ERROR))
        expectMsg(GiveBackChange(1))
      }
    }

    "deliver the product if in insert all money" in {
      val vendingMachine = system.actorOf(Props[VendingMachineActor])
      vendingMachine ! Initialize(Map("beer" -> (1, 4)))
      vendingMachine ! RequestProduct("beer")
      expectMsg(Instruction("Please insert 4 dollar(s)"))

      vendingMachine ! ReceiveMoney(1)
      expectMsg(Instruction(s"Please insert 3 dollar(s)"))

      vendingMachine ! ReceiveMoney(3)
      expectMsg(Deliver("beer"))
    }
  }

}

object FiniteStateMachineSpec {
  /*
    Vending machine
   */
  type InventoryItems = Map[String, (Int, Int)] // product (inventory id, price)

  case class Initialize(inventory: InventoryItems)
  case class RequestProduct(product: String)
  case class Instruction(message: String) // message the vending machine will show on the screen
  case class ReceiveMoney(amount: Int)
  case class Deliver(product: String)
  case class GiveBackChange(amount: Int)
  case class VendingError(message: String)
  case object ReceiveMoneyTimeout

  object VendingMachineMessages {
    val INIT_ERROR = "Machine not initialized"
    val NOT_AVAILABLE_ERROR = "ProductNotAvailable"
    val TIMEOUT_ERROR = "Request timed out"
  }

  class VendingMachineActor extends Actor with ActorLogging {
    implicit val ec: ExecutionContext = context.dispatcher

    override def receive: Receive = idle

    def idle: Receive = {
      case Initialize(inventory) =>
        context.become(operational(inventory))
      case _ =>
        sender() ! VendingError(VendingMachineMessages.INIT_ERROR)
    }

    def operational(inventory: InventoryItems): Receive = {
      case RequestProduct(product) => inventory.get(product) match {
        case None =>
          sender() ! VendingError(VendingMachineMessages.NOT_AVAILABLE_ERROR)
        case Some(item) =>
          val price = item._2
          sender() ! Instruction(s"Please insert $price dollar(s)")
          context.become(waitForMoney(inventory, price, product, 0, startReceiveTimeoutMoneySchedule, sender()))
      }
    }

    def waitForMoney(
                    inventory: InventoryItems,
                    itemPrice: Int,
                    product: String,
                    money: Int, // TODO ???
                    moneyTimeoutSchedule: Cancellable,
                    requester: ActorRef
                    ): Receive = {
      case ReceiveMoneyTimeout =>
        requester ! VendingError("Request timed out")
        if (money > 0) {
          requester ! GiveBackChange(money)
          context.become(operational(inventory))
        }
      case ReceiveMoney(amount) =>
        moneyTimeoutSchedule.cancel()
        if ((money + amount) >= itemPrice) {

          // user buys product
          requester ! Deliver(product)

          // deliver the change
          val moneyToReturn = money + amount - itemPrice
          if (moneyToReturn > 0)
            requester ! GiveBackChange(moneyToReturn)

          // update the inventory
          val newStock = (inventory(product)._1 - 1, inventory(product)._2)
          val newInventory = inventory + (product -> newStock)
          context.become(operational(newInventory))
        } else {
          val remainingMoney = itemPrice - amount - money
          requester ! Instruction(s"Please insert $remainingMoney dollar(s)")
          context.become(waitForMoney(inventory, itemPrice, product, money + amount, startReceiveTimeoutMoneySchedule, requester))
        }

    }

    def startReceiveTimeoutMoneySchedule: Cancellable = context.system.scheduler.scheduleOnce(1 seconds) {
      self ! ReceiveMoneyTimeout
    }
  }
}
