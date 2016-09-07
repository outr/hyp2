package org.hyperscala

import scala.concurrent.forkjoin.ThreadLocalRandom

object Unique {
  val LettersLower = "abcdefghijklmnopqrstuvwxyz"
  val LettersUpper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
  val Numbers = "0123456789"
  val LettersAndNumbers = s"$LettersUpper$Numbers"
  val AllLettersAndNumbers = s"$LettersLower$LettersUpper$Numbers"

  private def r = ThreadLocalRandom.current()

  /**
    * Generates a unique String using the characters supplied at the length defined.
    *
    * @param length the length of the resulting String. Defaults to 32.
    * @param characters the characters for use in the String. Defaults to AllLettersAndNumbers.
    * @return a unique String
    */
  def apply(length: Int = 32, characters: String = AllLettersAndNumbers): String = {
    val charMax = characters.length
    (0 until length).map(i => characters.charAt(r.nextInt(charMax))).mkString
  }
}