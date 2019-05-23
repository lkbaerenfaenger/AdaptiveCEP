package adaptivecep.graph.nodes

import adaptivecep.data.Events._
import adaptivecep.data.Queries._
import adaptivecep.graph.nodes.traits._
import adaptivecep.graph.qos._
import adaptivecep.publishers.Publisher._
import akka.NotUsed
import akka.actor.{ActorRef, PoisonPill}
import akka.stream.{KillSwitches, OverflowStrategy}
import akka.stream.scaladsl.{Keep, Sink, Source, StreamRefs}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

case class StreamNode(
    //query: StreamQuery,
    requirements: Set[Requirement],
    publisherName: String,
    publishers: Map[String, ActorRef],
    frequencyMonitorFactory: MonitorFactory,
    latencyMonitorFactory: MonitorFactory,
    bandwidthMonitorFactory: MonitorFactory,
    createdCallback: Option[() => Any],
    eventCallback: Option[(Event) => Any])
  extends LeafNode {

  val publisher: ActorRef = publishers(publisherName)
  var subscriptionAcknowledged: Boolean = false
  var parentReceived: Boolean = false

  publisher ! Subscribe
  println("subscribing to publisher", publisher.path)

  override def receive: Receive = {
    case DependenciesRequest =>
      sender ! DependenciesResponse(Seq.empty)
    case AcknowledgeSubscription(ref) if sender() == publisher =>
      subscriptionAcknowledged = true
      ref.getSource.to(Sink.foreach(a =>{
        emitEvent(a)
        //println(a)
      })).run(materializer)
      //if(parentReceived && !created) emitCreated()
    case Parent(p1) => {
      //println("Parent received", p1)
      parentNode = p1
      parentReceived = true
      nodeData = LeafNodeData(name, requirements, context, parentNode)
      //if (subscriptionAcknowledged && !created) emitCreated()
    }
    case CentralizedCreated =>
      if(!created){
        created = true
        emitCreated()
      }
    case SourceRequest =>
      source = Source.queue[Event](20000, OverflowStrategy.dropNew).preMaterialize()(materializer)
      future = source._2.runWith(StreamRefs.sourceRef())(materializer)
      sourceRef = Await.result(future, Duration.Inf)
      sender() ! SourceResponse(sourceRef)
    case KillMe => sender() ! PoisonPill
    case Kill =>
      //self ! PoisonPill
      //fMonitor.scheduledTask.cancel()
      //println("Shutting down....")
    case Controller(c) =>
      controller = c
      //println("Got Controller", c)
    case CostReport(c) =>
      costs = c
      frequencyMonitor.onMessageReceive(CostReport(c), nodeData)
      latencyMonitor.onMessageReceive(CostReport(c), nodeData)
      bandwidthMonitor.onMessageReceive(CostReport(c), nodeData)
    case _: Event =>
    case unhandledMessage =>
      frequencyMonitor.onMessageReceive(unhandledMessage, nodeData)
      latencyMonitor.onMessageReceive(unhandledMessage, nodeData)
      bandwidthMonitor.onMessageReceive(unhandledMessage, nodeData)
  }

}
