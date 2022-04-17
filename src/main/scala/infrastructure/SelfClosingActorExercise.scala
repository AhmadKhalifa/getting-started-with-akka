package com.akka.learn
package infrastructure
import scala.concurrent.duration._
import akka.actor.{Actor, ActorLogging, ActorSystem, Cancellable, PoisonPill, Props}

import scala.concurrent.ExecutionContextExecutor
import scala.language.postfixOps

class SelfClosingActor extends Actor with ActorLogging {

  private implicit val executionContext: ExecutionContextExecutor = context.system.dispatcher

  def newDelayedJob: Cancellable = context.system.scheduler.scheduleOnce(1 seconds) {
    self ! PoisonPill
  }

  override def receive: Receive = {
    case any =>
      log info any.toString
      context become withDelayedJob(newDelayedJob)
  }

  def withoutDelayedJob: Receive = {
    case any =>
      log info any.toString
      context become withDelayedJob(newDelayedJob)
  }

  def withDelayedJob(delayedJob: Cancellable): Receive = {
    case any =>
      log info any.toString
      delayedJob.cancel
      context become withDelayedJob(newDelayedJob)
  }
}

object SelfClosingActorExercise extends App {
  val actorSystem = ActorSystem("system")
  val actor = actorSystem.actorOf(Props[SelfClosingActor])
  actor ! "Hello"
  actor ! "World"
  Thread.sleep(300)
  actor ! "from"
  Thread.sleep(1050)
  actor ! "the other side"
}
