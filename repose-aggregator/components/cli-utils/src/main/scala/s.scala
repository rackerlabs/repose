class A {
  def foo {
    val infixExpr = 1 + 2 + (3 + 4) + 5 + 6 +
      7 + 8 + 9 + 10 + 11 + 12 + 13 + (14 +
      15) + 16 + 17 * 18 + 19 + 20
  }

  class Foo {
    def foo(x: Int = 0, y: Int = 1, z: Int = 2) = new Foo
  }

  val goo = new Foo

  goo.foo().foo(1, 2).foo(z = 1, y = 2).foo().foo(1, 2, 3).foo()

  def m(x: Int, y: Int, z: Int)(u: Int, f: Int, l: Int) {
    val zz = if (true) 1 else 3
    val uz = if (true)
      1
    else {
    }
    if (true) {
      false
    } else if (false) {
    } else true
    for (i <- 1 to 5) yield i + 1
    Some(3) match {
      case Some(a) if a != 2 => a
      case Some(1) |
           Some(2) =>

      case _ =>
    }
    try a + 2
    catch {
      case e => (i: Int) => i + 1
    } finally
      doNothing
    while (true)
      true = false
  }


}