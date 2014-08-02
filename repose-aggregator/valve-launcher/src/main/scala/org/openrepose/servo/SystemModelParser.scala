package org.openrepose.servo

import scala.xml.{XML, Node}

case class ReposeNode(clusterId: String, nodeId: String, host: String, httpPort: Option[Int], httpsPort: Option[Int])


/**
 * Create a new one of these when you want to parse the content of the SystemModel XML file
 * @param content
 */
class SystemModelParser(content:String) {

  /**
   * If you ask for the list of local nodes, you either get the local node list
   * or a list of problems.
   * Obviously if there are problems, it cannot continue to start valves.
   * This may also be used to avoid trying to reload any valves, because Servo had problems parsing the XML
   * And so it will report those errors instead of killing/restarting any Valves
   */
  val localNodes:Either[List[ReposeNode], List[String]] = ???


  def getNodesFromSystemModel(systemModelContent: String): List[ReposeNode] = {

    def resolveSingleAttribute(node:Node, attr:String):String = {
      node.attribute(attr).get.head.text
    }

    def resolveIntValueAttr(node:Node, attr:String):Option[Int] = {
      node.attribute(attr).flatMap( sn => {
        if(sn.nonEmpty){
          sn.head.map(s => Some(s.text.toInt)).head
        } else {
          None
        }
      })
    }

    val xmlized = XML.loadString(systemModelContent)

    val clusterList = (xmlized \\ "repose-cluster").toList

    clusterList.foldLeft(List[ReposeNode]())((acc, clusterNode) => {
      val nodesList = (clusterList \\ "node").toList
      //The XSD Defines that there's only one Cluster Identifier per cluster, and that it must be there
      val clusterId = clusterNode.attribute("id").get.head.text

      acc ++ nodesList.map { x =>
        val nodeId = resolveSingleAttribute(x, "id")
        val hostname = resolveSingleAttribute(x, "hostname")
        //httpPort and httpsPort might not be there
        val httpPort:Option[Int] = resolveIntValueAttr(x, "http-port")
        val httpsPort:Option[Int] = resolveIntValueAttr(x, "https-port")

        ReposeNode(clusterId, nodeId, hostname, httpPort, httpsPort)
      }
    })
  }

}
