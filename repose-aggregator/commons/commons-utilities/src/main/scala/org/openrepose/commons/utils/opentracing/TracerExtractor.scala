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
package org.openrepose.commons.utils.opentracing

import java.util
import java.util.AbstractMap.SimpleImmutableEntry

import com.typesafe.scalalogging.slf4j.LazyLogging
import io.opentracing.propagation.TextMap
import javax.servlet.http.HttpServletRequest

import scala.collection.mutable.ListBuffer

class TracerExtractor() extends TextMap with LazyLogging {
  var headers: Map[String, List[String]] = _

  def this(httpServletRequest: HttpServletRequest) = {
    this()

    headers = servletHeadersToMultiMap(httpServletRequest)

    def servletHeadersToMultiMap(httpServletRequest: HttpServletRequest): Map[String, List[String]] = {
      val headersResult = scala.collection.mutable.HashMap.empty[String, List[String]]
      if (httpServletRequest != null) {
        val headerNamesIterator = httpServletRequest.getHeaderNames
        while ( {
          headerNamesIterator.hasMoreElements
        }) {
          val headerName: String = headerNamesIterator.nextElement
          val valuesIterator = httpServletRequest.getHeaders(headerName)
          var valuesList = ListBuffer[String]()
          while ( {
            valuesIterator.hasMoreElements
          }) {
            valuesList += valuesIterator.nextElement
          }
          headersResult += (headerName -> valuesList.toList)
        }
      }

      headersResult.toMap

    }
  }

  override def put(key: String, value: String): Unit = ???

  override def iterator() = new MultivaluedMapFlatIterator[String, String](this.headers.toSet)

  /**
    * Iterate through a map with lists
    * @tparam K String key
    * @tparam V List[String] value
    */
  class MultivaluedMapFlatIterator[K <: AnyRef, V >: Null <: AnyRef]() extends util.Iterator[util.Map.Entry[K, V]] {
    var mapIterator: Iterator[(K, List[V])] = _
    var mapEntry: (K, List[V]) = _
    var listIterator: Iterator[V] = _

    def this(multiValuesEntrySet: Set[(K, List[V])]) = {
      this()

      mapIterator = multiValuesEntrySet.iterator
      println(s"ctor $mapIterator")
    }

    override def hasNext: Boolean = {
      println(s"hasNext $listIterator")
      if (listIterator != null && listIterator.hasNext) return true
      println(s"hasNext ${mapIterator.hasNext}")
      mapIterator.hasNext
    }

    override def next: util.Map.Entry[K, V] = {
      println(s"next $mapEntry")
      if (mapEntry == null || (!listIterator.hasNext && mapIterator.hasNext)) {
        mapEntry = mapIterator.next
        println(s"next 2 $mapEntry")
        listIterator = mapEntry._2.iterator
        println(s"listIterator $listIterator")
      }
      println(s"listIterator 2 ${listIterator.hasNext}")
      if (listIterator.hasNext) new SimpleImmutableEntry[K, V](mapEntry._1, listIterator.next)
      else new SimpleImmutableEntry[K, V](mapEntry._1, null)
    }

  }
}
