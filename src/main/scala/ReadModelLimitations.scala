object ReadModelLimitations extends App {
  // #1 OOP encapsulation is only valid in the single threaded models
  class BankAccount(private var amount: Int) {
    override def toString(): String = s"$amount"
    def withdraw(money: Int) = this.amount -= money
    def deposit(money: Int) = this.amount += money
    def getAmount = amount
  }

  val account = new BankAccount(2000)
  for (_ <- 1 to 1000) {
    new Thread(() => account.withdraw(1)).start()
  }

  for (_ <- 1 to 1000) {
    new Thread(() => account.deposit(1)).start()
  }
  println(account.getAmount)

  // OOP encapsulation is broken in multi threaded environment
  // synchronization! Locks to rescue (it can cause deadlocks, livelocks, ...)

  // #2 - delegating something to a thread is a pain
  // you have a running thread and you want to pass a runnable to that thread

  // #3 - tracing and dealing with errors is another pain!
}
