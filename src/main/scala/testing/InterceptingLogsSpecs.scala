package testing

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.testkit.{EventFilter, ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

class InterceptingLogsSpecs extends TestKit(ActorSystem("InterceptingLogSpecs", ConfigFactory.load().getConfig("interceptingLogMessages")))
  with ImplicitSender
  with AnyWordSpecLike
  with BeforeAndAfterAll {
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import InterceptingLogsSpecs._
  "A checkout flow" should {
    val item = "Skoda 120"
    val creditCard = "1234-4321-0987-7890"
    val invalidCreditCard = "0000-0000-0000-0000"

    "correctly log the dispatch of an order" in {
      EventFilter.info(pattern = s"Order id [0-9+] for $item has been dispatched", occurrences = 1) intercept {
        // our test code
        val checkoutRef = system.actorOf(Props[CheckoutActor])
        checkoutRef ! Checkout(item, creditCard)
      }
    }

    "freak out if the payment is denied" in {
      EventFilter[RuntimeException](occurrences = 1) intercept {
        val checkoutRef = system.actorOf(Props[CheckoutActor])
        checkoutRef ! Checkout(item, invalidCreditCard)
      }
    }
  }

}

object InterceptingLogsSpecs {

  case class Checkout(item: String, creditCard: String)
  case class AuthorizedCard(creditCard: String)
  case class DispatchOrder(item: String)
  case object PaymentAccepted
  case object PaymentDenied
  case object OrderConfirmed
  case object OrderRejected

  class CheckoutActor extends Actor {
    // contains child actors
    private val paymentManager = context.actorOf(Props[PaymentManager])
    private val fulfillmentManager = context.actorOf(Props[FulfillmentManager])

    override def receive: Receive = awaitingCheckout

    def awaitingCheckout: Receive = {
      case Checkout(item, card) =>
        paymentManager ! AuthorizedCard(card)
        context.become(pendingPayment(item))
    }

    def pendingPayment(item: String): Receive = {
      case PaymentAccepted =>
        fulfillmentManager ! DispatchOrder(item)
        context.become(pendingFulfillment(item))
      case PaymentDenied =>
        throw new RuntimeException("Oh no, payment denied!")
    }

    def pendingFulfillment(item: String): Receive = {
      case OrderConfirmed => context.become(awaitingCheckout)
    }
  }

  class PaymentManager extends Actor {
    override def receive: Receive = {
      case AuthorizedCard(creditCard: String) =>
        if (creditCard.startsWith("0"))
          sender ! PaymentDenied
        else
          sender ! PaymentAccepted
    }
  }

  class FulfillmentManager extends Actor with ActorLogging {
    var orderId = 0
    override def receive: Receive = {
      case DispatchOrder(item: String) =>
        orderId += 1
        log.info(s"Order id $orderId for $item has been dispatched")
        sender ! OrderConfirmed
    }
  }
}
