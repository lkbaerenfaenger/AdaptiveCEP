package adaptivecep

import adaptivecep.data.Events._
import adaptivecep.data.Queries._
import adaptivecep.dsl.Dsl._
import adaptivecep.graph.factory._
import adaptivecep.graph.qos._
import adaptivecep.publishers._
import akka.actor.{ActorRef, ActorSystem, PoisonPill, Props}
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike}
import shapeless.{::, HNil, Nat}

class GraphTests extends TestKit(ActorSystem()) with FunSuiteLike with BeforeAndAfterAll {

  def createTestPublisher(name: String): ActorRef = {
    system.actorOf(Props(TestPublisher()), name)
  }

  def createTestGraph(query: Query, publishers: Map[String, ActorRef], testActor: ActorRef): ActorRef = GraphFactory.createImpl(
    system,
    query,
    publishers,
    DummyMonitorFactory(),
    DummyMonitorFactory(),
    () => testActor ! Created,
    event => testActor ! event)

  // The following method implementation is taken straight out of the Akka docs:
  // http://doc.akka.io/docs/akka/current/scala/testing.html#Watching_Other_Actors_from_Probes
  def stopActor(actor: ActorRef): Unit = {
    val probe = TestProbe()
    probe watch actor
    actor ! PoisonPill
    probe.expectTerminated(actor)
  }

  def stopActors(actors: ActorRef*): Unit = {
    actors.foreach(stopActor)
  }

  override def afterAll(): Unit = {
    system.terminate()
  }

  test("LeafNode - StreamNode - 1") {
    val a: ActorRef = createTestPublisher("A")
    val query: HListQuery[String::HNil] = stream[String::HNil]("A")
    val graph: ActorRef = createTestGraph(query, Map("A" -> a), testActor)
    expectMsg(Created)
    a ! Event("42")
    expectMsg(Event("42"))
    stopActors(a, graph)
  }

  test("LeafNode - StreamNode - 2") {
    val a: ActorRef = createTestPublisher("A")
    val query: HListQuery[Int::Int::HNil] = stream[Int::Int::HNil]("A")
    val graph: ActorRef = createTestGraph(query, Map("A" -> a), testActor)
    expectMsg(Created)
    a ! Event(42, 42)
    expectMsg(Event(42, 42))
    stopActors(a, graph)
  }

  test("LeafNode - StreamNode - 3") {
    val a: ActorRef = createTestPublisher("A")
    val query: HListQuery[Long::Long::Long::HNil] = stream[Long::Long::Long::HNil]("A")
    val graph: ActorRef = createTestGraph(query, Map("A" -> a), testActor)
    expectMsg(Created)
    a ! Event(42l, 42l, 42l)
    expectMsg(Event(42l, 42l, 42l))
    stopActors(a, graph)
  }

  test("LeafNode - StreamNode - 4") {
    val a: ActorRef = createTestPublisher("A")
    val query: HListQuery[Float::Float::Float::Float::HNil] = stream[Float::Float::Float::Float::HNil]("A")
    val graph: ActorRef = createTestGraph(query, Map("A" -> a), testActor)
    expectMsg(Created)
    a ! Event(42f, 42f, 42f, 42f)
    expectMsg(Event(42f, 42f, 42f, 42f))
    stopActors(a, graph)
  }

  test("LeafNode - StreamNode - 5") {
    val a: ActorRef = createTestPublisher("A")
    val query: HListQuery[Double::Double::Double::Double::Double::HNil] =
      stream[Double::Double::Double::Double::Double::HNil]("A")
    val graph: ActorRef = createTestGraph(query, Map("A" -> a), testActor)
    expectMsg(Created)
    a ! Event(42.0, 42.0, 42.0, 42.0, 42.0)
    expectMsg(Event(42.0, 42.0, 42.0, 42.0, 42.0))
    stopActors(a, graph)
  }

  test("LeafNode - StreamNode - 6") {
    val a: ActorRef = createTestPublisher("A")
    val query: HListQuery[Boolean::Boolean::Boolean::Boolean::Boolean::Boolean::HNil] =
      stream[Boolean::Boolean::Boolean::Boolean::Boolean::Boolean::HNil]("A")
    val graph: ActorRef = createTestGraph(query, Map("A" -> a), testActor)
    expectMsg(Created)
    a ! Event(true, true, true, true, true, true)
    expectMsg(Event(true, true, true, true, true, true))
    stopActors(a, graph)
  }


  test("LeafNode - SequenceNode - 1") {
    val a: ActorRef = createTestPublisher("A")
    val b: ActorRef = createTestPublisher("B")
    val query: HListQuery[Int::Int::String::String::HNil] =
      sequence(nStream[Int::Int::HNil]("A") -> nStream[String::String::HNil]("B"))
    val graph: ActorRef = createTestGraph(query, Map("A" -> a, "B" -> b), testActor)
    expectMsg(Created)
    a ! Event(21, 42)
    Thread.sleep(2000)
    b ! Event("21", "42")
    expectMsg(Event(21, 42, "21", "42"))
    stopActors(a, b, graph)
  }

  test("LeafNode - SequenceNode - 2") {
    val a: ActorRef = createTestPublisher("A")
    val b: ActorRef = createTestPublisher("B")
    val query: HListQuery[Int::Int::String::String::HNil] =
      sequence(nStream[Int::Int::HNil]("A") -> nStream[String::String::HNil]("B"))
    val graph: ActorRef = createTestGraph(query, Map("A" -> a, "B" -> b), testActor)
    expectMsg(Created)
    a ! Event(1, 1)
    Thread.sleep(2000)
    a ! Event(2, 2)
    Thread.sleep(2000)
    a ! Event(3, 3)
    Thread.sleep(2000)
    b ! Event("1", "1")
    Thread.sleep(2000)
    b ! Event("2", "2")
    Thread.sleep(2000)
    b ! Event("3", "3")
    expectMsg(Event(1, 1, "1", "1"))
    stopActors(a, b, graph)
  }

  test("UnaryNode - FilterNode - 1") {
    val a: ActorRef = createTestPublisher("A")
    val query: HListQuery[Int::Int::HNil] =
      stream[Int::Int::HNil]("A")
      .where(x => x.head >= x.last)
    val graph: ActorRef = createTestGraph(query, Map("A" -> a), testActor)
    expectMsg(Created)
    a ! Event(41, 42)
    a ! Event(42, 42)
    a ! Event(43, 42)
    expectMsg(Event(42, 42))
    expectMsg(Event(43, 42))
    stopActors(a, graph)
  }

  test("UnaryNode - FilterNode - 2") {
    val a: ActorRef = createTestPublisher("A")
    val query: HListQuery[Int::Int::HNil] =
      stream[Int::Int::HNil]("A")
      .where(x => x.head <= x.last)
    val graph: ActorRef = createTestGraph(query, Map("A" -> a), testActor)
    expectMsg(Created)
    a ! Event(41, 42)
    a ! Event(42, 42)
    a ! Event(43, 42)
    expectMsg(Event(41, 42))
    expectMsg(Event(42, 42))
    stopActors(a, graph)
  }

  test("UnaryNode - FilterNode - 3") {
    val a: ActorRef = createTestPublisher("A")
    val query: HListQuery[Long::HNil] =
      stream[Long::HNil]("A")
      .where(_.head == 42l)
    val graph: ActorRef = createTestGraph(query, Map("A" -> a), testActor)
    expectMsg(Created)
    a ! Event(41l)
    a ! Event(42l)
    expectMsg(Event(42l))
    stopActors(a, graph)
  }
  test("UnaryNode - FilterNode - 4") {
    val a: ActorRef = createTestPublisher("A")
    val query: HListQuery[Float::HNil] =
      stream[Float::HNil]("A")
      .where(_.head > 41f)
    val graph: ActorRef = createTestGraph(query, Map("A" -> a), testActor)
    expectMsg(Created)
    a ! Event(41f)
    a ! Event(42f)
    expectMsg(Event(42f))
    stopActors(a, graph)
  }

  test("UnaryNode - FilterNode - 5") {
    val a: ActorRef = createTestPublisher("A")
    val query: HListQuery[Double::HNil] =
      stream[Double::HNil]("A")
      .where(_.head < 42.0)
    val graph: ActorRef = createTestGraph(query, Map("A" -> a), testActor)
    expectMsg(Created)
    a ! Event(41.0)
    a ! Event(42.0)
    expectMsg(Event(41.0))
    stopActors(a, graph)
  }

  test("UnaryNode - FilterNode - 6") {
    val a: ActorRef = createTestPublisher("A")
    val query: Query =
      stream[Boolean::HNil]("A")
      .where(_.head != true)
    val graph: ActorRef = createTestGraph(query, Map("A" -> a), testActor)
    expectMsg(Created)
    a ! Event(true)
    a ! Event(false)
    expectMsg(Event(false))
    stopActors(a, graph)
  }

  test("UnaryNode - DropElemNode - 1") {
    val a: ActorRef = createTestPublisher("A")
    val query: HListQuery[Int::HNil] =
      stream[Int::Int::HNil]("A")
        .drop(Nat._2)
    val graph: ActorRef = createTestGraph(query, Map("A" -> a), testActor)
    expectMsg(Created)
    a ! Event(21, 42)
    a ! Event(42, 21)
    expectMsg(Event(21))
    expectMsg(Event(42))
    stopActors(a, graph)
  }

  test("UnaryNode - DropElemNode - 2") {
    val a: ActorRef = createTestPublisher("A")
    val query: HListQuery[String::String::HNil] =
      stream[String::String::String::String::HNil]("A")
      .drop(Nat._1)
      .drop(Nat._2)
    val graph: ActorRef = createTestGraph(query, Map("A" -> a), testActor)
    expectMsg(Created)
    a ! Event("a", "b", "c", "d")
    a ! Event("e", "f", "g", "h")
    expectMsg(Event("b", "d"))
    expectMsg(Event("f", "h"))
    stopActors(a, graph)
  }

  test("UnaryNode - SelfJoinNode - 1") {
    val a: ActorRef = createTestPublisher("A")
    val query: HListQuery[String::String::String::String::HNil] =
      stream[String::String::HNil]("A")
      .selfJoin(tumblingWindow(3.instances), tumblingWindow(2.instances))
    val graph: ActorRef = createTestGraph(query, Map("A" -> a), testActor)
    expectMsg(Created)
    a ! Event("a", "b")
    a ! Event("c", "d")
    a ! Event("e", "f")
    expectMsg(Event("a", "b", "a", "b"))
    expectMsg(Event("a", "b", "c", "d"))
    expectMsg(Event("c", "d", "a", "b"))
    expectMsg(Event("c", "d", "c", "d"))
    expectMsg(Event("e", "f", "a", "b"))
    expectMsg(Event("e", "f", "c", "d"))
    stopActors(a, graph)
  }

  test("UnaryNode - SelfJoinNode - 2") {
    val a: ActorRef = createTestPublisher("A")
    val query: HListQuery[String::String::String::String::HNil] =
      stream[String::String::HNil]("A")
      .selfJoin(slidingWindow(3.instances), slidingWindow(2.instances))
    val graph: ActorRef = createTestGraph(query, Map("A" -> a), testActor)
    expectMsg(Created)
    a ! Event("a", "b")
    a ! Event("c", "d")
    a ! Event("e", "f")
    expectMsg(Event("a", "b", "a", "b"))
    expectMsg(Event("c", "d", "a", "b"))
    expectMsg(Event("c", "d", "c", "d"))
    expectMsg(Event("a", "b", "c", "d"))
    expectMsg(Event("e", "f", "c", "d"))
    expectMsg(Event("e", "f", "e", "f"))
    expectMsg(Event("a", "b", "e", "f"))
    expectMsg(Event("c", "d", "e", "f"))
    stopActors(a, graph)
  }

  test("BinaryNode - JoinNode - 1") {
    val a: ActorRef = createTestPublisher("A")
    val b: ActorRef = createTestPublisher("B")
    val sq: HListQuery[Int::Int::HNil] = stream[Int::Int::HNil]("B")
    val query: HListQuery[String::Boolean::String::Int::Int::HNil] =
      stream[String::Boolean::String::HNil]("A")
      .join(sq, tumblingWindow(3.instances), tumblingWindow(2.instances))
    val graph: ActorRef = createTestGraph(query, Map("A" -> a, "B" -> b), testActor)
    expectMsg(Created)
    a ! Event("a", true, "b")
    a ! Event("c", true, "d")
    a ! Event("e", true, "f")
    a ! Event("g", true, "h")
    a ! Event("i", true, "j")
    Thread.sleep(2000)
    b ! Event(1, 2)
    b ! Event(3, 4)
    b ! Event(5, 6)
    b ! Event(7, 8)
    expectMsg(Event("a", true, "b", 1, 2))
    expectMsg(Event("c", true, "d", 1, 2))
    expectMsg(Event("e", true, "f", 1, 2))
    expectMsg(Event("a", true, "b", 3, 4))
    expectMsg(Event("c", true, "d", 3, 4))
    expectMsg(Event("e", true, "f", 3, 4))
    expectMsg(Event("a", true, "b", 5, 6))
    expectMsg(Event("c", true, "d", 5, 6))
    expectMsg(Event("e", true, "f", 5, 6))
    expectMsg(Event("a", true, "b", 7, 8))
    expectMsg(Event("c", true, "d", 7, 8))
    expectMsg(Event("e", true, "f", 7, 8))
    stopActors(a, b, graph)
  }

  test("BinaryNode - JoinNode - 2") {
    val a: ActorRef = createTestPublisher("A")
    val b: ActorRef = createTestPublisher("B")
    val sq: HListQuery[Int::Int::HNil] = stream[Int::Int::HNil]("B")
    val query: HListQuery[String::Boolean::String::Int::Int::HNil] =
      stream[String::Boolean::String::HNil]("A")
      .join(sq, tumblingWindow(3.instances), tumblingWindow(2.instances))
    val graph: ActorRef = createTestGraph(query, Map("A" -> a, "B" -> b), testActor)
    expectMsg(Created)
    b ! Event(1, 2)
    b ! Event(3, 4)
    b ! Event(5, 6)
    b ! Event(7, 8)
    Thread.sleep(2000)
    a ! Event("a", true, "b")
    a ! Event("c", true, "d")
    a ! Event("e", true, "f")
    a ! Event("g", true, "h")
    a ! Event("i", true, "j")
    expectMsg(Event("a", true, "b", 5, 6))
    expectMsg(Event("a", true, "b", 7, 8))
    expectMsg(Event("c", true, "d", 5, 6))
    expectMsg(Event("c", true, "d", 7, 8))
    expectMsg(Event("e", true, "f", 5, 6))
    expectMsg(Event("e", true, "f", 7, 8))
    stopActors(a, b, graph)
  }

  test ("BinaryNode - JoinNode - 3") {
    val a: ActorRef = createTestPublisher("A")
    val b: ActorRef = createTestPublisher("B")
    val sq: HListQuery[Int::Int::HNil] = stream[Int::Int::HNil]("B")
    val query: HListQuery[String::Boolean::String::Int::Int::HNil] =
      stream[String::Boolean::String::HNil]("A")
      .join(sq, slidingWindow(3.instances), slidingWindow(2.instances))
    val graph: ActorRef = createTestGraph(query, Map("A" -> a, "B" -> b), testActor)
    expectMsg(Created)
    a ! Event("a", true, "b")
    a ! Event("c", true, "d")
    a ! Event("e", true, "f")
    a ! Event("g", true, "h")
    a ! Event("i", true, "j")
    Thread.sleep(2000)
    b ! Event(1, 2)
    b ! Event(3, 4)
    b ! Event(5, 6)
    b ! Event(7, 8)
    expectMsg(Event("e", true, "f", 1, 2))
    expectMsg(Event("g", true, "h", 1, 2))
    expectMsg(Event("i", true, "j", 1, 2))
    expectMsg(Event("e", true, "f", 3, 4))
    expectMsg(Event("g", true, "h", 3, 4))
    expectMsg(Event("i", true, "j", 3, 4))
    expectMsg(Event("e", true, "f", 5, 6))
    expectMsg(Event("g", true, "h", 5, 6))
    expectMsg(Event("i", true, "j", 5, 6))
    expectMsg(Event("e", true, "f", 7, 8))
    expectMsg(Event("g", true, "h", 7, 8))
    expectMsg(Event("i", true, "j", 7, 8))
    stopActors(a, b, graph)
  }

  test("BinaryNode - JoinNode - 4") {
    val a: ActorRef = createTestPublisher("A")
    val b: ActorRef = createTestPublisher("B")
    val sq: HListQuery[Int::Int::HNil] = stream[Int::Int::HNil]("B")
    val query: HListQuery[String::Boolean::String::Int::Int::HNil] =
      stream[String::Boolean::String::HNil]("A")
      .join(sq, slidingWindow(3.instances), slidingWindow(2.instances))
    val graph: ActorRef = createTestGraph(query, Map("A" -> a, "B" -> b), testActor)
    expectMsg(Created)
    b ! Event(1, 2)
    b ! Event(3, 4)
    b ! Event(5, 6)
    b ! Event(7, 8)
    Thread.sleep(2000)
    a ! Event("a", true, "b")
    a ! Event("c", true, "d")
    a ! Event("e", true, "f")
    a ! Event("g", true, "h")
    a ! Event("i", true, "j")
    expectMsg(Event("a", true, "b", 5, 6))
    expectMsg(Event("a", true, "b", 7, 8))
    expectMsg(Event("c", true, "d", 5, 6))
    expectMsg(Event("c", true, "d", 7, 8))
    expectMsg(Event("e", true, "f", 5, 6))
    expectMsg(Event("e", true, "f", 7, 8))
    expectMsg(Event("g", true, "h", 5, 6))
    expectMsg(Event("g", true, "h", 7, 8))
    expectMsg(Event("i", true, "j", 5, 6))
    expectMsg(Event("i", true, "j", 7, 8))
    stopActors(a, b, graph)
  }

  test("Binary Node - ConjunctionNode - 1") {
    val a: ActorRef = createTestPublisher("A")
    val b: ActorRef = createTestPublisher("B")
    val query: HListQuery[Int::Float::HNil] =
      stream[Int::HNil]("A")
      .and(stream[Float::HNil]("B"))
    val graph: ActorRef = createTestGraph(query, Map("A" -> a, "B" -> b), testActor)
    expectMsg(Created)
    a ! Event(21)
    b ! Event(21.0f)
    Thread.sleep(2000)
    a ! Event(42)
    b ! Event(42.0f)
    expectMsg(Event(21, 21.0f))
    expectMsg(Event(42, 42.0f))
    stopActors(a, b, graph)
  }

  test("Binary Node - ConjunctionNode - 2") {
    val a: ActorRef = createTestPublisher("A")
    val b: ActorRef = createTestPublisher("B")
    val query: HListQuery[Int::Float::HNil] =
      stream[Int::HNil]("A")
      .and(stream[Float::HNil]("B"))
    val graph: ActorRef = createTestGraph(query, Map("A" -> a, "B" -> b), testActor)
    expectMsg(Created)
    a ! Event(21)
    a ! Event(42)
    Thread.sleep(2000)
    b ! Event(21.0f)
    b ! Event(42.0f)
    expectMsg(Event(21, 21.0f))
    stopActors(a, b, graph)
  }

  test("Binary Node - DisjunctionNode - 1") {
    val a: ActorRef = createTestPublisher("A")
    val b: ActorRef = createTestPublisher("B")
    val query: HListQuery[Either[Int, String]::Either[Int, String]::HNil] =
      stream[Int::Int::HNil]("A")
      .or(stream[String::String::HNil]("B"))
    val graph: ActorRef = createTestGraph(query, Map("A" -> a, "B" -> b), testActor)
    expectMsg(Created)
    a ! Event(21, 42)
    Thread.sleep(2000)
    b ! Event("21", "42")
    expectMsg(Event(Left(21), Left(42)))
    expectMsg(Event(Right("21"), Right("42")))
    stopActors(a, b, graph)
  }

  test("Binary Node - DisjunctionNode - 2") {
    val a: ActorRef = createTestPublisher("A")
    val b: ActorRef = createTestPublisher("B")
    val c: ActorRef = createTestPublisher("C")
    val query: HListQuery[Either[Either[Int, String], Boolean]:: Either[Either[Int, String], Boolean]:: Either[Unit,Boolean]::HNil] =
      stream[Int::Int::HNil]("A")
        .or(stream[String::String::HNil]("B"))
        .or(stream[Boolean::Boolean::Boolean::HNil]("C"))
    val graph: ActorRef = createTestGraph(query, Map("A" -> a, "B" -> b, "C" -> c), testActor)
    expectMsg(Created)
    a ! Event(21, 42)
    Thread.sleep(2000)
    b ! Event("21", "42")
    Thread.sleep(2000)
    c ! Event(true, false, true)
    expectMsg(Event(Left(Left(21)), Left(Left(42)), Left(())))
    expectMsg(Event(Left(Right("21")), Left(Right("42")), Left(())))
    expectMsg(Event(Right(true), Right(false), Right(true)))
    stopActors(a, b, c, graph)
  }

  test("Nested - SP operators") {
    val a: ActorRef = createTestPublisher("A")
    val b: ActorRef = createTestPublisher("B")
    val c: ActorRef = createTestPublisher("C")
    val sq1: HListQuery[String::String::HNil] =
      stream[String::String::HNil]("A")
    val sq2: HListQuery[Int::Int::HNil] =
      stream[Int::Int::HNil]("B")
    val sq3: HListQuery[String::HNil] =
      stream[String::HNil]("C")
    val sq4: HListQuery[String::String::Int::Int::HNil] =
      sq1.join(sq2, tumblingWindow(3.instances), tumblingWindow(2.instances))
    val sq5: HListQuery[String::String::HNil] =
      sq3.selfJoin(tumblingWindow(3.instances), tumblingWindow(2.instances))
    val sq6: HListQuery[String::String::Int::Int::String::String::HNil] =
      sq4.join(sq5, tumblingWindow(1.instances), tumblingWindow(4.instances))
    val sq7: HListQuery[String::String::Int::Int::String::String::HNil] =
      sq6.where(x => x(Nat._2) < x(Nat._3))
    val query: HListQuery[String::String::HNil] =
      sq7
        .drop(Nat._2)
        .drop(Nat._2)
        .drop(Nat._2)
        .drop(Nat._2)
    val graph: ActorRef = createTestGraph(query, Map("A" -> a, "B" -> b, "C" -> c), testActor)
    expectMsg(Created)
    b ! Event(1, 2)
    b ! Event(3, 4)
    b ! Event(5, 6)
    b ! Event(7, 8)
    Thread.sleep(2000)
    a ! Event("a", "b")
    a ! Event("c", "d")
    a ! Event("e", "f")
    a ! Event("g", "h")
    a ! Event("i", "j")
    Thread.sleep(2000)
    c ! Event("a")
    c ! Event("b")
    c ! Event("c")
    expectMsg(Event("e", "a"))
    expectMsg(Event("e", "b"))
    expectMsg(Event("e", "a"))
    expectMsg(Event("e", "b"))
    stopActors(a, b, c, graph)
  }

  test("Nested - CEP operators") {
    val a: ActorRef = createTestPublisher("A")
    val b: ActorRef = createTestPublisher("B")
    val c: ActorRef = createTestPublisher("C")
    val query: HListQuery[Either[Int, Float]::Either[Float, Boolean]::HNil] =
      stream[Int::HNil]("A")
      .and(stream[Float::HNil]("B"))
      .or(sequence(nStream[Float::HNil]("B") -> nStream[Boolean::HNil]("C")))
    val graph: ActorRef = createTestGraph(query, Map("A" -> a, "B" -> b, "C" -> c), testActor)
    expectMsg(Created)
    a ! Event(21)
    a ! Event(42)
    Thread.sleep(2000)
    b ! Event(21.0f)
    b ! Event(42.0f)
    Thread.sleep(2000)
    c ! Event(true)
    expectMsg(Event(Left(21), Left(21.0f)))
    expectMsg(Event(Right(21.0f), Right(true)))
    stopActors(a, b, graph)
  }
}
