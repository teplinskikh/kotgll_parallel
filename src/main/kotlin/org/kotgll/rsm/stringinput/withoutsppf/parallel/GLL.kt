package org.kotgll.rsm.stringinput.withoutsppf.parallel

import org.kotgll.rsm.grammar.RSMState
import org.kotgll.rsm.grammar.symbol.Nonterminal
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class GLL(val startState: RSMState, val input: String) {
  private val queue: DescriptorsQueue = DescriptorsQueue(input.length + 1)
  private val poppedGSSNodes: ConcurrentHashMap<GSSNode, MutableSet<Int>> = ConcurrentHashMap()
  private val createdGSSNodes: ConcurrentHashMap<GSSNode, GSSNode> = ConcurrentHashMap()
  private val parseResult: AtomicBoolean = AtomicBoolean(false)

  fun getOrCreateGSSNode(nonterminal: Nonterminal, pos: Int): GSSNode {
    val gssNode = GSSNode(nonterminal, pos)
    return createdGSSNodes.computeIfAbsent(gssNode) { gssNode }
  }

  fun parse(): Boolean {
    queue.add(startState, getOrCreateGSSNode(startState.nonterminal, 0), 0)

    val threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
    val tasks = mutableListOf<Runnable>()

    while (!queue.isEmpty() && !parseResult.get()) {
      val descriptor = queue.next() ?: continue
      tasks.add(Runnable { parse(descriptor.rsmState, descriptor.gssNode, descriptor.pos) })
    }

    tasks.forEach { threadPool.submit(it) }
    threadPool.shutdown()
    while (!threadPool.isTerminated) {
      Thread.sleep(10)
    }

    return parseResult.get()
  }

  private fun parse(state: RSMState, gssNode: GSSNode, pos: Int) {
    if (parseResult.get()) return

    state.outgoingTerminalEdges.forEach { rsmEdge ->
      if (pos < input.length && rsmEdge.terminal.match(pos, input)) {
        queue.add(rsmEdge.head, gssNode, pos + rsmEdge.terminal.size)
      }
    }

    state.outgoingNonterminalEdges.forEach { rsmEdge ->
      val newNode = createGSSNode(rsmEdge.nonterminal, rsmEdge.head, gssNode, pos)
      queue.add(rsmEdge.nonterminal.startState, newNode, pos)
    }

    if (state.isFinal) {
      pop(gssNode, pos)
    }
  }

  private fun pop(gssNode: GSSNode, pos: Int) {
    if (parseResult.get()) return

    if (!parseResult.get() &&
      gssNode.nonterminal == startState.nonterminal &&
      gssNode.pos == 0 &&
      pos == input.length
    ) {
      parseResult.set(true)
      return
    }

    val poppedPositions = poppedGSSNodes.computeIfAbsent(gssNode) { ConcurrentHashMap.newKeySet() }
    poppedPositions.add(pos)

    gssNode.edges.forEach { (state, nodes) ->
      nodes.keys.forEach { u ->
        queue.add(state, u, pos)
      }
    }
  }

  private fun createGSSNode(
    nonterminal: Nonterminal,
    state: RSMState,
    gssNode: GSSNode,
    pos: Int
  ): GSSNode {
    val v = getOrCreateGSSNode(nonterminal, pos)

    if (v.addEdge(state, gssNode)) {
      poppedGSSNodes[v]?.forEach { z ->
        queue.add(state, gssNode, z)
      }
    }

    return v
  }
}