package com.akka.learn
package actors

import akka.actor._

import scala.annotation.tailrec
import scala.util.Random

class CharCounter extends Actor {

  import CharCounter._
  import CharCounterWorker._
  import CharCounterInvoker._

  val WORKERS_COUNT = 5

  val workers: Seq[ActorRef] =
    List
      .range(0, WORKERS_COUNT)
      .map(number => context.actorOf(Props[CharCounterWorker], s"worker-$number"))

  override def receive: Receive = onMessage(Map.empty, Map.empty)

  def sendToWorkers(invoker: ActorRef, string: String, char: Char): Int = {

    @tailrec
    def assignToWorker(parts: Seq[String], workerIndex: Int): Int =
      if (parts.isEmpty) workerIndex
      else {
        workers(workerIndex % workers.size) ! CountCharInPartRequest(invoker, parts.head, char)
        assignToWorker(parts.tail, workerIndex + 1)
      }

    assignToWorker(string.grouped(Math.ceil(string.length / WORKERS_COUNT).toInt).toList, 0)
  }

  def onMessage(pendingTasksCount: Map[ActorRef, Int], results: Map[ActorRef, Int]): Receive = {

    case request@ InitCountCharsRequest(invoker, string, char) =>
      if (pendingTasksCount.contains(invoker)) self ! request
      else context become onMessage(pendingTasksCount + (invoker -> sendToWorkers(invoker, string, char)), results)

    case CountCharInPartResponse(invoker, count) =>
      var newResults = results + (invoker -> (results.getOrElse(invoker, 0) + count))
      val newPendingTasksCount =
        if (pendingTasksCount(invoker) == 1) {
          invoker ! CountResponse(newResults(invoker))
          newResults = newResults - invoker
          pendingTasksCount - invoker
        }
        else pendingTasksCount + (invoker -> (pendingTasksCount(invoker) - 1))
      context become onMessage(newPendingTasksCount, newResults)
  }


}

class CharCounterWorker extends Actor {

  import CharCounterWorker._
  import CharCounter._

  override def receive: Receive = ready

  def calculateCount(string: String, char: Char): Int = {
    Thread.sleep(2000 + Random.nextInt(3000))
    string.count(_ == char)
  }

  def ready: Receive = {
    case CountCharInPartRequest(invoker, stringPart, char) =>
      sender() ! CountCharInPartResponse(invoker, calculateCount(stringPart, char))
  }
}

object CharCounterWorker {
  case class CountCharInPartRequest(invoker: ActorRef, stringPart: String, char: Char)
}
object CharCounter {
  case class InitCountCharsRequest(invoker: ActorRef, string: String, char: Char)
  case class CountCharInPartResponse(invoker: ActorRef, count: Int)
}

class CharCounterInvoker extends Actor {

  import CharCounterInvoker._

  override def receive: Receive = ready

  def ready: Receive = {
    case request@ CountChars(charCounter, string, char) =>
      charCounter ! CharCounter.InitCountCharsRequest(self, string, char)
      context become awaitingCount(request)
  }

  def awaitingCount(request: CountChars): Receive = {
    case request: CountChars => self ! request

    case PrintResults =>
      println(s"[${self.path}] Results are not ready yet")
      self ! PrintResults

    case CountResponse(count) =>
      context become countReceived(count, request)
      self ! PrintResults
  }

  def countReceived(count: Int, request: CountChars): Receive = {
    case request: CountChars =>
      self ! request

    case PrintResults =>
      println(s"${self.path}: ${request.string} has $count ${request.char}")
      context become ready
  }
}

object CharCounterInvoker {
  case class CountChars(charCounter: ActorRef, string: String, char: Char)
  case class CountResponse(count: Int)
  case object PrintResults
}

object ThreadingExercise extends App {

  val actorSystem: ActorSystem = ActorSystem("actorSystem")

  val counter: ActorRef = actorSystem.actorOf(Props[CharCounter], "counter")

  val charCounterInvoker1: ActorRef = actorSystem.actorOf(Props[CharCounterInvoker], "Invoker-1")
  val charCounterInvoker2: ActorRef = actorSystem.actorOf(Props[CharCounterInvoker], "Invoker-2")

  import CharCounterInvoker._
  charCounterInvoker1 ! CountChars(counter, "Hi very nice to meet you", 'e')
  charCounterInvoker1 ! CountChars(counter, "Hello world", 'l')
  charCounterInvoker2 ! CountChars(counter, "What can I say? haa?", 'a')
}
