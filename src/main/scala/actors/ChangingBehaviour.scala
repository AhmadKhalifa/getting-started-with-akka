package com.akka.learn
package actors

import actors.Mother._

import akka.actor._

class Child extends Actor {

  import Child._

  // Stateful solution

//  var status: Status = HAPPY
//
//  override def receive: Receive = {
//    case Chocolate => status = HAPPY
//    case Vegetables => status = SAD
//    case InvitationToPlay => sender() ! (if (status == HAPPY) ChildAccepts else ChildRejects)
//  }

  // Stateless solution - Context replacement

//  override def receive: Receive = happyReceive
//
//  def happyReceive: Receive = {
//    case Chocolate =>
//    case Vegetables => context become sadReceive
//    case InvitationToPlay => sender() ! ChildAccepts
//  }
//
//  def sadReceive: Receive = {
//    case Chocolate => context become happyReceive
//    case Vegetables =>
//    case InvitationToPlay => sender() ! ChildRejects
//  }

  // Stateless context stacking

  override def receive: Receive = happyReceive

  def happyReceive: Receive = {
    case Chocolate =>
    case Vegetables => context.become(sadReceive, discardOld = false)
    case InvitationToPlay => sender() ! ChildAccepts
  }

  def sadReceive: Receive = {
    case Chocolate => context.unbecome()
    case Vegetables => context.become(sadReceive, discardOld = false)
    case InvitationToPlay => sender() ! ChildRejects
  }
}

object Child {
  case object Chocolate
  case object Vegetables
  case object InvitationToPlay

  case object ChildAccepts
  case object ChildRejects

  sealed trait Status
  case object HAPPY extends Status
  case object SAD extends Status
}

class Mother extends Actor {

  import Child._
  import Mother._

  override def receive: Receive = {
    case FeedChocolate(child) => child ! Chocolate
    case FeedVegetables(child) => child ! Vegetables
    case InviteToPlay(child) => child ! InvitationToPlay
    case ChildAccepts => println("My child is happy")
    case ChildRejects => println("My child isn't happy")
  }
}

object Mother {
  case class FeedChocolate(child: ActorRef)
  case class FeedVegetables(child: ActorRef)
  case class InviteToPlay(child: ActorRef)
}



object ChangingBehaviour extends App {

  val actorSystem: ActorSystem = ActorSystem("actorSystem")

  val mother: ActorRef = actorSystem.actorOf(Props[Mother], "mother")
  val child: ActorRef = actorSystem.actorOf(Props[Child], "child")

  mother ! FeedVegetables(child)
  mother ! FeedVegetables(child)
  mother ! FeedChocolate(child)
  mother ! FeedChocolate(child)
  mother ! InviteToPlay(child)
}
