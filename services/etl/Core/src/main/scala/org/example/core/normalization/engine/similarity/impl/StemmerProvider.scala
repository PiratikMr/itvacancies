package org.example.core.normalization.engine.similarity.impl

import org.tartarus.snowball.SnowballStemmer
import org.tartarus.snowball.ext.{englishStemmer, russianStemmer}

object StemmerProvider {

  private val ruStemmerTL = new ThreadLocal[russianStemmer] {
    override def initialValue(): russianStemmer = new russianStemmer()
  }

  private val enStemmerTL = new ThreadLocal[englishStemmer] {
    override def initialValue(): englishStemmer = new englishStemmer()
  }

  def stem(word: String): String = {
    if (word == null || word.isEmpty) return ""

    val stemmer = getStemmerFor(word)

    stemmer.setCurrent(word)
    stemmer.stem()
    stemmer.getCurrent
  }

  private def getStemmerFor(word: String): SnowballStemmer = {
    if (isRussian(word)) ruStemmerTL.get()

    else enStemmerTL.get()
  }


  private def isRussian(word: String): Boolean = {
    var i = 0
    val len = word.length

    while (i < len) {
      val c = word.charAt(i)

      if (c >= '\u0400' && c <= '\u04FF')
        return true

      i += 1
    }

    false
  }

}
