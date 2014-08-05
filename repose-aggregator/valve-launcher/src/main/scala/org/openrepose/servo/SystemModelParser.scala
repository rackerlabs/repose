package org.openrepose.servo

import java.net.{UnknownHostException, InetAddress, NetworkInterface}

import scala.xml.{XML, Node}

case class ReposeNode(clusterId: String, nodeId: String, host: String, httpPort: Option[Int], httpsPort: Option[Int])


/**
 * Create a new one of these when you want to parse the content of the SystemModel XML file
 * @param content
 */
class SystemModelParser(content: String) {
  import scala.collection.JavaConverters._

  val allNodes = getNodesFromSystemModel(content)
  val localInterfaces = NetworkInterface.getNetworkInterfaces.asScala.toList

  val allLocalNodes = allNodes.filter(node => {
    try {
      val nodeAddress = InetAddress.getByName(node.host)
      //Determine if this node is local
      localInterfaces.foldLeft(false)((acc, iface) => {
        //Fold all the local interfaces into a true/false
        val ifaceAddresses = iface.getInetAddresses.asScala.toList
        //Can probably short circuit (although there's probably not many...
        val ifacesForAddress = ifaceAddresses.filter(_.equals(nodeAddress))

        if (ifacesForAddress.nonEmpty) {
          true
        } else {
          acc
        }
      })
    } catch {
      case e: UnknownHostException => {
        //If I can't resolve the host, it's not local!
        false
      }
    }

  })

  /**
   * If you ask for the list of local nodes, you either get the local node list
   * or a list of problems.
   * Obviously if there are problems, it cannot continue to start valves.
   * This may also be used to avoid trying to reload any valves, because Servo had problems parsing the XML
   * And so it will report those errors instead of killing/restarting any Valves
   */
  val localNodes: Either[List[ReposeNode], List[String]] = {
    if (allLocalNodes.isEmpty) {
      Right(List("No local node(s) found!"))
    } else {
      //Check for conflicting Node IDs
      val conflictingNodeList = allLocalNodes.groupBy(f => f.nodeId).filter(pair => {
        pair._2.size > 1
      })
      //Nodes that are missing both ports (shouldn't happen, but we need to catch it)
      val missingAllPortsList = allLocalNodes.filter(node => node.httpPort.isEmpty && node.httpsPort.isEmpty)

      def hasDuplicatedPorts(nodeList:List[ReposeNode]): Boolean = {
        def findDuplicatedPorts(acc:Boolean, usedPorts: Set[Int], list:List[ReposeNode]):Boolean = {
          if(list.isEmpty) {
            acc
          } else {
            val node = list.head
            node match {
              case ReposeNode(_,_,_,Some(httpPort),Some(httpsPort)) => {
                if(usedPorts.contains(httpPort) || usedPorts.contains(httpsPort) || httpPort == httpsPort) {
                  true //Duplicated port found
                } else {
                  findDuplicatedPorts(acc, usedPorts + httpPort + httpsPort, list.tail)
                }
              }
              case ReposeNode(_,_,_,Some(httpPort),None) => {
                if(usedPorts.contains(httpPort)) {
                  true
                } else {
                  findDuplicatedPorts(acc, usedPorts + httpPort, list.tail)
                }
              }
              case ReposeNode(_,_,_,None, Some(httpsPort)) => {
                if(usedPorts.contains(httpsPort)) {
                  true
                } else {
                  findDuplicatedPorts(acc, usedPorts + httpsPort, list.tail)
                }
              }
            }
          }
        }

        findDuplicatedPorts(acc = false, Set.empty[Int], nodeList)
      }

      //Get a set of all the ports we care about, crap this idea doesn't work :|
      //How do I find duplicate ports amongst http and https?!?!

      if (conflictingNodeList.nonEmpty) {
        Right(List("Conflicting local node IDs found!"))
      } else if(missingAllPortsList.nonEmpty) {
        Right(List("No port configured on a local node!"))
      } else if(hasDuplicatedPorts(allLocalNodes)) {
        Right(List("Conflicting local node ports found!"))
      } else {
        Left(allLocalNodes)
      }
    }
  }


  def getNodesFromSystemModel(systemModelContent: String): List[ReposeNode] = {

    def resolveSingleAttribute(node: Node, attr: String): String = {
      node.attribute(attr).get.head.text
    }

    def resolveIntValueAttr(node: Node, attr: String): Option[Int] = {
      node.attribute(attr).flatMap(sn => {
        if (sn.nonEmpty) {
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
        val httpPort: Option[Int] = resolveIntValueAttr(x, "http-port")
        val httpsPort: Option[Int] = resolveIntValueAttr(x, "https-port")

        ReposeNode(clusterId, nodeId, hostname, httpPort, httpsPort)
      }
    })
  }

}
