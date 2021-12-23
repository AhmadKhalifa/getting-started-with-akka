package com.akka.learn
package actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.testkit.{EventFilter, ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

import scala.language.postfixOps

class InterceptingLogsSpec
  extends TestKit(ActorSystem("probeSpec", ConfigFactory.load().getConfig("interceptingLogMessages")))
    with ImplicitSender
    with AnyWordSpecLike
    with BeforeAndAfterAll {

  import InterceptingLogsSpec._
  import CheckoutActor._

  override protected def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  private val item              = "Akka essentials course"

  "A checkout flow" should {
    "correctly log the dispatch of an order with a valid credit card" in {
      // Then
      EventFilter
        .info(pattern = s"Order [0-9]+ has been created successfully with item $item", occurrences = 1)
        .intercept {
          // Given
          val validCreditCard   = "1234-1234-1234-1234"
          val checkoutActor = system.actorOf(Props[CheckoutActor])

          // When
          checkoutActor ! Checkout(item, validCreditCard)
        }
    }

    "throw runtime exception when an invalid credit card is passed" in {
      // Then
      EventFilter[RuntimeException](occurrences = 1).intercept {
          // Given
          val invalidCreditCard = "0000-0000-0000-0000"
          val checkoutActor = system.actorOf(Props[CheckoutActor])

          // When
          checkoutActor ! Checkout(item, invalidCreditCard)
        }
    }
  }
}

object InterceptingLogsSpec {

  class CheckoutActor extends Actor with ActorLogging {

    import CheckoutActor._
    import FulfillmentManager._
    import PaymentManager._

    private val paymentManager = context.actorOf(Props[PaymentManager])
    private val fulfillmentManager = context.actorOf(Props[FulfillmentManager])

    override def receive: Receive = awaitingCheckout

    private def awaitingCheckout: Receive = {
      case Checkout(item, creditCardNumber) =>
        paymentManager ! AuthorizedCard(creditCardNumber)
        context become awaitingPayment(item)
    }

    private def awaitingPayment(item: String): Receive = {
      case PaymentAccepted =>
        fulfillmentManager ! DispatchOrder(item)
        context become awaitingFulfillment

      case PaymentDeclined =>
        throw new RuntimeException("Invalid credit card")
    }

    private def awaitingFulfillment: Receive = {
      case OrderConfirmed(orderId, item) =>
        log.info(s"Order $orderId has been created successfully with item $item")
        context become awaitingCheckout
    }
  }

  object CheckoutActor {
    case class Checkout(item: String, creditCardNumber: String)
    case class OrderConfirmed(orderId: Int, item: String)
    case object PaymentAccepted
    case object PaymentDeclined
  }

  class PaymentManager extends Actor {

    import CheckoutActor._
    import PaymentManager._

    override def receive: Receive = {
      case AuthorizedCard(cardNumber) =>
        Thread.sleep(4000)
        sender() ! (if (cardNumber.startsWith("0")) PaymentDeclined else PaymentAccepted)
    }
  }

  object PaymentManager {
    case class AuthorizedCard(cardNumber: String)
  }

  class FulfillmentManager extends Actor {

    import CheckoutActor._
    import FulfillmentManager._

    override def receive: Receive = onMessage(-1)

    private def onMessage(lastOrderId: Int): Receive = {
      case DispatchOrder(item) =>
        val orderId = lastOrderId + 1
        sender() ! OrderConfirmed(orderId, item)
        context become onMessage(orderId)
    }
  }

  object FulfillmentManager {
    case class DispatchOrder(item: String)
  }
}




































