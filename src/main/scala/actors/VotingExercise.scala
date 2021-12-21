package com.akka.learn
package actors

import akka.actor._

class Citizen extends Actor {

  import Citizen._

  override def receive: Receive = onMessage(None)

  private def onMessage(candidate: Option[String]): Receive = {
    case Vote(candidate) => context become onMessage(Some(candidate))
    case CollectVoteRequest => sender() ! CollectVoteResponse(candidate)
  }
}

object Citizen {
  case class Vote(candidate: String)
  case object CollectVoteRequest
  case class CollectVoteResponse(candidate: Option[String])
}

class VotingSystem extends Actor {

  import Citizen._
  import VotingSystem._

  override def receive: Receive = awaitingStart

  private def updateVotes(results: Map[String, Int], candidate: String): Map[String, Int] =
    results + (candidate -> (results.getOrElse(candidate, 0) + 1))

  def awaitingStart: Receive = {
    case CollectVotes(citizens) =>
      citizens.foreach(_ ! CollectVoteRequest)
      context become awaitingVotes(citizens, Map.empty)
  }

  def awaitingVotes(pending: Set[ActorRef], results: Map[String, Int]): Receive = {
    case CollectVoteResponse(Some(candidate)) =>
      val newPending = pending - sender()
      val updatedVotes = updateVotes(results, candidate)
      context become (if (newPending.isEmpty) resultsReady(updatedVotes) else awaitingVotes(newPending, updatedVotes))

    case CollectVoteResponse(None) =>
      sender() ! CollectVoteRequest

    case PrintWinner => self ! PrintWinner
  }

  def resultsReady(results: Map[String, Int]): Receive = {
    case PrintWinner =>
      println(results.toList.sortBy(_._2).reverse.map(entry => s"${entry._1}: ${entry._2}").mkString(", "))
  }
}

object VotingSystem {
  case class CollectVotes(citizens: Set[ActorRef])
  case object PrintWinner
}

object VotingExercise extends App {

  val actorSystem: ActorSystem = ActorSystem("actorSystem")

  val votingSystem: ActorRef = actorSystem.actorOf(Props[VotingSystem])

  val citizen1: ActorRef = actorSystem.actorOf(Props[Citizen])
  val citizen2: ActorRef = actorSystem.actorOf(Props[Citizen])
  val citizen3: ActorRef = actorSystem.actorOf(Props[Citizen])
  val citizen4: ActorRef = actorSystem.actorOf(Props[Citizen])
  val citizen5: ActorRef = actorSystem.actorOf(Props[Citizen])

  import Citizen._

  citizen1 ! Vote("Khalifa")
  citizen2 ! Vote("Celine")
  citizen3 ! Vote("John")
  citizen4 ! Vote("Adam")
  citizen5 ! Vote("Khalifa")

  import VotingSystem._

  votingSystem ! CollectVotes(Set(citizen1, citizen2, citizen3, citizen4, citizen5))

  votingSystem ! PrintWinner
}
