package dyukha.rpg.generators

import dyukha.rpg.core.Generator
import dyukha.rpg.core.IL
import java.util.*

class Printer : Generator<String> {
  data class PrintResult(val res : String, val isComplex : Boolean) {

  }

  class PrintCaller : IL.ElementCaller<PrintResult>() {

    fun callAndWrap(e : IL.Element) : String {
      val (print, isComplex) = call(e)
      if (isComplex)
        return "($print)";
      return print;
    }

    override fun callToken(t: IL.Token) = PrintResult(t.name, false)

    override fun callLiteral(lit: IL.Literal) = PrintResult(lit.text, false)

    override fun callNonTerm(nonTerm: IL.NonTerm) = PrintResult(nonTerm.name, false)

    override fun callSeq(seq: IL.Seq): PrintResult {
      val elems = seq.elements;
      if (elems.size == 0)
        return PrintResult("", false)
      if (elems.size == 1)
        return call(elems[0])
      val joiner = StringJoiner(" ")
      for (e in elems)
        joiner.add(callAndWrap(e))
      return PrintResult(joiner.toString(), true)
    }

    override fun callEbnf(ebnf: IL.Ebnf): PrintResult {
      val (e, type) = ebnf
      if (type == IL.Ebnf.EbnfType.OPT)
        return PrintResult("[" + call(e) + "]", false)
      return PrintResult(callAndWrap(e) + type.symbol, false)
    }

    override fun callAlt(alt: IL.Alt): PrintResult {
      throw UnsupportedOperationException()
    }

    override fun callMeta(meta: IL.Meta): PrintResult {
      throw UnsupportedOperationException()
    }

    override fun callLookaheadModifier(lookaheadModifier: IL.LookaheadModifier): PrintResult {
      throw UnsupportedOperationException()
    }

    override fun callActionCode(actionCode: IL.ActionCode): PrintResult {
      throw UnsupportedOperationException()
    }
  }

  override fun generate(g: IL.Grammar): String {
    val (rules, options, header, tokenTypes) = g
    val joiner = StringJoiner(System.lineSeparator())

    joiner.add("%{")
    joiner.add(header)
    joiner.add("}%")

    joiner.add(options.print())
    joiner.add(tokenTypes.print())

    val printCaller = PrintCaller()
    for (rule in rules) {
      val (nonTerm, prod, attrs) = rule
      for (attr in attrs)
        joiner.add(attr.print())

      val ruleBuilder = StringBuilder()
      ruleBuilder
          .append(nonTerm).append(": ")
          .append(printCaller.call(prod))
      joiner.add(ruleBuilder)
    }
    return joiner.toString()
  }

}