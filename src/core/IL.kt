package core

class IL {
  data class Grammar(val rules: List<Rule>, val options : Map<String, String>, val header : String) { }

  data class Rule(val nonTerm : String, val production : Element, val attrs : List<Attribute>) { }

  data class Attribute(val name : String) { }

  // any part of the rule
  interface Element {
    fun visit(visitor: ElementVisitor)
    fun <T> call(caller: ElementCaller<T>): T
  }

  // Elements, which are not part of BNF notation
  interface NonParseElement : Element { }

  // Types of elements
  data class Token(val name: String) : Element {
    override fun visit(visitor: ElementVisitor) = visitor.visitToken(this)
    override fun <T> call(caller: ElementCaller<T>) = caller.callToken(this)
  }

  data class Literal(val text: String) : Element {
    override fun visit(visitor: ElementVisitor) = visitor.visitLiteral(this)
    override fun <T> call(caller: ElementCaller<T>) = caller.callLiteral(this);
  }

  data class Seq(val elements: List<Element>) : Element {
    override fun visit(visitor: ElementVisitor) = visitor.visitSeq(this)
    override fun <T> call(caller: ElementCaller<T>) = caller.callSeq(this);
  }

  data class Ebnf(val element: Element, val ebnfType: EbnfType) : Element {
    enum class EbnfType(symbol: String) {
      OPT("?"),
      ANY("*"),
      NO_ZERO("+")
    }

    override fun visit(visitor: ElementVisitor) = visitor.visitEbnf(this)
    override fun <T> call(caller: ElementCaller<T>) = caller.callEbnf(this);
  }

  data class NonTerm(val name: String) : Element {
    override fun visit(visitor: ElementVisitor) = visitor.visitNonTerm(this)
    override fun <T> call(caller: ElementCaller<T>) = caller.callNonTerm(this)
  }

  data class Alt(val alts: List<Element>) : Element {
    override fun visit(visitor: ElementVisitor) = visitor.visitAlt(this)
    override fun <T> call(caller: ElementCaller<T>) = caller.callAlt(this)
  }

  data class Meta(val name : String, val arguments : List<Element>) : Element {
    override fun visit(visitor: ElementVisitor) = visitor.visitMeta(this)
    override fun <T> call(caller: ElementCaller<T>) = caller.callMeta(this)
  }

  // NonParseElements

  data class LookaheadModifier(val tokens : List<String>, val modifier: Modifier) : NonParseElement {
    enum class Modifier(mode : Boolean) {
      FORBID(false),
      NEED(true)
    }
    override fun visit(visitor: ElementVisitor) = visitor.visitLookaheadModifier(this)
    override fun <T> call(caller: ElementCaller<T>) = caller.callLookaheadModifier(this)
  }

  data class ActionCode(val code : String) : NonParseElement {
    override fun visit(visitor: ElementVisitor) = visitor.visitActionCode(this)
    override fun <T> call(caller: ElementCaller<T>) = caller.callActionCode(this)
  }

  // Visitors

  abstract class ElementVisitor() {
    abstract fun visitToken(t: Token)
    abstract fun visitLiteral(lit: Literal)
    abstract fun visitSeq(seq: Seq)
    abstract fun visitEbnf(ebnf: Ebnf)
    abstract fun visitNonTerm(nonTerm: NonTerm)
    abstract fun visitAlt(alt: Alt)
    abstract fun visitMeta(meta: Meta)
    abstract fun visitLookaheadModifier(lookaheadModifier: LookaheadModifier)
    abstract fun visitActionCode(actionCode: ActionCode)
  }

  abstract class ElementVisitorWithDefault : ElementVisitor() {
    fun visitElement(e: Element) { }

    override fun visitToken(t: Token) = visitElement(t)
    override fun visitLiteral(lit: Literal) = visitElement(lit)
    override fun visitSeq(seq: Seq) = visitElement(seq)
    override fun visitEbnf(ebnf: IL.Ebnf) = visitElement(ebnf)
    override fun visitNonTerm(nonTerm: NonTerm) = visitElement(nonTerm)
    override fun visitAlt(alt: Alt) = visitElement(alt)
    override fun visitMeta(meta: Meta) = visitElement(meta)
    override fun visitLookaheadModifier(lookaheadModifier: LookaheadModifier) = visitElement(lookaheadModifier)
    override fun visitActionCode(actionCode: ActionCode) = visitElement(actionCode)
  }

  abstract class ElementCaller<T>() {
    abstract fun callToken(t: Token): T
    abstract fun callLiteral(lit: Literal): T
    abstract fun callSeq(seq: Seq): T
    abstract fun callEbnf(ebnf: Ebnf): T
    abstract fun callNonTerm(nonTerm: NonTerm): T
    abstract fun callAlt(alt: Alt): T
    abstract fun callMeta(meta: Meta): T
    abstract fun callLookaheadModifier(lookaheadModifier: LookaheadModifier): T
    abstract fun callActionCode(actionCode: ActionCode): T
  }

  abstract class ElementCallerWithDefault<T> : ElementCaller<T>() {
    abstract fun callElement(e: Element): T
    override fun callToken(t: Token) = callElement(t)
    override fun callLiteral(lit: Literal) = callElement(lit)
    override fun callSeq(seq: Seq) = callElement(seq)
    override fun callEbnf(ebnf: Ebnf) = callElement(ebnf)
    override fun callNonTerm(nonTerm: NonTerm) = callElement(nonTerm)
    override fun callAlt(alt: Alt) = callElement(alt)
    override fun callMeta(meta: Meta) = callElement(meta)
    override fun callLookaheadModifier(lookaheadModifier: LookaheadModifier) = callElement(lookaheadModifier)
    override fun callActionCode(actionCode: ActionCode) = callElement(actionCode)
  }
}