package org.kotgll

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.required
import org.kotgll.graph.readGraphFromCSV
import org.kotgll.rsm.grammar.readRSMFromTXT
import kotlin.system.measureTimeMillis
import java.io.File

fun main(args: Array<String>) {
    val parser = ArgParser("kotgll-performance")
    val inputMode by parser.option(ArgType.Choice<InputMode>(), fullName = "input", description = "Input format").required()
    val grammarMode by parser.option(ArgType.Choice<GrammarMode>(), fullName = "grammar", description = "Grammar format").required()
    val sppfMode by parser.option(ArgType.Choice<SPPFMode>(), fullName = "sppf", description = "Sppf mode").required()
    val pathToInput by parser.option(ArgType.String, fullName = "inputPath", description = "Path to input file").required()
    val pathToGrammar by parser.option(ArgType.String, fullName = "grammarPath", description = "Path to grammar file").required()
    val pathToOutput by parser.option(ArgType.String, fullName = "outputPath", description = "Path to output directory").required()

    parser.parse(args)

    val times = mutableMapOf<String, Long>()
    var singleThreadedResult: Any? = null
    var parallelResult: Any? = null

    if (inputMode == InputMode.STRING) {
        val input = File(pathToInput).readText()
        if (grammarMode == GrammarMode.RSM) {
            val grammar = readRSMFromTXT(pathToGrammar)

            if (sppfMode == SPPFMode.ON) {
                times["Parallel"] = measureTimeMillis {
                    parallelResult = org.kotgll.rsm.stringinput.withsppf.parallel.GLL(grammar, input).parse()
                }
                times["Single-threaded"] = measureTimeMillis {
                    singleThreadedResult = org.kotgll.rsm.stringinput.withsppf.GLL(grammar, input).parse()
                }
            } else {
                times["Single-threaded"] = measureTimeMillis {
                    singleThreadedResult = org.kotgll.rsm.stringinput.withoutsppf.GLL(grammar, input).parse()
                }
            }
        }
    } else if (inputMode == InputMode.GRAPH) {
        val graph = readGraphFromCSV(pathToInput)
        if (grammarMode == GrammarMode.RSM) {
            val grammar = readRSMFromTXT(pathToGrammar)

            if (sppfMode == SPPFMode.ON) {
                times["Parallel"] = measureTimeMillis {
                    parallelResult = org.kotgll.rsm.graphinput.withsppf.parallel.GLL(grammar, graph).parse()
                }
                times["Single-threaded"] = measureTimeMillis {
                    singleThreadedResult = org.kotgll.rsm.graphinput.withsppf.GLL(grammar, graph).parse()
                }
            } else {
                times["Single-threaded"] = measureTimeMillis {
                    singleThreadedResult = org.kotgll.rsm.graphinput.withoutsppf.GLL(grammar, graph).parse()
                }
            }
        }
    }

    val singleThreadedFile = File(pathToOutput, "single-threaded-result.txt")
    val parallelFile = File(pathToOutput, "parallel-result.txt")

    singleThreadedFile.writeText(singleThreadedResult.toString())
    parallelFile.writeText(parallelResult.toString())

    println("Results saved to:")
    println("Single-threaded: ${singleThreadedFile.absolutePath}")
    println("Parallel: ${parallelFile.absolutePath}")

    println("Performance comparison:")
    times.forEach { (mode, time) ->
        println("$mode mode: $time ms")
    }
}
