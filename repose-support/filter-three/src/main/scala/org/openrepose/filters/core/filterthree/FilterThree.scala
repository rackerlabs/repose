package org.openrepose.filters.core.filterthree

import javax.inject.Named
import javax.servlet._
import javax.servlet.http.HttpServletRequest

import org.openrepose.others.SimplicityDivine

import scala.util.{Failure, Success}

@Named
class FilterThree extends Filter {
  override def init(p1: FilterConfig): Unit = {
    //Meh?
  }

  override def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain): Unit = {
    //Needs to call class.forName to try to load a dependency that it doesn't have, but another ear does have.
    //It should throw a ClassNotFound exception

    val didItWork = try {
      //This is a dependency in the FilterOne ear file
      println("\n\n")
      println("FILTER THREE: trying to instantiate Simplicity Divine")
      println("\n\n")
      Class.forName("org.openrepose.others.SimplicityDivine")
      println("\n\n")
      println("FILTER THREE: was able to instantiate Simplicity Divine REAL BAD")
      println("\n\n")
      Failure(new Exception("Should have not been able to instantiate SimplicityDivine"))
    } catch {
      case e: ClassNotFoundException => {
        println("Caught the CNFE, this is the appropriate behavior")
        Success("Yep")
      }
    }

    if(didItWork.isFailure) {
      println("Throwing encapsulated exception")
      throw didItWork.failed.get
    }

    println("Finishing off the filter")
    chain.doFilter(request, response)
  }

  override def destroy(): Unit = {

  }
}

