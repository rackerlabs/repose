package org.openrepose.servo

import java.net.{UnknownHostException, InetAddress, NetworkInterface}

import scala.xml.{Elem, XML, Node}
import scala.util.{Success, Failure, Try}
import java.text.ParseException

case class ReposeNode(clusterId: String, nodeId: String, host: String, httpPort: Option[Int], httpsPort: Option[Int])

//Gimmie an exception type to wrap stuff with
case class SystemModelParseException(reason: String, cause: Throwable = null) extends Exception(reason, cause)

/**
 * Create a new one of these when you want to parse the content of the SystemModel XML file
 * @param content
 */
class SystemModelParser(content: String) {

  //Build a list of all the local addresses, so I can quickly check to see if a node is local
  val localAddresses: Set[InetAddress] = {
    import scala.collection.JavaConverters._

    NetworkInterface.getNetworkInterfaces.asScala.toList.flatMap(interface => {
      interface.getInetAddresses.asScala.toList
    }).toSet
  }

  def isLocal(node: ReposeNode): Boolean = {
    try {
      val nodeAddress = InetAddress.getByName(node.host)
      localAddresses.contains(nodeAddress)
    } catch {
      case e:UnknownHostException => {
        //If I can't look up the hostname, it's not local!
        false
      }
    }
  }

  /**
   * If you ask for the list of local nodes, you either get the local node list
   * or a list of problems.
   * Obviously if there are problems, it cannot continue to start valves.
   * This may also be used to avoid trying to reload any valves, because Servo had problems parsing the XML
   * And so it will report those errors instead of killing/restarting any Valves
   */
  val localNodes: Try[List[ReposeNode]] = {
    try {
      val xmlized = XML.loadString(content)

      val allClusterIds: List[String] = (xmlized \\ "repose-cluster").toList.map(_.attribute("id").get.head.text)

      val allLocalNodes: List[ReposeNode] = getNodesFromSystemModel(xmlized).filter(isLocal)

      if (allLocalNodes.isEmpty) {
        Failure(SystemModelParseException("No local node(s) found!"))
      } else {
        //If we convert this to a set, and then compare it to the list size, we'll know if we have duplicates
        val conflictingClusters: Boolean = allClusterIds.toSet.size != allClusterIds.size

        //Check for conflicting Node IDs
        val conflictingNodeList = allLocalNodes.groupBy(f => f.clusterId + f.nodeId).filter(pair => {
          pair._2.size > 1
        })

        //Nodes that are missing both ports (shouldn't happen, but we need to catch it)
        val missingAllPortsList = allLocalNodes.filter(node => node.httpPort.isEmpty && node.httpsPort.isEmpty)

        def hasDuplicatedPorts(nodeList: List[ReposeNode]): Boolean = {
          def findDuplicatedPorts(acc: Boolean, usedPorts: Set[Int], list: List[ReposeNode]): Boolean = {
            if (list.isEmpty) {
              acc
            } else {
              val node = list.head
              node match {
                case ReposeNode(_, _, _, Some(httpPort), Some(httpsPort)) => {
                  if (usedPorts.contains(httpPort) || usedPorts.contains(httpsPort) || httpPort == httpsPort) {
                    true //Duplicated port found
                  } else {
                    findDuplicatedPorts(acc, usedPorts + httpPort + httpsPort, list.tail)
                  }
                }
                case ReposeNode(_, _, _, Some(httpPort), None) => {
                  if (usedPorts.contains(httpPort)) {
                    true
                  } else {
                    findDuplicatedPorts(acc, usedPorts + httpPort, list.tail)
                  }
                }
                case ReposeNode(_, _, _, None, Some(httpsPort)) => {
                  if (usedPorts.contains(httpsPort)) {
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

        //Account for various failure cases.
        if (conflictingClusters) {
          Failure(SystemModelParseException("Conflicting cluster IDs found!"))
        } else if (conflictingNodeList.nonEmpty) {
          Failure(SystemModelParseException("Conflicting local node IDs found!"))
        } else if (missingAllPortsList.nonEmpty) {
          Failure(SystemModelParseException("No port configured on a local node!"))
        } else if (hasDuplicatedPorts(allLocalNodes)) {
          Failure(SystemModelParseException("Conflicting local node ports found!"))
        } else {
          //No failures, send back the list of all the local nodes
          Success(allLocalNodes)
        }
      }
    } catch {
      case e:Exception => {
        //If there's any other kind of exception, wrap it, because we're not able to parse the system-model
        Failure(SystemModelParseException("Unable to parse the system-model", e))
      }
    }
  }


  /**
   * Get all the nodes from the system model, without checking to see if they're local
   * @param systemModel
   * @return
   */
  def getNodesFromSystemModel(systemModel: Elem): List[ReposeNode] = {
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

    val clusterList = (systemModel \\ "repose-cluster").toList

    //Fold a list of clusters into a list of ReposeNodes
    clusterList.foldLeft(List[ReposeNode]())((acc, clusterNode) => {
      //Get the nodes for this cluster
      val nodesList = (clusterNode \\ "node").toList
      //The XSD Defines that there's only one Cluster Identifier per cluster, and that it must be there
      val clusterId = clusterNode.attribute("id").get.head.text

      //Append to our accumulator the mapped node list
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
