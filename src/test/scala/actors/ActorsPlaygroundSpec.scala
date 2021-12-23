package com.akka.learn
package actors

import actors.ActorsPlaygroundSpec.ToUpperCaseActor.{Akka, Scala}
import actors.ActorsPlaygroundSpec.{BlackHoleActor, SimpleActor, ToUpperCaseActor}

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

import scala.util.Random

class ActorsPlaygroundSpec extends TestKit(ActorSystem("actorSystem"))
  with ImplicitSender
  with AnyWordSpecLike
  with BeforeAndAfterAll {

  override protected def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  "A simple actor" should {
    "send back the same message" in {
      // Given
      val actor = system.actorOf(Props[SimpleActor])
      val message = "Hello"

      // When
      actor ! message

      // Then
      expectMsg(message)
    }
  }

  "A black hole actor" should {
    "send nothing back" in {
      // Given
      val actor = system.actorOf(Props[BlackHoleActor])
      val message = "Hello"

      // When
      actor ! message

      // Then
      expectNoMessage()
    }
  }

  "A to-upper-case actor" should {

    val actor = system.actorOf(Props[ToUpperCaseActor])

    "send back an upper-case version of the massage" in {
      // Given
      val message = "Learning Akka"

      // When
      actor ! message

      // Then
      val actualMessage = expectMsgType[String]
      assert(actualMessage == "LEARNING AKKA")
    }

    "send back either 'Hi' or 'Hello' when a 'Hello' is sent to it" in {
      // Given
      val message = "Hello"

      // When
      actor ! message

      // Then
      expectMsgAnyOf("Hi", "Hello")
    }

    "send back either 'Hi' and 'Hello' when a 'Hello and Hi' is sent to it" in {
      // Given
      val message = "Hello and Hi"

      // When
      actor ! message

      // Then
      expectMsgAllOf("Hi", "Hello")
    }

    "send back either 'Hi' and 'Hello' when a 'Hello and Hi' is sent to it (another way)" in {
      // Given
      val message = "Hello and Hi"

      // When
      actor ! message

      // Then
      val actualMessages = receiveN(2)
      actualMessages == Seq("Hi", "Hello")
    }

    "send back either 'Scala instance' or 'Akka object' when a 'Scala' is sent to it" in {
      // Given
      val message = "Scala"

      // When
      actor ! message

      // Then
      expectMsgPF() {
        case Scala(str) => assert(str == "Scala")
        case Akka =>
      }
    }
  }
}

object ActorsPlaygroundSpec {

  class SimpleActor extends Actor {
    override def receive: Receive = {
      case message => sender() ! message
    }
  }

  class BlackHoleActor extends Actor {
    override def receive: Receive = {
      case _ =>
    }
  }

  class ToUpperCaseActor extends Actor {
    override def receive: Receive = {
      case "Hello" => sender() ! (if (Random.nextBoolean()) "Hi" else "Hello")
      case "Hello and Hi" =>
        sender() ! "Hello"
        sender() ! "Hi"
      case "Scala" => sender() ! (if (Random.nextBoolean()) Scala("Scala") else Akka)
      case str: String => sender() ! str.toUpperCase
    }
  }

  object ToUpperCaseActor {
    case class Scala(string: String)
    case object Akka
  }
}