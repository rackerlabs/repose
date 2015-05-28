package org.openrepose.filters.keystonev2

import org.joda.time.{DateTimeZone, DateTime}
import org.joda.time.format.DateTimeFormat

trait IdentityResponses {

  def authenticateTokenResponse(token:String = "glibglob",
                                 expires:DateTime = DateTime.now()):String = {


    val tokenExpiryFormat ="yyyy-MM-dd'THH:mm:ss.SSS'Z"
    val fmt = DateTimeFormat.forPattern(tokenExpiryFormat)
    val formattedTime = fmt.print(expires.withZone(DateTimeZone.forOffsetHours(0)))

    s"""
      |{
      |  "access": {
      |    "token": {
      |      "id": "$token",
      |      "expires": "$formattedTime",
      |      "tenant": {
      |        "id": "123ab1",
      |        "name": "123ab1"
      |      },
      |      "RAX-AUTH:authenticatedBy": [
      |        "PASSCODE"
      |      ]
      |    },
      |    "serviceCatalog": [
      |      {
      |        "name": "cloudBlockStorage",
      |        "endpoints": [
      |          {
      |            "region": "DFW",
      |            "tenantId": "123ab1",
      |            "publicURL": "https://dfw.blockstorage.api.rackspacecloud.com/v1/123ab1"
      |          },
      |          {
      |            "region": "SYD",
      |            "tenantId": "123ab1",
      |            "publicURL": "https://syd.blockstorage.api.rackspacecloud.com/v1/123ab1"
      |          }
      |        ],
      |        "type": "volume"
      |      },
      |      {
      |        "name": "cloudImages",
      |        "endpoints": [
      |          {
      |            "region": "IAD",
      |            "tenantId": "123ab1",
      |            "publicURL": "https://iad.images.api.rackspacecloud.com/v2"
      |          },
      |          {
      |            "region": "HKG",
      |            "tenantId": "123ab1",
      |            "publicURL": "https://hkg.images.api.rackspacecloud.com/v2"
      |          }
      |        ],
      |        "type": "image"
      |      },
      |      {
      |        "name": "cloudQueues",
      |        "endpoints": [
      |          {
      |            "region": "HKG",
      |            "tenantId": "123ab1",
      |            "publicURL": "https://hkg.queues.api.rackspacecloud.com/v1/123ab1",
      |            "internalURL": "https://snet-hkg.queues.api.rackspacecloud.com/v1/123ab1"
      |          },
      |          {
      |            "region": "SYD",
      |            "tenantId": "123ab1",
      |            "publicURL": "https://syd.queues.api.rackspacecloud.com/v1/123ab1",
      |            "internalURL": "https://snet-syd.queues.api.rackspacecloud.com/v1/123ab1"
      |          }
      |        ],
      |        "type": "rax:queues"
      |      },
      |      {
      |        "name": "cloudBigData",
      |        "endpoints": [
      |          {
      |            "region": "IAD",
      |            "tenantId": "123ab1",
      |            "publicURL": "https://iad.bigdata.api.rackspacecloud.com/v1.0/123ab1"
      |          },
      |          {
      |            "region": "DFW",
      |            "tenantId": "123ab1",
      |            "publicURL": "https://dfw.bigdata.api.rackspacecloud.com/v1.0/123ab1"
      |          }
      |        ],
      |        "type": "rax:bigdata"
      |      },
      |      {
      |        "name": "cloudOrchestration",
      |        "endpoints": [
      |          {
      |            "region": "HKG",
      |            "tenantId": "929418",
      |            "publicURL": "https://hkg.orchestration.api.rackspacecloud.com/v1/929418"
      |          },
      |          {
      |            "region": "DFW",
      |            "tenantId": "929418",
      |            "publicURL": "https://dfw.orchestration.api.rackspacecloud.com/v1/929418"
      |          },
      |          {
      |            "region": "IAD",
      |            "tenantId": "929418",
      |            "publicURL": "https://iad.orchestration.api.rackspacecloud.com/v1/929418"
      |          },
      |          {
      |            "region": "SYD",
      |            "tenantId": "929418",
      |            "publicURL": "https://syd.orchestration.api.rackspacecloud.com/v1/929418"
      |          }
      |        ],
      |        "type": "orchestration"
      |      },
      |      {
      |        "name": "cloudServersOpenStack",
      |        "endpoints": [
      |          {
      |            "region": "IAD",
      |            "tenantId": "929418",
      |            "publicURL": "https://iad.servers.api.rackspacecloud.com/v2/929418",
      |            "versionInfo": "https://iad.servers.api.rackspacecloud.com/v2",
      |            "versionList": "https://iad.servers.api.rackspacecloud.com/",
      |            "versionId": "2"
      |          },
      |          {
      |            "region": "DFW",
      |            "tenantId": "929418",
      |            "publicURL": "https://dfw.servers.api.rackspacecloud.com/v2/929418",
      |            "versionInfo": "https://dfw.servers.api.rackspacecloud.com/v2",
      |            "versionList": "https://dfw.servers.api.rackspacecloud.com/",
      |            "versionId": "2"
      |          }
      |        ]
      |      },
      |      {
      |        "name": "autoscale",
      |        "endpoints": [
      |          {
      |            "region": "DFW",
      |            "tenantId": "123ab1",
      |            "publicURL": "https://dfw.autoscale.api.rackspacecloud.com/v1.0/123ab1"
      |          },
      |          {
      |            "region": "HKG",
      |            "tenantId": "123ab1",
      |            "publicURL": "https://hkg.autoscale.api.rackspacecloud.com/v1.0/123ab1"
      |          }
      |        ],
      |        "type": "rax:autoscale"
      |      },
      |      {
      |        "name": "cloudDatabases",
      |        "endpoints": [
      |          {
      |            "region": "DFW",
      |            "tenantId": "123ab1",
      |            "publicURL": "https://dfw.databases.api.rackspacecloud.com/v1.0/123ab1"
      |          }
      |        ]
      |      },
      |      {
      |        "name": "cloudBackup",
      |        "endpoints": [
      |          {
      |            "region": "IAD",
      |            "tenantId": "123ab1",
      |            "publicURL": "https://iad.backup.api.rackspacecloud.com/v1.0/123ab1"
      |          },
      |          {
      |            "region": "HKG",
      |            "tenantId": "123ab1",
      |            "publicURL": "https://hkg.backup.api.rackspacecloud.com/v1.0/123ab1"
      |          }
      |        ],
      |        "type": "rax:backup"
      |      },
      |      {
      |        "name": "cloudNetworks",
      |        "endpoints": [
      |          {
      |            "region": "IAD",
      |            "tenantId": "123ab1",
      |            "publicURL": "https://iad.networks.api.rackspacecloud.com/v2.0"
      |          },
      |          {
      |            "region": "LON",
      |            "tenantId": "123ab1",
      |            "publicURL": "https://lon.networks.api.rackspacecloud.com/v2.0"
      |          }
      |        ],
      |        "type": "network"
      |      },
      |      {
      |        "name": "cloudLoadBalancers",
      |        "endpoints": [
      |          {
      |            "region": "IAD",
      |            "tenantId": "123ab1",
      |            "publicURL": "https://iad.loadbalancers.api.rackspacecloud.com/v1.0/123ab1"
      |          },
      |          {
      |            "region": "HKG",
      |            "tenantId": "123ab1",
      |            "publicURL": "https://hkg.loadbalancers.api.rackspacecloud.com/v1.0/123ab1"
      |          }
      |        ],
      |        "type": "rax:load-balancer"
      |      },
      |      {
      |        "name": "cloudMetrics",
      |        "endpoints": [
      |          {
      |            "region": "IAD",
      |            "tenantId": "123ab1",
      |            "publicURL": "https://global.metrics.api.rackspacecloud.com/v2.0/123ab1"
      |          }
      |        ],
      |        "type": "rax:cloudmetrics"
      |      },
      |      {
      |        "name": "cloudFeeds",
      |        "endpoints": [
      |          {
      |            "region": "HKG",
      |            "tenantId": "123ab1",
      |            "publicURL": "https://hkg.feeds.api.rackspacecloud.com/123ab1",
      |            "internalURL": "https://atom.prod.hkg1.us.ci.rackspace.net/123ab1"
      |          },
      |          {
      |            "region": "SYD",
      |            "tenantId": "123ab1",
      |            "publicURL": "https://syd.feeds.api.rackspacecloud.com/123ab1",
      |            "internalURL": "https://atom.prod.syd2.us.ci.rackspace.net/123ab1"
      |          }
      |        ],
      |        "type": "rax:feeds"
      |      },
      |      {
      |        "name": "cloudMonitoring",
      |        "endpoints": [
      |          {
      |            "tenantId": "123ab1",
      |            "publicURL": "https://monitoring.api.rackspacecloud.com/v1.0/123ab1"
      |          }
      |        ],
      |        "type": "rax:monitor"
      |      },
      |      {
      |        "name": "cloudFiles",
      |        "endpoints": [
      |          {
      |            "region": "IAD",
      |            "tenantId": "MossoCloudFS_123ab1",
      |            "publicURL": "https://storage101.iad3.clouddrive.com/v1/MossoCloudFS_123ab1",
      |            "internalURL": "https://snet-storage101.iad3.clouddrive.com/v1/MossoCloudFS_123ab1"
      |          }
      |        ],
      |        "type": "object-store"
      |      }
      |    ],
      |    "user": {
      |      "id": "10d2c2d0f3b644b9abea0d9fe80669e4",
      |      "roles": [
      |        {
      |          "tenantId": "MossoCloudFS_123ab1",
      |          "id": "5",
      |          "description": "A Role that allows a user access to keystone Service methods",
      |          "name": "object-store:default"
      |        },
      |        {
      |          "tenantId": "123ab1",
      |          "id": "6",
      |          "description": "A Role that allows a user access to keystone Service methods",
      |          "name": "compute:default"
      |        },
      |        {
      |          "id": "3",
      |          "description": "User Admin Role.",
      |          "name": "identity:user-admin"
      |        }
      |      ],
      |      "name": "demoAuthor",
      |      "RAX-AUTH:defaultRegion": "IAD"
      |    }
      |  }
      |}
    """.stripMargin
  }
}
