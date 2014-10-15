package org.openrepose.core.spring.testfilter

import javax.inject.{Inject, Named}
import javax.servlet._

import org.openrepose.core.spring.test.HerpBean
import org.openrepose.core.spring.test.foo.FooBean

@Named
class TestFilter @Inject() (herp:HerpBean, foo: FooBean) extends Filter {
  override def init(p1: FilterConfig): Unit = ???

  override def doFilter(p1: ServletRequest, p2: ServletResponse, p3: FilterChain): Unit = ???

  override def destroy(): Unit = ???
}
