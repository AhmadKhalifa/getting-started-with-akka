package com.akka.learn
package actors

import akka.actor.SupervisorStrategy._
import akka.actor.{Actor, ActorRef, ActorSystem, AllForOneStrategy, OneForOneStrategy, Props, SupervisorStrategy, Terminated}
import akka.testkit.{EventFilter, ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

class SupervisionSpec extends TestKit(ActorSystem("SupervisionSpec"))
  with ImplicitSender
  with AnyWordSpecLike
  with BeforeAndAfterAll {

  import SupervisionSpec._
  import SupervisionSpec.FussyWordCounter._

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "A Supervisor" should {
    "resume its child in case of a minor fault" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      child ! "I love Akka"
      child ! Report
      expectMsg(3)

      child ! "Akka is awesome for many many many many reasons"
      child ! Report
      expectMsg(3)

      child ! "I love Akka again"
      child ! Report
      expectMsg(7)
    }

    "restart its child in case of an empty sentence" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      child ! "I love Akka"
      child ! Report
      expectMsg(3)

      child ! ""
      child ! Report
      expectMsg(0)

      child ! "I love Akka again"
      child ! Report
      expectMsg(4)
    }

    "stop its child in case of a major fault" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      child ! "I love Akka"
      child ! Report
      expectMsg(3)

      watch(child)
      child ! "hi"
      val terminatedMessage = expectMsgType[Terminated]
      assert(terminatedMessage.actor == child)
    }

    "escalate an error when it doesn't know what to do" in {
      val supervisor = system.actorOf(Props[Supervisor],"supervisor")
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      watch(child)
      child ! 5
      val terminatedMessage = expectMsgType[Terminated]
      assert(terminatedMessage.actor == child)
    }
  }

  "A kinder supervisor" should {
    "not kill children in case it's restarted or escalates failure" in {
      val supervisor = system.actorOf(Props[NoDeathOnRestartSupervisor], "supervisor2")
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      child ! "Akka is cool"
      child ! Report
      expectMsg(3)

      child ! 45
      child ! Report
      expectMsg(0)
    }
  }

  "An all-for-one supervisor" should {
    "apply the all-for-one strategy" in {
      val supervisor = system.actorOf(Props[AllForOneSupervisor], "supervisor3")
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      supervisor ! Props[FussyWordCounter]
      val secondChild = expectMsgType[ActorRef]

      secondChild ! "Testing supervision"
      secondChild ! Report
      expectMsg(2)

      EventFilter[NullPointerException]() intercept {
        child ! ""
      }

      Thread.sleep(1000)
      secondChild ! Report
      expectMsg(0)
    }
  }
}

object SupervisionSpec {

  class Supervisor extends Actor {

    override val supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
      case _: NullPointerException => Restart
      case _: IllegalArgumentException => Stop
      case _: RuntimeException => Resume
      case _: Exception => Escalate
    }

    override def receive: Receive = {
      case props: Props =>
        val child = context.actorOf(props)
        sender() ! child
    }
  }

  class NoDeathOnRestartSupervisor extends Supervisor {

    override def preRestart(reason: Throwable, message: Option[Any]): Unit = {}
  }

  class AllForOneSupervisor extends Supervisor {
    override val supervisorStrategy: SupervisorStrategy = AllForOneStrategy() {
      case _: NullPointerException => Restart
      case _: IllegalArgumentException => Stop
      case _: RuntimeException => Resume
      case _: Exception => Escalate
    }
  }

  class FussyWordCounter extends Actor {

    import FussyWordCounter._

    var words = 0

    override def receive: Receive = {
      case Report => sender() ! words
      case "" => throw new NullPointerException("Sentence is empty")
      case sentence: String =>
        if (sentence.length > 20)
          throw new RuntimeException("Too long")
        else if (!Character.isUpperCase(sentence.head))
          throw new IllegalArgumentException("Must start with an upper-case")
        else
          words += sentence.split(" ").length
      case _ => throw new Exception("Only strings allowed")
    }
  }

  object FussyWordCounter {
    case object Report
  }
}