package com.akka.learn
package actors

import akka.actor._


class Counter extends Actor {

  import Counter._

  def onMessage(count: Int): Receive = {
    case Increment => context become onMessage(count + 1)
    case Decrement => context become onMessage(count - 1)
    case Print => println(count)
  }

  override def receive: Receive = onMessage(0)
}

object Counter {
  case object Increment
  case object Decrement
  case object Print
}

object CounterExercise extends App {
  val actorSystem: ActorSystem = ActorSystem("actorSystem")
  val counter: ActorRef = actorSystem.actorOf(Props[Counter], "counter")

  import Counter._
  counter ! Increment
  counter ! Increment
  counter ! Decrement
  counter ! Print
}
