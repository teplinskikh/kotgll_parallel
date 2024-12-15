package org.kotgll.rsm.graphinput.withsppf.parallel

import org.kotgll.graph.GraphNode
import org.kotgll.rsm.grammar.RSMState
import org.kotgll.rsm.grammar.symbol.Nonterminal
import org.kotgll.rsm.grammar.symbol.Terminal
import org.kotgll.rsm.graphinput.withsppf.parallel.sppf.*
import java.util.concurrent.*

class GLL(val startState: RSMState, val startGraphNodes: ArrayList<GraphNode>) {
    constructor(
        startState: RSMState,
        startGraphNodes: List<GraphNode>
    ) : this(startState, ArrayList(startGraphNodes))

    val queue: DescriptorsQueue = DescriptorsQueue()
    val poppedGSSNodes: ConcurrentHashMap<GSSNode, MutableSet<SPPFNode?>> = ConcurrentHashMap()
    val createdGSSNodes: ConcurrentHashMap<GSSNode, GSSNode> = ConcurrentHashMap()
    val createdSPPFNodes: ConcurrentHashMap<SPPFNode, SPPFNode> = ConcurrentHashMap()
    val parseResult: ConcurrentHashMap<Int, ConcurrentHashMap<Int, SPPFNode>> = ConcurrentHashMap()
    private val forkJoinPool = ForkJoinPool()

    fun getOrCreateGSSNode(nonterminal: Nonterminal, pos: GraphNode): GSSNode {
        return createdGSSNodes.computeIfAbsent(GSSNode(nonterminal, pos)) { it }
    }

    fun parse(): ConcurrentHashMap<Int, ConcurrentHashMap<Int, SPPFNode>> {
        val tasks = startGraphNodes.map { graphNode ->
            Callable {
                parse(startState, getOrCreateGSSNode(startState.nonterminal, graphNode), null, graphNode)
            }
        }

        forkJoinPool.invokeAll(tasks)
        return parseResult
    }

    fun parse(state: RSMState, gssNode: GSSNode, sppfNode: SPPFNode?, pos: GraphNode) {
        var curSPPFNode: SPPFNode? = sppfNode

        if (state.isStart && state.isFinal)
            curSPPFNode = getNodeP(state, curSPPFNode, getOrCreateItemSPPFNode(state, pos, pos))

        state.outgoingTerminalEdges.parallelStream().forEach { rsmEdge ->
            pos.outgoingEdges[rsmEdge.terminal.value]?.parallelStream()?.forEach { head ->
                queue.add(
                    rsmEdge.head,
                    gssNode,
                    getNodeP(
                        rsmEdge.head,
                        curSPPFNode,
                        getOrCreateTerminalSPPFNode(rsmEdge.terminal, pos, head)),
                    head)
            }
        }

        state.outgoingNonterminalEdges.parallelStream().forEach { rsmEdge ->
            queue.add(
                rsmEdge.nonterminal.startState,
                createGSSNode(rsmEdge.nonterminal, rsmEdge.head, gssNode, curSPPFNode, pos),
                null,
                pos)
        }

        if (state.isFinal) pop(gssNode, curSPPFNode, pos)
    }

    fun pop(gssNode: GSSNode, sppfNode: SPPFNode?, pos: GraphNode) {
        poppedGSSNodes.computeIfAbsent(gssNode) { ConcurrentHashMap.newKeySet() }.add(sppfNode)
        gssNode.edges.forEach { (key, value) ->
            value.parallelStream().forEach { u ->
                queue.add(key.first, u, getNodeP(key.first, key.second, sppfNode!!), pos)
            }
        }
    }

    fun createGSSNode(
        nonterminal: Nonterminal,
        state: RSMState,
        gssNode: GSSNode,
        sppfNode: SPPFNode?,
        pos: GraphNode
    ): GSSNode {
        val v: GSSNode = getOrCreateGSSNode(nonterminal, pos)

        if (v.addEdge(state, sppfNode, gssNode)) {
            poppedGSSNodes[v]?.forEach { z ->
                queue.add(state, gssNode, getNodeP(state, sppfNode, z!!), z.rightExtent)
            }
        }

        return v
    }

    fun getNodeP(state: RSMState, sppfNode: SPPFNode?, nextSPPFNode: SPPFNode): SPPFNode {
        val leftExtent = sppfNode?.leftExtent ?: nextSPPFNode.leftExtent
        val rightExtent = nextSPPFNode.rightExtent

        val y =
            if (state.isFinal) getOrCreateSymbolSPPFNode(state.nonterminal, leftExtent, rightExtent)
            else getOrCreateItemSPPFNode(state, leftExtent, rightExtent)

        y.kids.add(PackedSPPFNode(nextSPPFNode.leftExtent, state, sppfNode, nextSPPFNode))

        return y
    }

    fun getOrCreateTerminalSPPFNode(terminal: Terminal, leftExtent: GraphNode, rightExtent: GraphNode): SPPFNode {
        return createdSPPFNodes.computeIfAbsent(
            TerminalSPPFNode(leftExtent, rightExtent, terminal)) { it }
    }

    fun getOrCreateItemSPPFNode(state: RSMState, leftExtent: GraphNode, rightExtent: GraphNode): ItemSPPFNode {
        return createdSPPFNodes.computeIfAbsent(
            ItemSPPFNode(leftExtent, rightExtent, state)) { it } as ItemSPPFNode
    }

    fun getOrCreateSymbolSPPFNode(
        nonterminal: Nonterminal,
        leftExtent: GraphNode,
        rightExtent: GraphNode
    ): SymbolSPPFNode {
        val result = createdSPPFNodes.computeIfAbsent(
            SymbolSPPFNode(leftExtent, rightExtent, nonterminal)) { it } as SymbolSPPFNode
        if (nonterminal == startState.nonterminal && leftExtent.isStart && rightExtent.isFinal) {
            parseResult.computeIfAbsent(leftExtent.id) { ConcurrentHashMap() }[rightExtent.id] = result
        }
        return result
    }
}
