package adaptivecep.distributed.centralized

import adaptivecep.data.Events._
import adaptivecep.data.Queries.{Operator => _, _}
import adaptivecep.distributed._
import adaptivecep.distributed.operator.{Host, NoHost, NodeHost, Operator}
import adaptivecep.graph.qos.MonitorFactory
import akka.actor.{ActorRef, ActorSystem, Deploy, PoisonPill}
import akka.remote.RemoteScope
import rescala.default._

case class PlacementActorCentralized(actorSystem: ActorSystem,
                                     query: Query,
                                     publishers: Map[String, ActorRef],
                                     publisherHosts: Map[String, Host],
                                     frequencyMonitorFactory: MonitorFactory,
                                     latencyMonitorFactory: MonitorFactory,
                                     bandwidthMonitorFactory: MonitorFactory,
                                     here: NodeHost,
                                     testHosts: Set[ActorRef],
                                     optimizeFor: String)
  extends PlacementActorBase{

  def placeAll(map: Map[Operator, Host]): Unit ={
    map.foreach(pair => place(pair._1, pair._2))
    testHosts.foreach(host => host ! HostToNodeMap(hostToNodeMap))
    map.keys.foreach(operator => {
      if (operator.props != null) {
        val actorRef = propsActors(operator.props)
        val children = operator.dependencies
        val parent = parents(operator)
        if (parent.isDefined) {
          actorRef ! Parent(propsActors(parent.get.props))
          //println("setting Parent of", actorRef, propsActors(parent.get.props))
        }
        children.length match {
          case 0 =>
          case 1 =>
            if (children.head.props != null) {
              //map(operator).asInstanceOf[NodeHost].actorRef ! ChildHost1(map(propsOperators(children.head.props)).asInstanceOf[NodeHost].actorRef)
              actorRef ! Child1(propsActors(children.head.props))
              actorRef ! CentralizedCreated

            }
          case 2 =>
            if (children.head.props != null && children(1).props != null) {
              //map(operator).asInstanceOf[NodeHost].actorRef ! ChildHost2(map(propsOperators(children.head.props)).asInstanceOf[NodeHost].actorRef, map(propsOperators(children(1).props)).asInstanceOf[NodeHost].actorRef)
              actorRef ! Child2(propsActors(children.head.props), propsActors(children(1).props))
              actorRef ! CentralizedCreated

            }
        }
      }
    })
    placement.set(map)
  }

  def place(operator: Operator, host: Host): Unit = {
    if(host != NoHost && operator.props != null){
      //operator.host = host
      val moved = placement.now.contains(operator) && placement.now.apply(operator) != host
      if(moved) {
        propsActors(operator.props) ! Kill
        //println("killing old actor", propsActors(operator.props))
      }
      if (moved || placement.now.size < operators.now.size){
        val hostActor = host.asInstanceOf[NodeHost].actorRef
        val ref = actorSystem.actorOf(operator.props.withDeploy(Deploy(scope = RemoteScope(hostActor.path.address))))
        propsActors += operator.props -> ref
        hostToNodeMap += hostMap(hostActor) -> ref
        hostActor ! Node(ref)
        ref ! Controller(self)
        //println("placing Actor", ref)
      }
    }
  }
}
