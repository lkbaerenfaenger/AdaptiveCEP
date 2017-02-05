package com.scalarookie.eventscala

import akka.actor.{ActorRef, ActorSystem, PoisonPill, Props}
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike}
import com.scalarookie.eventscala.data.Events._
import com.scalarookie.eventscala.data.Queries._
import com.scalarookie.eventscala.dsl.Dsl._
import com.scalarookie.eventscala.graph.factory._
import com.scalarookie.eventscala.publishers._
import com.scalarookie.eventscala.graph.qos._

class GraphTests extends TestKit(ActorSystem()) with FunSuiteLike with BeforeAndAfterAll {

  def createTestPublisher(name: String): ActorRef =
    system.actorOf(Props(TestPublisher()), name)

  def createTestGraph(query: Query, publishers: Map[String, ActorRef], testActor: ActorRef): ActorRef = GraphFactory.createImpl(
    system,
    query,
    publishers,
    DummyMonitorFactory(),
    DummyMonitorFactory(),
    () => testActor ! Created,
    event => testActor ! event)

  // Source: http://doc.akka.io/docs/akka/current/scala/testing.html#Watching_Other_Actors_from_Probes
  def stopActor(actor: ActorRef): Unit = {
    val probe = TestProbe()
    probe watch actor
    actor ! PoisonPill
    probe.expectTerminated(actor)
  }

  def stopActors(actors: ActorRef*): Unit =
    actors.foreach(stopActor)

  override def afterAll(): Unit =
    system.terminate()

  test("LeafNode - StreamNode - 1") {
    val a: ActorRef = createTestPublisher("A")
    val query: Query1[String] = stream[String]("A")
    val graph: ActorRef = createTestGraph(query, Map("A" -> a), testActor)
    expectMsg(Created)
    a ! Event1("42")
    expectMsg(Event1("42"))
    stopActors(a, graph)
  }

  test("LeafNode - StreamNode - 2") {
    val a: ActorRef = createTestPublisher("A")
    val query: Query2[Int, Int] = stream[Int, Int]("A")
    val graph: ActorRef = createTestGraph(query, Map("A" -> a), testActor)
    expectMsg(Created)
    a ! Event2(42, 42)
    expectMsg(Event2(42, 42))
    stopActors(a, graph)
  }

  test("LeafNode - StreamNode - 3") {
    val a: ActorRef = createTestPublisher("A")
    val query: Query3[Long, Long, Long] = stream[Long, Long, Long]("A")
    val graph: ActorRef = createTestGraph(query, Map("A" -> a), testActor)
    expectMsg(Created)
    a ! Event3(42l, 42l, 42l)
    expectMsg(Event3(42l, 42l, 42l))
    stopActors(a, graph)
  }

  test("LeafNode - StreamNode - 4") {
    val a: ActorRef = createTestPublisher("A")
    val query: Query4[Float, Float, Float, Float] = stream[Float, Float, Float, Float]("A")
    val graph: ActorRef = createTestGraph(query, Map("A" -> a), testActor)
    expectMsg(Created)
    a ! Event4(42f, 42f, 42f, 42f)
    expectMsg(Event4(42f, 42f, 42f, 42f))
    stopActors(a, graph)
  }

  test("LeafNode - StreamNode - 5") {
    val a: ActorRef = createTestPublisher("A")
    val query: Query5[Double, Double, Double, Double, Double] = stream[Double, Double, Double, Double, Double]("A")
    val graph: ActorRef = createTestGraph(query, Map("A" -> a), testActor)
    expectMsg(Created)
    a ! Event5(42.0, 42.0, 42.0, 42.0, 42.0)
    expectMsg(Event5(42.0, 42.0, 42.0, 42.0, 42.0))
    stopActors(a, graph)
  }

  test("LeafNode - StreamNode - 6") {
    val a: ActorRef = createTestPublisher("A")
    val query: Query6[Boolean, Boolean, Boolean, Boolean, Boolean, Boolean] = stream[Boolean, Boolean, Boolean, Boolean, Boolean, Boolean]("A")
    val graph: ActorRef = createTestGraph(query, Map("A" -> a), testActor)
    expectMsg(Created)
    a ! Event6(true, true, true, true, true, true)
    expectMsg(Event6(true, true, true, true, true, true))
    stopActors(a, graph)
  }

  test("UnaryNode - FilterNode - 1") {
    val a: ActorRef = createTestPublisher("A")
    val query: Query2[Int, Int] =
      stream[Int, Int]("A")
      .keepEventsWith(_ >= _)
    val graph: ActorRef = createTestGraph(query, Map("A" -> a), testActor)
    expectMsg(Created)
    a ! Event2(41, 42)
    a ! Event2(42, 42)
    a ! Event2(43, 42)
    expectMsg(Event2(42, 42))
    expectMsg(Event2(43, 42))
    stopActors(a, graph)
  }

  test("UnaryNode - FilterNode - 2") {
    val a: ActorRef = createTestPublisher("A")
    val query: Query2[Int, Int] =
      stream[Int, Int]("A")
      .keepEventsWith(_ <= _)
    val graph: ActorRef = createTestGraph(query, Map("A" -> a), testActor)
    expectMsg(Created)
    a ! Event2(41, 42)
    a ! Event2(42, 42)
    a ! Event2(43, 42)
    expectMsg(Event2(41, 42))
    expectMsg(Event2(42, 42))
    stopActors(a, graph)
  }

  test("UnaryNode - FilterNode - 3") {
    val a: ActorRef = createTestPublisher("A")
    val query: Query1[Long] =
      stream[Long]("A")
      .keepEventsWith(_ == 42l)
    val graph: ActorRef = createTestGraph(query, Map("A" -> a), testActor)
    expectMsg(Created)
    a ! Event1(41l)
    a ! Event1(42l)
    expectMsg(Event1(42l))
    stopActors(a, graph)
  }

  test("UnaryNode - FilterNode - 4") {
    val a: ActorRef = createTestPublisher("A")
    val query: Query1[Float] =
      stream[Float]("A")
      .keepEventsWith(_ > 41f)
    val graph: ActorRef = createTestGraph(query, Map("A" -> a), testActor)
    expectMsg(Created)
    a ! Event1(41f)
    a ! Event1(42f)
    expectMsg(Event1(42f))
    stopActors(a, graph)
  }

  test("UnaryNode - FilterNode - 5") {
    val a: ActorRef = createTestPublisher("A")
    val query: Query1[Double] =
      stream[Double]("A")
      .keepEventsWith(_ < 42.0)
    val graph: ActorRef = createTestGraph(query, Map("A" -> a), testActor)
    expectMsg(Created)
    a ! Event1(41.0)
    a ! Event1(42.0)
    expectMsg(Event1(41.0))
    stopActors(a, graph)
  }

  test("UnaryNode - FilterNode - 6") {
    val a: ActorRef = createTestPublisher("A")
    val query: Query =
      stream[Boolean]("A")
      .keepEventsWith(_ != true)
    val graph: ActorRef = createTestGraph(query, Map("A" -> a), testActor)
    expectMsg(Created)
    a ! Event1(true)
    a ! Event1(false)
    expectMsg(Event1(false))
    stopActors(a, graph)
  }

  test("UnaryNode - SelectNode - 1") {
    val a: ActorRef = createTestPublisher("A")
    val query: Query1[Int] =
      stream[Int, Int]("A")
      .removeElement2()
    val graph: ActorRef = createTestGraph(query, Map("A" -> a), testActor)
    expectMsg(Created)
    a ! Event2(21, 42)
    a ! Event2(42, 21)
    expectMsg(Event1(21))
    expectMsg(Event1(42))
    stopActors(a, graph)
  }

  test("UnaryNode - SelectNode - 2") {
    val a: ActorRef = createTestPublisher("A")
    val query: Query2[String, String] =
      stream[String, String, String, String]("A")
      .removeElement1()
      .removeElement2()
    val graph: ActorRef = createTestGraph(query, Map("A" -> a), testActor)
    expectMsg(Created)
    a ! Event4("a", "b", "c", "d")
    a ! Event4("e", "f", "g", "h")
    expectMsg(Event2("b", "d"))
    expectMsg(Event2("f", "h"))
    stopActors(a, graph)
  }

  test("UnaryNode - SelfJoinNode - 1") {
    val a: ActorRef = createTestPublisher("A")
    val query: Query4[String, String, String, String] =
      stream[String, String]("A")
      .selfJoin(tumblingWindow(3.instances), tumblingWindow(2.instances))
    val graph: ActorRef = createTestGraph(query, Map("A" -> a), testActor)
    expectMsg(Created)
    a ! Event2("a", "b")
    a ! Event2("c", "d")
    a ! Event2("e", "f")
    expectMsg(Event4("a", "b", "a", "b"))
    expectMsg(Event4("a", "b", "c", "d"))
    expectMsg(Event4("c", "d", "a", "b"))
    expectMsg(Event4("c", "d", "c", "d"))
    expectMsg(Event4("e", "f", "a", "b"))
    expectMsg(Event4("e", "f", "c", "d"))
    stopActors(a, graph)
  }

  test("UnaryNode - SelfJoinNode - 2") {
    val a: ActorRef = createTestPublisher("A")
    val query: Query4[String, String, String, String] =
      stream[String, String]("A")
      .selfJoin(slidingWindow(3.instances), slidingWindow(2.instances))
    val graph: ActorRef = createTestGraph(query, Map("A" -> a), testActor)
    expectMsg(Created)
    a ! Event2("a", "b")
    a ! Event2("c", "d")
    a ! Event2("e", "f")
    expectMsg(Event4("a", "b", "a", "b"))
    expectMsg(Event4("c", "d", "a", "b"))
    expectMsg(Event4("c", "d", "c", "d"))
    expectMsg(Event4("a", "b", "c", "d"))
    expectMsg(Event4("e", "f", "c", "d"))
    expectMsg(Event4("e", "f", "e", "f"))
    expectMsg(Event4("a", "b", "e", "f"))
    expectMsg(Event4("c", "d", "e", "f"))
    stopActors(a, graph)
  }

  test("BinaryNode - JoinNode - 1") {
    val a: ActorRef = createTestPublisher("A")
    val b: ActorRef = createTestPublisher("B")
    val sq: Query2[Int, Int] = stream[Int, Int]("B")
    val query: Query5[String, Boolean, String, Int, Int] =
      stream[String, Boolean, String]("A")
      .join(sq, tumblingWindow(3.instances), tumblingWindow(2.instances))
    val graph: ActorRef = createTestGraph(query, Map("A" -> a, "B" -> b), testActor)
    expectMsg(Created)
    a ! Event3("a", true, "b")
    a ! Event3("c", true, "d")
    a ! Event3("e", true, "f")
    a ! Event3("g", true, "h")
    a ! Event3("i", true, "j")
    Thread.sleep(2000)
    b ! Event2(1, 2)
    b ! Event2(3, 4)
    b ! Event2(5, 6)
    b ! Event2(7, 8)
    expectMsg(Event5("a", true, "b", 1, 2))
    expectMsg(Event5("c", true, "d", 1, 2))
    expectMsg(Event5("e", true, "f", 1, 2))
    expectMsg(Event5("a", true, "b", 3, 4))
    expectMsg(Event5("c", true, "d", 3, 4))
    expectMsg(Event5("e", true, "f", 3, 4))
    expectMsg(Event5("a", true, "b", 5, 6))
    expectMsg(Event5("c", true, "d", 5, 6))
    expectMsg(Event5("e", true, "f", 5, 6))
    expectMsg(Event5("a", true, "b", 7, 8))
    expectMsg(Event5("c", true, "d", 7, 8))
    expectMsg(Event5("e", true, "f", 7, 8))
    stopActors(a, b, graph)
  }

  test("BinaryNode - JoinNode - 2") {
    val a: ActorRef = createTestPublisher("A")
    val b: ActorRef = createTestPublisher("B")
    val sq: Query2[Int, Int] = stream[Int, Int]("B")
    val query: Query5[String, Boolean, String, Int, Int] =
      stream[String, Boolean, String]("A")
      .join(sq, tumblingWindow(3.instances), tumblingWindow(2.instances))
    val graph: ActorRef = createTestGraph(query, Map("A" -> a, "B" -> b), testActor)
    expectMsg(Created)
    b ! Event2(1, 2)
    b ! Event2(3, 4)
    b ! Event2(5, 6)
    b ! Event2(7, 8)
    Thread.sleep(2000)
    a ! Event3("a", true, "b")
    a ! Event3("c", true, "d")
    a ! Event3("e", true, "f")
    a ! Event3("g", true, "h")
    a ! Event3("i", true, "j")
    expectMsg(Event5("a", true, "b", 5, 6))
    expectMsg(Event5("a", true, "b", 7, 8))
    expectMsg(Event5("c", true, "d", 5, 6))
    expectMsg(Event5("c", true, "d", 7, 8))
    expectMsg(Event5("e", true, "f", 5, 6))
    expectMsg(Event5("e", true, "f", 7, 8))
    stopActors(a, b, graph)
  }

  test ("BinaryNode - JoinNode - 3") {
    val a: ActorRef = createTestPublisher("A")
    val b: ActorRef = createTestPublisher("B")
    val sq: Query2[Int, Int] = stream[Int, Int]("B")
    val query: Query5[String, Boolean, String, Int, Int] =
      stream[String, Boolean, String]("A")
      .join(sq, slidingWindow(3.instances), slidingWindow(2.instances))
    val graph: ActorRef = createTestGraph(query, Map("A" -> a, "B" -> b), testActor)
    expectMsg(Created)
    a ! Event3("a", true, "b")
    a ! Event3("c", true, "d")
    a ! Event3("e", true, "f")
    a ! Event3("g", true, "h")
    a ! Event3("i", true, "j")
    Thread.sleep(2000)
    b ! Event2(1, 2)
    b ! Event2(3, 4)
    b ! Event2(5, 6)
    b ! Event2(7, 8)
    expectMsg(Event5("e", true, "f", 1, 2))
    expectMsg(Event5("g", true, "h", 1, 2))
    expectMsg(Event5("i", true, "j", 1, 2))
    expectMsg(Event5("e", true, "f", 3, 4))
    expectMsg(Event5("g", true, "h", 3, 4))
    expectMsg(Event5("i", true, "j", 3, 4))
    expectMsg(Event5("e", true, "f", 5, 6))
    expectMsg(Event5("g", true, "h", 5, 6))
    expectMsg(Event5("i", true, "j", 5, 6))
    expectMsg(Event5("e", true, "f", 7, 8))
    expectMsg(Event5("g", true, "h", 7, 8))
    expectMsg(Event5("i", true, "j", 7, 8))
    stopActors(a, b, graph)
  }

  test("BinaryNode - JoinNode - 4") {
    val a: ActorRef = createTestPublisher("A")
    val b: ActorRef = createTestPublisher("B")
    val sq: Query2[Int, Int] = stream[Int, Int]("B")
    val query: Query5[String, Boolean, String, Int, Int] =
      stream[String, Boolean, String]("A")
      .join(sq, slidingWindow(3.instances), slidingWindow(2.instances))
    val graph: ActorRef = createTestGraph(query, Map("A" -> a, "B" -> b), testActor)
    expectMsg(Created)
    b ! Event2(1, 2)
    b ! Event2(3, 4)
    b ! Event2(5, 6)
    b ! Event2(7, 8)
    Thread.sleep(2000)
    a ! Event3("a", true, "b")
    a ! Event3("c", true, "d")
    a ! Event3("e", true, "f")
    a ! Event3("g", true, "h")
    a ! Event3("i", true, "j")
    expectMsg(Event5("a", true, "b", 5, 6))
    expectMsg(Event5("a", true, "b", 7, 8))
    expectMsg(Event5("c", true, "d", 5, 6))
    expectMsg(Event5("c", true, "d", 7, 8))
    expectMsg(Event5("e", true, "f", 5, 6))
    expectMsg(Event5("e", true, "f", 7, 8))
    expectMsg(Event5("g", true, "h", 5, 6))
    expectMsg(Event5("g", true, "h", 7, 8))
    expectMsg(Event5("i", true, "j", 5, 6))
    expectMsg(Event5("i", true, "j", 7, 8))
    stopActors(a, b, graph)
  }

  test("DisjunctionNode - 1") {
    val a: ActorRef = createTestPublisher("A")
    val b: ActorRef = createTestPublisher("B")
    val query: Query2[Either[Int, String], Either[Int, String]] =
      stream[Int, Int]("A")
      .or(stream[String, String]("B"))
    val graph: ActorRef = createTestGraph(query, Map("A" -> a, "B" -> b), testActor)
    expectMsg(Created)
    a ! Event2(21, 42)
    Thread.sleep(2000)
    b ! Event2("21", "42")
    expectMsg(Event2(Left(21), Left(42)))
    expectMsg(Event2(Right("21"), Right("42")))
    stopActors(a, b, graph)
  }

  test("DisjunctionNode - 2") {
    val a: ActorRef = createTestPublisher("A")
    val b: ActorRef = createTestPublisher("B")
    val c: ActorRef = createTestPublisher("C")
    val query:
      Query3[Either[Either[Int, String], Boolean],
             Either[Either[Int, String], Boolean],
             Either[Unit,                Boolean]] =
      stream[Int, Int]("A")
        .or(stream[String, String]("B"))
        .or(stream[Boolean, Boolean, Boolean]("C"))
    val graph: ActorRef = createTestGraph(query, Map("A" -> a, "B" -> b, "C" -> c), testActor)
    expectMsg(Created)
    a ! Event2(21, 42)
    Thread.sleep(2000)
    b ! Event2("21", "42")
    Thread.sleep(2000)
    c ! Event3(true, false, true)
    expectMsg(Event3(Left(Left(21)), Left(Left(42)), Left(())))
    expectMsg(Event3(Left(Right("21")), Left(Right("42")), Left(())))
    expectMsg(Event3(Right(true), Right(false), Right(true)))
    stopActors(a, b, c, graph)
  }

  test("Complex") {
    val a: ActorRef = createTestPublisher("A")
    val b: ActorRef = createTestPublisher("B")
    val c: ActorRef = createTestPublisher("C")
    val sq1: Query2[String, String] = stream[String, String]("A")
    val sq2: Query2[Int, Int] = stream[Int, Int]("B")
    val sq3: Query1[String] = stream[String]("C")
    val sq4: Query4[String, String, Int, Int] =
      sq1.join(sq2, tumblingWindow(3.instances), tumblingWindow(2.instances))
    val sq5: Query2[String, String] =
      sq3.selfJoin(tumblingWindow(3.instances), tumblingWindow(2.instances))
    val sq6: Query6[String, String, Int, Int, String, String] =
      sq4.join(sq5, tumblingWindow(1.instances), tumblingWindow(4.instances))
    val sq7: Query6[String, String, Int, Int, String, String] =
      sq6.keepEventsWith((_, _, e3, e4, _, _) => e3 < e4)
    val query: Query2[String, String] =
      sq7
      .removeElement2()
      .removeElement2()
      .removeElement2()
      .removeElement2()
    val graph: ActorRef = createTestGraph(query, Map("A" -> a, "B" -> b, "C" -> c), testActor)
    expectMsg(Created)
    b ! Event2(1, 2)
    b ! Event2(3, 4)
    b ! Event2(5, 6)
    b ! Event2(7, 8)
    Thread.sleep(2000)
    a ! Event2("a", "b")
    a ! Event2("c", "d")
    a ! Event2("e", "f")
    a ! Event2("g", "h")
    a ! Event2("i", "j")
    Thread.sleep(2000)
    c ! Event1("a")
    c ! Event1("b")
    c ! Event1("c")
    expectMsg(Event2("e", "a"))
    expectMsg(Event2("e", "b"))
    expectMsg(Event2("e", "a"))
    expectMsg(Event2("e", "b"))
    stopActors(a, b, c, graph)
  }

}