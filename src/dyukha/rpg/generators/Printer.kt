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

    override fun callToken(t: IL.Token) = PrintResult(t.name.text, false)

    override fun callLiteral(lit: IL.Literal) = PrintResult(lit.text.text, false)

    override fun callNonTerm(nonTerm: IL.NonTerm) = PrintResult(nonTerm.name.text, false)

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
      val (alts) = alt
      if (alts.size == 1)
        return call(alts[0])
      val joiner = StringJoiner(" | ")
      for (e in alts)
        joiner.add(callAndWrap(e))
      return PrintResult(joiner.toString(), true)
    }

    override fun callMeta(meta: IL.Meta): PrintResult {
      val (name, args) = meta
      fun wrap(str: String) = PrintResult("$name<$str>", false)
      if (args.size == 0)
        return PrintResult(name.text, false)
      if (args.size == 1)
        return wrap(call(args[0]).res)
      val joiner = StringJoiner(" ")
      for (e in args)
        joiner.add(callAndWrap(e))
      return wrap(joiner.toString())

    }

    override fun callLookaheadModifier(lookaheadModifier: IL.LookaheadModifier): PrintResult {
      val (tokens, modifier) = lookaheadModifier
      var pref = when(modifier) {
        IL.LookaheadModifier.Modifier.FORBID -> "/~"
        IL.LookaheadModifier.Modifier.NEED -> "/"
      }
      if (tokens.size == 1)
        return PrintResult(pref + tokens[0], false)
      val joiner = StringJoiner(", ", pref + "[", "]")
      for (t in tokens)
        joiner.add(t.text)
      return PrintResult(joiner.toString(), false)
    }

    override fun callActionCode(actionCode: IL.ActionCode): PrintResult {
      return PrintResult("{[ ${actionCode.code}]}", false)
    }
  }

  override fun generate(g: IL.Grammar): String {
    val (rules, options, header, tokenTypes) = g
    val joiner = StringJoiner(System.lineSeparator())

    if (header != null) {
      joiner.add("header {")
      joiner.add(header.text)
      joiner.add("}%")
    }

    if (options != null)
      joiner.add(options.print())
    if (tokenTypes != null)
      joiner.add(tokenTypes.print())

    joiner.add("grammar {")
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
    joiner.add("}")
    return joiner.toString()
  }

}