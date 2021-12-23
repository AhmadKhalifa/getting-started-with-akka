package com.akka.learn
package actors

import actors.TestProbeSpec.Master

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

class TestProbeSpec extends TestKit(ActorSystem("probeSpec"))
  with ImplicitSender
  with AnyWordSpecLike
  with BeforeAndAfterAll {

  override protected def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  "A master actor" should {

    import Master._

    "register a slave" in {
      // Given
      val master = system.actorOf(Props[Master])
      val slave = TestProbe("slave")

      // When
      master ! Register(slave.ref)

      // Then
      expectMsg(RegistrationAck)
    }

    "send work to slave" in {
      // Given
      val master = system.actorOf(Props[Master])
      val slave = TestProbe("slave")
      val workLoadString = "Hello world"
      val originalSender = testActor
      val wordsCount = workLoadString.split(" ").length
      val slaveReply = WorkCompleted(wordsCount, originalSender)
      val expectedReport = Report(wordsCount)

      master ! Register(slave.ref)
      expectMsg(RegistrationAck)

      master ! Work(workLoadString)
      slave.expectMsgPF() {
        case SlaveWork(`workLoadString`, `originalSender`) =>
      }

      slave reply slaveReply
      expectMsg(expectedReport)
    }

    "send work to slave - multiple times" in {
      // Given
      val master = system.actorOf(Props[Master])
      val slave = TestProbe("slave")
      val workLoadString = "Hello world"
      val originalSender = testActor
      val wordsCount = workLoadString.split(" ").length
      val expectedReport1 = Report(wordsCount)
      val expectedReport2 = Report(wordsCount * 2)

      master ! Register(slave.ref)
      expectMsg(RegistrationAck)

      master ! Work(workLoadString)
      master ! Work(workLoadString)
      slave.receiveWhile() {
        case SlaveWork(`workLoadString`, `originalSender`) => slave reply WorkCompleted(wordsCount, originalSender)
      }

      expectMsg(expectedReport1)
      expectMsg(expectedReport2)
    }
  }
}


object TestProbeSpec {

  class SimpleActor extends Actor {
    override def receive: Receive = {
      case message => sender() ! message
    }
  }

  class Master extends Actor {

    import Master._

    override def receive: Receive = {
      case Register(slave) =>
        sender() ! RegistrationAck
        context become online(slave, 0)
      case message => self ! message
    }

    private def online(slave: ActorRef, currentWordCount: Int): Receive = {
      case Work(text) => slave ! SlaveWork(text, sender())
      case WorkCompleted(count, originalSender) =>
        val newWordCount = currentWordCount + count
        originalSender ! Report(newWordCount)
        context become online(slave, newWordCount)
    }
  }

  object Master {
    case object RegistrationAck
    case class Work(text: String)
    case class SlaveWork(text: String, sender: ActorRef)
    case class WorkCompleted(count: Int, originalSender: ActorRef)
    case class Register(slave: ActorRef)
    case class Report(count: Int)
  }
}
