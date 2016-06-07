package dyukha.rpg.frontends

import com.sun.deploy.util.StringUtils
import dyukha.rpg.core.Frontend
import dyukha.rpg.core.IL
import dyukha.rpg.core.IL.*;
import java.util.*

class YardFrontend : Frontend<String> {
  class ParseException(val msg : String, val token: SourceText) : Exception(msg) { }
  class SemanticException(val msg : String) : Exception(msg) { }

  class YardParser(val input : String) {
    val EOL : Char = '\n'
    //val TAB : Char = '\t'

    var i = 0;
    var col = 1
    var row = 1
    private fun next() = i++

    private fun skipSpaces() {
      while (!eof && Character.isWhitespace(cur)) {
        if (cur == EOL) {
          col = 0;
          row++;
        } /*else {
          if (cur == TAB) {
            reportError("tabs are not allowed. Please replace all tabs with spaces.")
          }
        }*/
        next()
      }
    }

    data class Pos(val row : Int, val col : Int)
    fun curPos() = Pos(row, col)
    fun toSourceText(text : String, pos : Pos) = SourceText(text, pos.row, pos.col)

    private val isIdentStart : Boolean
        get() = !eof && cur.isJavaIdentifierStart()

    private val cur : Char
      get() = input[i]
    private var curToken : SourceText = SourceText("", 0, 0)
    private val eof : Boolean
      get() = i == input.length

    fun readIdent() : SourceText {
      val startPos = curPos()
      assert(cur.isJavaIdentifierStart())
      val builder = StringBuilder()
      while (!eof && cur.isJavaIdentifierPart()) {
        builder.append(cur)
      }
      curToken = toSourceText(builder.toString(), startPos)
      skipSpaces()
      return curToken
    }

    fun readLiteral() : SourceText {
      val startPos = curPos()
      var slashCnt = 0
      var builder = StringBuilder()
      builder.append(readCharNoSkip('\''))
      while (!eof) {
        builder.append(cur)
        if (cur == '\\') {
          slashCnt++;
          next()
          continue;
        }
        if (cur == '\'') {
          if (slashCnt % 2 == 1)
            break;
        }
        slashCnt = 0
        next()
      }
      readChar('\'')
      val token = builder.toString()
      curToken = toSourceText(token, startPos)
      return curToken
    }

    fun isNextChar(ch : Char) = !eof && ch == cur

    // Doesn't check if it's a complete literal/identifier/etc.
    fun isNextString(str : String) : Boolean {
      if (input.length - i < str.length)
        return false;
      for ((j,c) in str.withIndex())
        if (c != input[i+j])
          return false;
      return true;
    }

    fun readString(str : String) : SourceText {
      val startPos = curPos()
      if (input.length - i < str.length)
        reportExpectedError(str);
      for ((j,c) in str.withIndex()) {
        if (c != cur)
          reportExpectedError(str);
        next()
      }
      skipSpaces()
      curToken = toSourceText(str, startPos)
      return curToken;
    }

    fun readChar(ch : Char) : SourceText {
      val res = readCharNoSkip(ch)
      skipSpaces()
      return res
    }

    fun readCharNoSkip(ch : Char) : SourceText {
      val startPos = curPos()
      val token = ch.toString()
      if (eof || ch != cur)
        reportExpectedError(token)
      next()
      curToken = toSourceText(token, startPos)
      return curToken
    }

    fun readUntilEol(skipWS : Boolean) : SourceText {
      val startPos = curPos()
      val builder = StringBuilder()
      while (!eof && cur != EOL) {
        builder.append(cur)
      }
      curToken = toSourceText(builder.toString(), startPos)
      if (skipWS)
        skipSpaces()
      else
        next()
      return curToken
    }

    fun reportError(msg : String) {
      throw ParseException(msg, curToken)
    }

    fun reportExpectedError(vararg expected : String) {
      reportError("Expected: " + java.lang.String.join(", ", expected.asIterable()))
    }

    fun reportUnexpectedError(unexpected : String) {
      reportError("Unexpected: " + unexpected)
    }

    fun readOptions() : IL.Options {
      readChar('{')
      val options = IL.Options()
      while (isIdentStart) {
        val ident = readIdent()
        readChar('=')
        val value = readUntilEol(true)
        options.put(ident.text, value.text)
      }
      readChar('}')
      return options
    }

    fun readHeader() : SourceText {
      val startPos = curPos()
      val token = "{"
      if (eof || '{' != cur)
        reportExpectedError(token)
      next()
      readUntilEol(false);
      val joiner = StringJoiner(EOL.toString())
      while (true) {
        val str = readUntilEol(false).text
        if (str.length > 2 && str[0] == '}' && str[1] == '%')
          break
        joiner.add(str)
      }
      skipSpaces()
      return toSourceText(joiner.toString(), startPos)
    }

    fun readTokens() : IL.TokenTypes {
      readChar('{')
      val tokens = IL.TokenTypes()
      while (!isNextChar('|')) {
        readChar('|')
        val ident =
            if (isIdentStart)
              readIdent()
            else
              readLiteral()

        readString("of")
        val value = readUntilEol(true)
        if (ident.text == "_")
          tokens.defaultType = value.text
        else
          tokens.put(ident.text, value.text)
      }
      readChar('}')
      return tokens
    }

    fun readAtomOpt() : IL.Element {
      if (isNextChar('[')) {
        readChar('[')
        val inner = readProduction();
        readChar(']')
        return IL.Ebnf(inner, IL.Ebnf.EbnfType.OPT)
      }
      if (isNextChar('(')) {
        readChar('(')
        val inner = readProduction();
        readChar(')')
        if (cur in IL.Ebnf.EbnfType.allChars) {
          val ch = cur;
          readChar(cur)
          return IL.Ebnf(inner, IL.Ebnf.EbnfType.ofChar(ch))
        }
      }
      if (isNextString("[{")) {

      }

      // Literal
      if (isNextChar('\'')) {

      }
      // TokenModifier
      if (isNextChar('/')) {

      }
      if (cur.isLetter()) {
        if (cur.)
      }
    }

    fun readSeq() : IL.Element {
      val atoms = ArrayList<IL.Element>();
      while (!eof && cur !in listOf('|', ']', ')', ';')) {
        val atom = readAtomOpt();
        if (atom != null)
          atoms.add(atom)
        else
          break;
      }
      if (atoms.size == 1)
        return atoms[0];
      return IL.Seq(atoms)
    }

    fun readAlt() : IL.Element {
      val seqs = ArrayList<IL.Element>();
      seqs.add(readSeq());
      while (!eof && cur == '|') {
        readChar('|')
        seqs.add(readSeq());
      }
      if (seqs.size == 1)
        return seqs[0]
      return IL.Alt(seqs)
    }

    fun readProduction() : IL.Element {
      return readAlt();
    }

    fun readRules() : List<IL.Rule> {
      val rules = ArrayList<IL.Rule>()
      readChar('{')
      while (isNextString("[<") || isIdentStart) {
        val attrs = ArrayList<IL.Attribute>()
        while (isNextString("[<")) {
          readString("[<")
          val attrName = readIdent()
          readString(">]")
          attrs.add(IL.Attribute(attrName))
        }
        val nonTerm = readIdent()
        readChar(':')
        val prod = readProduction()
        rules.add(IL.Rule(nonTerm, prod, attrs))
      }
      readChar('}')
      return rules
    }

    fun <T> replaceVal(old: T,  new: T, part: String) : T {
      if (old != null)
        throw SemanticException("Multiple definition of " + part)
      return new
    }

    fun parseGrammar() : IL.Grammar {
      skipSpaces()
      var options : IL.Options? = null
      var tokens : IL.TokenTypes? = null
      var header : SourceText? = null
      var rules : List<IL.Rule>? = null
      while (true) {
        if (isIdentStart) {
          val ident = readIdent()
          if (ident.text == "options")
            options = replaceVal(options, readOptions(), "options")
          else if (ident.text == "tokens")
            tokens = replaceVal(tokens, readTokens(), "tokens")
          else if (ident.text == "header")
            header = replaceVal(header, readHeader(), "header")
          else if (ident.text == "grammar")
            rules = replaceVal(rules, readRules(), "rules")
        }
        reportExpectedError("options", "tokens", "header", "grammar")
        break;
      }
      if (rules == null)
        throw SemanticException("grammar rules must be defined")

      return IL.Grammar(rules, options, header, tokens)
    }
  }

  fun changeEOLs(s : String) = s.replace("\r\n", "\n").replace("\n\r", "\n").replace('\r', '\n')

  override fun get(input: String): IL.Grammar {
    val changed = changeEOLs(input)
    return YardParser(changed).parseGrammar()
  }
}