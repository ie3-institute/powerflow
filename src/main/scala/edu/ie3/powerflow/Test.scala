package edu.ie3.powerflow

import edu.ie3.powerflow.libraries.UMFPACK

object Test {

  def main(args: Array[String]): Unit = {
    val lib = UMFPACK("libumfpack.so.6.3.2")
    
    val n = 5
    val Ap: Array[Long] = Array(0, 2, 5, 9, 10, 12)
    val Ai: Array[Long] = Array(0, 1, 0, 2, 4, 1, 2, 3, 4, 2, 1, 4)
    val Ax = Array(2.0, 3.0, 3.0, -1.0, 4.0, 4.0, -3.0, 1.0, 2.0, 2.0, 6.0, 1.0)
    val b = Array(8.0, 45.0, -3.0, 3.0, 19.0)

    val res = lib.solve(n, Ap, Ai, Ax, b)

    res.length

  }


}
