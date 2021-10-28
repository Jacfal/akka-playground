import akka.actor.{ActorSystem, Actor, Props}
import akka.actor.ActorRef
// Exercises
// 1. counter actor
//  -- increment
//  -- decrement
//  -- print

object CounterActorExercise extends App {
  val actorSystem = ActorSystem("simpleCounter")
  val counterActor = actorSystem.actorOf(Props[CounterActor], "simpleCounter")

  counterActor ! CounterActor.Increment
  counterActor ! CounterActor.Decrement
  counterActor ! CounterActor.Print
}

class CounterActor extends Actor {
  import CounterActor._
  private var counter = 0

  override def receive: Receive = {
    case Increment => counter += 1
    case Decrement => counter -= 1
    case Print => println(s"Counter value $counter")
  }
}

object CounterActor { // domain of the actor
  case object Increment
  case object Decrement
  case object Print
}

// 2. bank account as an actor
// -> receives
//  -- deposit amount
//  -- withdraw amount
//  -- statement
// -> replies with
//  -- success
//  -- failure
// interact with some other type of actor
trait BankAccountOp

case class CustomerOp(baOp: BankAccountOp, ba: ActorRef)
case class TransactionSuccess(message: String)
case class TransactionFailure(errMessage: String)

object BankAccountExercise extends App {
  val actorSystem = ActorSystem("bankAccountExercise")
  val bankAccountActor = actorSystem.actorOf(Props[BankAccount], "bankAccount")
  val customerActor = actorSystem.actorOf(Props[Customer], "customer")

  customerActor ! CustomerOp(BankAccount.Status, bankAccountActor)
  customerActor ! CustomerOp(BankAccount.Deposit(100), bankAccountActor)
  customerActor ! CustomerOp(BankAccount.Withdraw(40), bankAccountActor)
  customerActor ! CustomerOp(BankAccount.Status, bankAccountActor)
  customerActor ! CustomerOp(BankAccount.Withdraw(100), bankAccountActor)
}

class BankAccount extends Actor {
  import BankAccount._

  private var accountState = 0

  override def receive: Receive = {
    case Deposit(amount) => 
      if (amount < 0) {
        sender ! TransactionFailure("Invalid amount. Must be positive integer")
      } else {
        accountState += amount
        sender ! TransactionSuccess(s"Amount $amount deposit success")
      }
    case Withdraw(amount) => 
      if (accountState - amount >= 0) {
        accountState -= amount 
        sender ! TransactionSuccess(s"Amount $amount successfully withdrew")
      } else {
        sender ! TransactionFailure("Insufficient account balance")
      }
    case Status => 
      sender ! s"Account state $accountState"
  }
}

object BankAccount {
  case class Deposit(amount: Int) extends BankAccountOp
  case class Withdraw(amount: Int) extends BankAccountOp
  case object Status extends BankAccountOp
}

class Customer extends Actor {

  override def receive: Receive = {
    case msg: String => println(msg)
    case TransactionSuccess(msg) => println(s"Transaction success => $msg")
    case TransactionFailure(errMsg) => println(s"Transaction error => $errMsg")
    case CustomerOp(baOp, ba) => ba ! baOp
  }
}
