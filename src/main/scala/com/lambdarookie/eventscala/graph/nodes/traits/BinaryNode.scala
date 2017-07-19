package com.lambdarookie.eventscala.graph.nodes.traits

import akka.actor.ActorRef
import com.lambdarookie.eventscala.backend.system.BinaryOperator
import com.lambdarookie.eventscala.backend.system.traits.Operator
import com.lambdarookie.eventscala.data.Events._
import com.lambdarookie.eventscala.data.Queries._
import com.lambdarookie.eventscala.graph.qos._

trait BinaryNode extends Node {

  override val query: BinaryQuery
  override val operator: BinaryOperator

  val createdCallback: Option[() => Any]
  val eventCallback: Option[(Event) => Any]

  private val childOperator1: Operator = operator.inputs.head
  private val childOperator2: Operator = operator.inputs(1)

  val childNode1: ActorRef = createChildNode(1, query.sq1, childOperator1)
  val childNode2: ActorRef = createChildNode(2, query.sq2, childOperator2)
  system.nodesToOperatorsVar() = system.nodesToOperators.now ++
    Map(childNode1 -> childOperator1, childNode2 -> childOperator2)

  val frequencyMonitor: BinaryNodeMonitor = frequencyMonitorFactory.createBinaryNodeMonitor
  val latencyMonitor: BinaryNodeMonitor = latencyMonitorFactory.createBinaryNodeMonitor
  val nodeData: BinaryNodeData = BinaryNodeData(name, query, context, childNode1, childNode2)

  def emitCreated(): Unit = {
    if (createdCallback.isDefined) createdCallback.get.apply() else context.parent ! Created
    frequencyMonitor.onCreated(nodeData)
    latencyMonitor.onCreated(nodeData)
  }

  def emitEvent(event: Event): Unit = {
    if (eventCallback.isDefined) eventCallback.get.apply(event) else context.parent ! event
    frequencyMonitor.onEventEmit(event, nodeData)
    latencyMonitor.onEventEmit(event, nodeData)
  }

}