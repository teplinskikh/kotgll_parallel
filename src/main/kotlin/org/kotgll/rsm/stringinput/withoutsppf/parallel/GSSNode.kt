package org.kotgll.rsm.stringinput.withoutsppf.parallel

import org.kotgll.rsm.grammar.RSMState
import org.kotgll.rsm.grammar.symbol.Nonterminal
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class GSSNode(val nonterminal: Nonterminal, val pos: Int) {
  val edges: ConcurrentHashMap<RSMState, ConcurrentHashMap<GSSNode, Boolean>> = ConcurrentHashMap()

  fun addEdge(rsmState: RSMState, gssNode: GSSNode): Boolean {
    edges.computeIfAbsent(rsmState) { ConcurrentHashMap() }
    return edges[rsmState]!!.putIfAbsent(gssNode, true) == null
  }

  override fun toString() = "GSSNode(nonterminal=$nonterminal, pos=$pos)"

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is GSSNode) return false
    if (nonterminal != other.nonterminal) return false
    if (pos != other.pos) return false
    return true
  }

  override fun hashCode(): Int = Objects.hash(nonterminal, pos)
}