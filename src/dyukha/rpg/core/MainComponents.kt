package dyukha.rpg.core

interface Generator<T> {
  fun generate(g : IL.Grammar) : T
}

interface Conversion {
  fun convert(g : IL.Grammar) : IL.Grammar
}

interface Frontend<T> {
  fun get(params : T) : IL.Grammar
}


