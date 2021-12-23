package com.akka.learn
package actors

import actors.TestProbeSpec.Master

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.akka.learn.actors.TimesAssertionsSpec.SimpleActor.Result
import com.typesafe.config.ConfigFactory
import javafx.util.Duration.seconds
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.Random

class TimesAssertionsSpec
  extends TestKit(ActorSystem("probeSpec", ConfigFactory.load().getConfig("specialTimedAssertionsConfig")))
  with ImplicitSender
  with AnyWordSpecLike
  with BeforeAndAfterAll {

  override protected def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  "A simple actor" should {

    import TimesAssertionsSpec._

    "finish large calculation within 500 millis to 1 second" in {
      within(500 milli, 1 seconds) {
        // Given
        val actor = system.actorOf(Props[SimpleActor])

        // When
        actor ! "large"

        // Then
        expectMsg(Result(26))
      }
    }

    "finish small calculations within 1 second" in {
      within(1 seconds) {
        // Given
        val actor = system.actorOf(Props[SimpleActor])

        // When
        actor ! "small"

        // Then
        val results = receiveWhile[Int](messages = 10) {
          case Result(value) => value
        }
        assert(results.size == 10)
        assert(results.sum == (1 to 10).toList.sum)
      }
    }
  }
}

object TimesAssertionsSpec {

  class SimpleActor extends Actor {
    override def receive: Receive = {
      case "large" =>
        Thread.sleep(500)
        sender() ! Result(26)
      case "small" =>
      for (value <- 1 to 10) {
        Thread.sleep(Random.nextInt(50))
        sender() ! Result(value)
      }
    }
  }

  object SimpleActor {
    case class Result(value: Int)
  }
}

