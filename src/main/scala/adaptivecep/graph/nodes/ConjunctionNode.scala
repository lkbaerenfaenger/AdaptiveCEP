package adaptivecep.graph.nodes

import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, Address, Deploy, PoisonPill, Props}
import com.espertech.esper.client._
import adaptivecep.data.Events._
import adaptivecep.data.Queries._
import adaptivecep.graph.nodes.traits._
import adaptivecep.graph.nodes.traits.EsperEngine._
import adaptivecep.graph.qos._
import akka.NotUsed
import akka.remote.RemoteScope
import akka.stream.{OverflowStrategy, SourceRef}
import akka.stream.scaladsl.{Sink, Source, SourceQueueWithComplete, StreamRefs}

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, FiniteDuration}

case class ConjunctionNode(
    requirements: Set[Requirement],
    queryLength1: Int,
    queryLength2: Int,
    publishers: Map[String, ActorRef],
    frequencyMonitorFactory: MonitorFactory,
    latencyMonitorFactory: MonitorFactory,
    createdCallback: Option[() => Any],
    eventCallback: Option[(Event) => Any])
  extends BinaryNode with EsperEngine {

  override val esperServiceProviderUri: String = name

  var childNode1Created: Boolean = false
  var childNode2Created: Boolean = false
  var parentReceived: Boolean = false

  override def receive: Receive = {
    case DependenciesRequest =>
      sender ! DependenciesResponse(Seq(childNode1, childNode2))
    case Created if sender() == childNode1 =>
      childNode1Created = true
    case Created if sender() == childNode2 =>
      childNode2Created = true
    case CentralizedCreated =>
      if(!created){
        created = true
        emitCreated()
      }
    case Parent(p1) => {
      parentNode = p1
      parentReceived = true
      nodeData = BinaryNodeData(name, requirements, context, childNode1, childNode2, parentNode)
    }
    case SourceRequest =>
      source = Source.queue[Event](20000, OverflowStrategy.dropNew).preMaterialize()(materializer)
      future = source._2.runWith(StreamRefs.sourceRef())(materializer)
      sourceRef = Await.result(future, Duration.Inf)

      sender() ! SourceResponse(sourceRef)
    case SourceResponse(ref) =>
      val s = sender()
      ref.getSource.to(Sink foreach(e =>{
        processEvent(e, s)
      })).run(materializer)
    case Child2(c1, c2) => {
      childNode1 = c1
      childNode2 = c2
      c1 ! SourceRequest
      c2 ! SourceRequest
      nodeData = BinaryNodeData(name, requirements, context, childNode1, childNode2, parentNode)
      emitCreated()
    }
    case ChildUpdate(old, a) => {
      emitCreated()
      if (childNode1.eq(old)) {
        childNode1 = a
      }
      if (childNode2.eq(old)) {
        childNode2 = a
      }
      nodeData = BinaryNodeData(name, requirements, context, childNode1, childNode2, parentNode)
    }
    case Controller(c) =>
      controller = c
    case KillMe => sender() ! PoisonPill
    case Kill =>
      scheduledTask.cancel()
      if(lmonitor.isDefined) lmonitor.get.scheduledTask.cancel()
    case CostReport(c) =>
      costs = c
      frequencyMonitor.onMessageReceive(CostReport(c), nodeData)
      latencyMonitor.onMessageReceive(CostReport(c), nodeData)
    case e: Event => processEvent(e, sender())
    case unhandledMessage =>
      frequencyMonitor.onMessageReceive(unhandledMessage, nodeData)
      latencyMonitor.onMessageReceive(unhandledMessage, nodeData)
  }

  def processEvent(event: Event, sender: ActorRef): Unit = {
    processedEvents += 1
    if(sender == childNode1){
            event match {
              case Event1(e1) => sendEvent("sq1", Array(toAnyRef(e1)))
              case Event2(e1, e2) => sendEvent("sq1", Array(toAnyRef(e1), toAnyRef(e2)))
              case Event3(e1, e2, e3) => sendEvent("sq1", Array(toAnyRef(e1), toAnyRef(e2), toAnyRef(e3)))
              case Event4(e1, e2, e3, e4) => sendEvent("sq1", Array(toAnyRef(e1), toAnyRef(e2), toAnyRef(e3), toAnyRef(e4)))
              case Event5(e1, e2, e3, e4, e5) => sendEvent("sq1", Array(toAnyRef(e1), toAnyRef(e2), toAnyRef(e3), toAnyRef(e4), toAnyRef(e5)))
              case Event6(e1, e2, e3, e4, e5, e6) => sendEvent("sq1", Array(toAnyRef(e1), toAnyRef(e2), toAnyRef(e3), toAnyRef(e4), toAnyRef(e5), toAnyRef(e6)))
            }}
    else if(sender == childNode2){
            event match {
              case Event1(e1) => sendEvent("sq2", Array(toAnyRef(e1)))
              case Event2(e1, e2) => sendEvent("sq2", Array(toAnyRef(e1), toAnyRef(e2)))
              case Event3(e1, e2, e3) => sendEvent("sq2", Array(toAnyRef(e1), toAnyRef(e2), toAnyRef(e3)))
              case Event4(e1, e2, e3, e4) => sendEvent("sq2", Array(toAnyRef(e1), toAnyRef(e2), toAnyRef(e3), toAnyRef(e4)))
              case Event5(e1, e2, e3, e4, e5) => sendEvent("sq2", Array(toAnyRef(e1), toAnyRef(e2), toAnyRef(e3), toAnyRef(e4), toAnyRef(e5)))
              case Event6(e1, e2, e3, e4, e5, e6) => sendEvent("sq2", Array(toAnyRef(e1), toAnyRef(e2), toAnyRef(e3), toAnyRef(e4), toAnyRef(e5), toAnyRef(e6)))
            }}
  }

  override def postStop(): Unit = {
    destroyServiceProvider()
  }
  addEventType("sq1", createArrayOfNames(queryLength1), createArrayOfClasses(queryLength1))
  addEventType("sq2", createArrayOfNames(queryLength2), createArrayOfClasses(queryLength2))

/*
  addEventType("sq1", createArrayOfNames(query.sq1), createArrayOfClasses(query.sq1))
  addEventType("sq2", createArrayOfNames(query.sq2), createArrayOfClasses(query.sq2))
*/
  val epStatement: EPStatement = createEpStatement("select * from pattern [every (sq1=sq1 and sq2=sq2)]")

  val updateListener: UpdateListener = (newEventBeans: Array[EventBean], _) => newEventBeans.foreach(eventBean => {
    val values: Array[Any] =
      eventBean.get("sq1").asInstanceOf[Array[Any]] ++
      eventBean.get("sq2").asInstanceOf[Array[Any]]
    val event: Event = values.length match {
      case 2 => Event2(values(0), values(1))
      case 3 => Event3(values(0), values(1), values(2))
      case 4 => Event4(values(0), values(1), values(2), values(3))
      case 5 => Event5(values(0), values(1), values(2), values(3), values(4))
      case 6 => Event6(values(0), values(1), values(2), values(3), values(4), values(5))
    }
    emitEvent(event)
  })

  epStatement.addListener(updateListener)

}
