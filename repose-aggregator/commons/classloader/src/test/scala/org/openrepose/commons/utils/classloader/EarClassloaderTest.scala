package org.openrepose.commons.utils.classloader

import org.scalatest.{FunSpec, Matchers}

class EarClassloaderTest extends FunSpec with Matchers {

  it("unpacks an ear to a directory successfully") {
    pending
  }
  it("throws an IO Exception if unable to unpack the ear to the specified directory") {
    pending
  }

  it("provides a class that is not in the current classloader") {
    pending
  }

  it("cleans up it's unpacked artifacts") {
    pending
  }

  it("throws a ClassNotFoundException when you ask for a class that isn't in the ear") {
    pending
  }

  it("multiple ear files don't share classes") {
    pending
  }

  it("can get the web-fragment.xml") {
    pending
  }

  describe("in the context of spring") {
    it("when given to a AppContext beans are provided") {
      pending
    }
  }
}
