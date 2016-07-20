/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
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
