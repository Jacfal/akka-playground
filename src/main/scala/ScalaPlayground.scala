object ScalaPlayground extends App {
  // FUnctional programming
  val anonIncrementer = (x: Int, y: Int) => x + y  + 1
  anonIncrementer(10, 20) // int => int === Function1[Int, Int]

  // implicits fun
  implicit class Dog(name: String) {
      def doSomeTricks = println("Doing some tricks...")
  }
  "Alik".doSomeTricks

  case class Person(name: String) {
      def hello = println(s"Hello, I'm $name")
  }
  implicit def string2Person(s: String): Person = Person(s) 
  "James".hello
}
