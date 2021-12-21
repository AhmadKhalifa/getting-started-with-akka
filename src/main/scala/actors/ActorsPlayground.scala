package com.akka.learn
package actors

import actors.BankAccount.{FailedTransaction, SuccessTransaction, Transaction}
import actors.Person.{Deposit, Statement, Withdraw}

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

import scala.util.{Failure, Success, Try}

class Bot(name: String) extends Actor {
  override def receive: Receive = {
    case "Hi" => println(s"[$name] Hi there my name is $name")
    case this.name => println(s"[$name] Yes?")
    case Bot.Message(content) => println(s"[$name] I received content: $content")
    case Bot.ForwardedMessage(content, receiver) => receiver forward Bot.Message(content)
    case Bot.Greeting(receiver) => receiver ! "Hi"
    case Bot.Repeat(content) => sender() ! content
    case Bot.TalkToYourself(content) => self ! content
    case _ => println("[$name] Hmm. let's try it an another words")
  }
}

object Bot {
  def props(name: String): Props = Props(new Bot(name))
  case class Message(content: String)
  case class Greeting(receiver: ActorRef)
  case class ForwardedMessage(content: String, sender: ActorRef)
  case class Repeat(content: String)
  case class TalkToYourself(content: String)
}

class BankAccount extends Actor {

  var balance: Double = 0

  def withdraw(amount: Double): Transaction =
    Try {
      if (amount > balance) throw new IllegalArgumentException(s"Current balance is $$$balance")
      balance -= amount
      balance
    } match {
      case Failure(exception) => FailedTransaction(exception.getMessage)
      case Success(value) => SuccessTransaction(value)
    }

  def deposit(amount: Double): Transaction =
    Try {
      if (amount < 0) throw new IllegalArgumentException(s"Invalid amount $$$amount")
      balance += amount
      balance
    } match {
      case Failure(exception) => FailedTransaction(exception.getMessage)
      case Success(value) => SuccessTransaction(value)
    }

  def statement: Transaction =
    Try {
      balance
    } match {
      case Failure(exception) => FailedTransaction(exception.getMessage)
      case Success(value) => SuccessTransaction(value)
    }

  override def receive: Receive = {
    case BankAccount.Withdraw(amount) => sender() ! withdraw(amount)
    case BankAccount.Deposit(amount) => sender() ! deposit(amount)
    case BankAccount.Statement => sender() ! statement
  }
}

object BankAccount {
  case class Withdraw(amount: Double)
  case class Deposit(amount: Double)
  case object Statement

  sealed trait Transaction
  case class SuccessTransaction(newBalance: Double) extends Transaction
  case class FailedTransaction(message: String) extends Transaction
}

class Person extends Actor {
  override def receive: Receive = {
    case Person.Withdraw(amount, bankAccount) => bankAccount ! BankAccount.Withdraw(amount)
    case Person.Deposit(amount, bankAccount) => bankAccount ! BankAccount.Deposit(amount)
    case Person.Statement(bankAccount) => bankAccount ! BankAccount.Statement
    case SuccessTransaction(newBalance) => println(s"Successful! new balance is $$$newBalance")
    case FailedTransaction(message) => println(s"Failed: $message")
  }
}

object Person {
  case class Withdraw(amount: Double, bankAccount: ActorRef)
  case class Deposit(amount: Double, bankAccount: ActorRef)
  case class Statement(bankAccount: ActorRef)
}

object ActorsPlayground extends App {

  val actorSystem: ActorSystem = ActorSystem("actorSystem")

  val alice: ActorRef = actorSystem.actorOf(Bot.props("Alice"), "alice")
  val bob: ActorRef = actorSystem.actorOf(Bot.props("Bob"), "bob")
  val carl: ActorRef = actorSystem.actorOf(Bot.props("Carl"), "carl")

  alice ! "Hi"
  alice ! "Alice"
  alice ! Bot.Message("Hello there")
  alice ! Bot.Greeting(bob)
  alice ! Bot.TalkToYourself("Hi")
  alice ! Bot.ForwardedMessage("Hi", bob)
  alice.tell(Bot.ForwardedMessage("Hi", bob), carl)
  alice.tell(Bot.Repeat("Hi"), bob)

  println("===================================")

  val person: ActorRef = actorSystem.actorOf(Props[Person], "person")
  val bankAccount: ActorRef = actorSystem.actorOf(Props[BankAccount], "bankAccount")
  person ! Deposit(10, bankAccount)
  person ! Withdraw(20, bankAccount)
  person ! Deposit(100, bankAccount)
  person ! Withdraw(50, bankAccount)
  person ! Statement(bankAccount)
}
