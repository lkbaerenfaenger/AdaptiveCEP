package adaptivecep.graph.nodes

import adaptivecep.data.Events._
import adaptivecep.data.Queries.{Disjunction, DisjunctionQuery, HListQuery}
import adaptivecep.graph.nodes.traits.BinaryNode
import adaptivecep.graph.qos.MonitorFactory
import akka.actor.ActorRef

case class DisjunctionNode(
    query: DisjunctionQuery,
    publishers: Map[String, ActorRef],
    frequencyMonitorFactory: MonitorFactory,
    latencyMonitorFactory: MonitorFactory,
    createdCallback: Option[() => Any],
    eventCallback: Option[(Event) => Any])
  extends BinaryNode {

  var childNode1Created: Boolean = false
  var childNode2Created: Boolean = false

  def fillArray(desiredLength: Int, array: Array[Either[Any, Any]]): Array[Either[Any, Any]] = {
    require(array.length <= desiredLength)
    require(array.length > 0)
    val unit: Either[Unit, Unit] = array(0) match {
      case Left(_) => Left(())
      case Right(_) => Right(())
    }
    (0 until desiredLength).map(i => {
      if (i < array.length) {
        array(i)
      } else {
        unit
      }
    }).toArray
  }

  def handleEvent(array: Array[Either[Any, Any]]): Unit = {
    val disjunction = query.asInstanceOf[Disjunction[_, _, _]]
    val filledArray: Array[Either[Any, Any]] = fillArray(disjunction.length, array)
    disjunction.length match {
      case 1 => emitEvent(Event1(filledArray(0)))
      case 2 => emitEvent(Event2(filledArray(0), filledArray(1)))
      case 3 => emitEvent(Event3(filledArray(0), filledArray(1), filledArray(2)))
      case 4 => emitEvent(Event4(filledArray(0), filledArray(1), filledArray(2), filledArray(3)))
      case 5 => emitEvent(Event5(filledArray(0), filledArray(1), filledArray(2), filledArray(3), filledArray(4)))
      case 6 => emitEvent(Event6(filledArray(0), filledArray(1), filledArray(2), filledArray(3), filledArray(4), filledArray(5)))
    }
  }

  override def receive: Receive = {
    case DependenciesRequest =>
      sender ! DependenciesResponse(Seq(childNode1, childNode2))
    case Created if sender() == childNode1 =>
      childNode1Created = true
      if (childNode2Created) emitCreated()
    case Created if sender() == childNode2 =>
      childNode2Created = true
      if (childNode1Created) emitCreated()
    case event: Event if sender() == childNode1 => event match {
      case Event1(e1) => handleEvent(Array(Left(e1)))
      case Event2(e1, e2) => handleEvent(Array(Left(e1), Left(e2)))
      case Event3(e1, e2, e3) => handleEvent(Array(Left(e1), Left(e2), Left(e3)))
      case Event4(e1, e2, e3, e4) => handleEvent(Array(Left(e1), Left(e2), Left(e3), Left(e4)))
      case Event5(e1, e2, e3, e4, e5) => handleEvent(Array(Left(e1), Left(e2), Left(e3), Left(e4), Left(e5)))
      case Event6(e1, e2, e3, e4, e5, e6) => handleEvent(Array(Left(e1), Left(e2), Left(e3), Left(e4), Left(e5), Left(e6)))
    }
    case event: Event if sender() == childNode2 => event match {
      case Event1(e1) => handleEvent(Array(Right(e1)))
      case Event2(e1, e2) => handleEvent(Array(Right(e1), Right(e2)))
      case Event3(e1, e2, e3) => handleEvent(Array(Right(e1), Right(e2), Right(e3)))
      case Event4(e1, e2, e3, e4) => handleEvent(Array(Right(e1), Right(e2), Right(e3), Right(e4)))
      case Event5(e1, e2, e3, e4, e5) => handleEvent(Array(Right(e1), Right(e2), Right(e3), Right(e4), Right(e5)))
      case Event6(e1, e2, e3, e4, e5, e6) => handleEvent(Array(Right(e1), Right(e2), Right(e3), Right(e4), Right(e5), Right(e6)))
    }
    case unhandledMessage =>
      frequencyMonitor.onMessageReceive(unhandledMessage, nodeData)
      latencyMonitor.onMessageReceive(unhandledMessage, nodeData)
  }

}
