package org.openrepose.filters.uristripper

import net.sf.saxon.lib.ExtensionFunctionDefinition
import net.sf.saxon.om.StructuredQName
import net.sf.saxon.om.Sequence
import net.sf.saxon.value.SequenceType
import net.sf.saxon.value.StringValue
import net.sf.saxon.lib.ExtensionFunctionCall
import net.sf.saxon.expr.XPathContext


/**
  * Allows the definition of a string function in saxon.
  * The function takes a string as a parameter and returns a string,
  * it should have no side effects.
  *
  */

class UrlPathTransformExtension(funName : String, namespacePrefix : String, namespaceURI : String,
                                fun : (String, String, Option[String], Option[String]) => String) extends ExtensionFunctionDefinition
{

  override def getFunctionQName = new StructuredQName(namespacePrefix, namespaceURI, funName)
  override def getArgumentTypes = Array(SequenceType.SINGLE_STRING, SequenceType.SINGLE_STRING, SequenceType.OPTIONAL_STRING, SequenceType.OPTIONAL_STRING)
  override def getResultType(argTypes : Array[SequenceType]) = SequenceType.SINGLE_STRING
  override def makeCallExpression = new ExtensionFunctionCall {
    override def call (context : XPathContext, args : Array[Sequence]) = {
      new StringValue(fun(args(0).head.asInstanceOf[StringValue].getPrimitiveStringValue.toString,
        args(1).head.asInstanceOf[StringValue].getPrimitiveStringValue.toString,
        { Option(args(2).head.asInstanceOf[StringValue].getPrimitiveStringValue.toString) match {
            case Some(ct) if ct == "" => None
            case Some(ct) => Some(ct)
            case _ => None
          }
        },
        { Option(args(3).head.asInstanceOf[StringValue].getPrimitiveStringValue.toString) match {
            case Some(ct) if ct == "" => None
            case Some(ct) => Some(ct)
            case _ => None
          }
        }))
      }
    }
  override def trustResultType = true
}
