package com.scalarookie.eventscalaTS.graph.nodes

import akka.actor.{Actor, ActorRef, Props}
import com.scalarookie.eventscalaTS.data.Queries._

trait Node extends Actor {

  val name: String = self.path.name

  def createChildNode(
      id: Int,
      query: Query,
      publishers: Map[String, ActorRef]
      //frequencyStrategy: StrategyFactory,
      //latencyStrategy: StrategyFactory,
    ): ActorRef = query match {
    case streamQuery: StreamQuery =>
      context.actorOf(Props(StreamNode(streamQuery, publishers, None)), s"$name-$id-stream")
    case filterQuery: FilterQuery =>
      context.actorOf(Props(FilterNode(filterQuery, publishers, None)), s"$name-$id-filter")
    case selectQuery: SelectQuery =>
      context.actorOf(Props(SelectNode(selectQuery, publishers, None)), s"$name-$id-select")
    //case selfJoinQuery: SelfJoinQuery =>
    //  context.actorOf(Props(SelfJoinNode(selfJoinQuery, publishers, None)), s"$name-$id-selfjoin")
    case joinQuery: JoinQuery =>
      context.actorOf(Props(JoinNode(joinQuery, publishers, None)), s"$name-$id-join")
  }

}
