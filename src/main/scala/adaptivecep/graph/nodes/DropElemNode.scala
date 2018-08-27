package adaptivecep.graph.nodes

import adaptivecep.data.Events._
import adaptivecep.data.Queries._
import adaptivecep.graph.nodes.traits._
import adaptivecep.graph.qos._
import akka.actor.ActorRef

case class DropElemNode(
    query: DropElemQuery,
    publishers: Map[String, ActorRef],
    frequencyMonitorFactory: MonitorFactory,
    latencyMonitorFactory: MonitorFactory,
    createdCallback: Option[() => Any],
    eventCallback: Option[(Event) => Any])
  extends UnaryNode {

  def handleEvent(es: Seq[Any]): Unit = {
    val dropped = query match {
        case de@DropElem(_, _, _) =>
          es.patch(de.pos, Nil, 1)
        case de@DropElemRecord(_, _, _) => de.dropKey(es) match {
          case Some(vals) => vals
          case None => sys.error(errorMsg)
        }
    }
    emitEvent(Event(dropped: _*))
  }

  override def receive: Receive = {
    case DependenciesRequest =>
      sender ! DependenciesResponse(Seq(childNode))
    case Created if sender() == childNode =>
      emitCreated()
    case event: Event if sender() == childNode =>
      handleEvent(event.es)
    case unhandledMessage =>
      frequencyMonitor.onMessageReceive(unhandledMessage, nodeData)
      latencyMonitor.onMessageReceive(unhandledMessage, nodeData)
  }
}
