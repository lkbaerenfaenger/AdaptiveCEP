package com.lambdarookie.eventscala.backend.system.traits

import com.lambdarookie.eventscala.backend.data.Coordinate
import com.lambdarookie.eventscala.backend.data.QoSUnits._
import rescala._

/**
  * Created by monur.
  */
trait Host {
  val position: Coordinate

  def neighbors: Set[Host]
  def measureFrequency(): Ratio
  def measureNeighborLatencies(): Unit
  def measureNeighborBandwidths(): Unit
  def measureNeighborThroughputs(): Unit


  private val operatorsVar: Var[Set[Operator]] = Var(Set.empty)
  val operators: Signal[Set[Operator]] = operatorsVar

  var lastFrequency: Ratio = Ratio(0.instances, 0.sec)
  var lastProximities: Map[Host, Distance] = Map(this -> 0.m)
  var neighborLatencies: Map[Host, (Seq[Host], TimeSpan)] = Map(this -> (Seq.empty, 0.ms))
  var neighborThroughputs: Map[Host, (Seq[Host], BitRate)] = Map(this -> (Seq.empty, Int.MaxValue.gbps))
  var neighborBandwidths: Map[Host, (Seq[Host], BitRate)] = Map(this -> (Seq.empty, Int.MaxValue.gbps))


  def addOperator(operator: Operator): Unit = operatorsVar.transform(_ + operator)

  def removeOperator(operator: Operator): Unit = operatorsVar.transform(_ - operator)

  def measureProximities(): Unit =
    neighbors.foreach(n => lastProximities += (n -> position.calculateDistanceTo(n.position).m))

  def measureMetrics(): Unit = {
    measureFrequency()
    measureProximities()
    measureNeighborLatencies()
    measureNeighborBandwidths()
    measureNeighborThroughputs()
  }
}