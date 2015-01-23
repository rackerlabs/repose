package org.openrepose.filters.herp

case class TemplateValues(
                           userName: String,
                           impersonatorName: String,
                           projectId: java.util.Iterator[String],
                           roles: java.util.Iterator[String],
                           userAgent: String,
                           requestMethod: String,
                           requestUrl: String,
                           requestQueryString: String,
                           parameters: java.util.Set[java.util.Map.Entry[String, Array[String]]],
                           timestamp: Long,
                           responseCode: Int,
                           responseMessage: String,
                           guid: String,
                           serviceCode: String,
                           region: String,
                           datacenter: String
                           ) extends Serializable
