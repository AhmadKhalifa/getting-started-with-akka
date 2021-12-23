package com.akka.learn
package actors

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{CallingThreadDispatcher, TestActorRef, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration.Duration

class SynchronousTestingSpec extends AnyWordSpecLike with BeforeAndAfterAll {
  implicit val system: ActorSystem = ActorSystem("SynchronousTestingSpec")

  override protected def afterAll(): Unit = system.terminate()

  import SynchronousTestingSpec._
  import Counter._

  "A counter" should {
    "synchronously increase its counter" in {
      // Given
      val counter = TestActorRef[Counter](Props[Counter])

      // When
      counter ! Increment

      // Then
      assert(counter.underlyingActor.count == 1)
    }

    "synchronously increase its counter at the call of the receive function" in {
      // Given
      val counter = TestActorRef[Counter](Props[Counter])

      // When
      counter.receive(Increment)

      // Then
      assert(counter.underlyingActor.count == 1)
    }

    "work on the calling thread dispatcher" in {
      // Given
      val counter = system.actorOf(Props[Counter].withDispatcher(CallingThreadDispatcher.Id))
      val probe = TestProbe()

      // When
      probe.send(counter, GetValue)

      // Then
      probe.expectMsg(Duration.Zero, 0)
    }
  }
}

object SynchronousTestingSpec {

  class Counter extends Actor {

    import Counter._

    var count = 0

    override def receive: Receive = {
      case Increment => count += 1
      case GetValue  => sender() ! count
    }
  }

  object Counter {
    case object Increment
    case object GetValue
  }
}