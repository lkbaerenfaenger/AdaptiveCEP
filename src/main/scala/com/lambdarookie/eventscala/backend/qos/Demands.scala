package com.lambdarookie.eventscala.backend.qos

import com.lambdarookie.eventscala.backend.qos.QualityOfService.Violation
import rescala._


/**
  * Created by monur.
  */
trait Demands {
  private val violationsVar: Var[Set[Violation]] = Var(Set.empty)
  private val fireAdaptationPlanned: Evt[Set[Violation]] = Evt[Set[Violation]]
  private val waitingVar: Var[Set[Violation]] = Var(Set.empty)
  private val adaptingVar: Var[Option[Set[Violation]]] = Var(None)

  val violations: Signal[Set[Violation]] = violationsVar
  val adaptationPlanned: Event[Set[Violation]] = fireAdaptationPlanned
  val waiting: Signal[Set[Violation]] = waitingVar
  val adapting: Signal[Option[Set[Violation]]] = adaptingVar

  adaptationPlanned += { vs => waitingVar.transform(_ ++ vs) }

  private[backend] def addViolations(violations: Set[Violation]): Unit = violationsVar.transform(_ ++ violations)
  private[backend] def removeViolation(violation: Violation): Unit = violationsVar.transform(_ - violation)
  private[backend] def fireAdaptationPlanned(violations: Set[Violation]): Unit = fireAdaptationPlanned fire violations
  private[backend] def stopAdapting(): Unit = adaptingVar.transform(_ => None)
  private[backend] def startAdapting(): Unit = if (adapting.now.isEmpty) {
    val w: Set[Violation] = waiting.now
    waitingVar.transform(_ => Set.empty)
    adaptingVar.transform(_ => Some(w))
  }
}
