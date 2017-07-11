package com.lambdarookie.eventscala.graph.factory

import com.lambdarookie.eventscala.backend.system.{BinaryOperator, EventSource, UnaryOperator}
import com.lambdarookie.eventscala.backend.system.traits._
import com.lambdarookie.eventscala.data.Queries.{BinaryQuery, LeafQuery, Query, UnaryQuery}

/**
  * Created by monur.
  */
object OperatorFactory {
  def createOperator(id: String, system: System, query: Query, outputs: Set[Operator]): Operator = query match {
    case q: LeafQuery => EventSource(id, system, q, outputs)
    case q: UnaryQuery => UnaryOperator(id, system, q, outputs)
    case q: BinaryQuery => BinaryOperator(id, system, q, outputs)
  }
}