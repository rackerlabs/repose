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
package org.openrepose.filters.keystonev2

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

trait IdentityResponses {

  protected final val VALID_TOKEN = "validToken"
  protected final val VALID_USER_ID = "123"

  def tokenDateFormat(dateTime:DateTime): String = {
    ISODateTimeFormat.dateTime().print(dateTime)
  }

  def adminAuthenticationTokenResponse(token: String = "glibglob",
                                       expires: DateTime = DateTime.now()): String = {
    val formattedTime = tokenDateFormat(expires)

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

  def validateTokenResponse(token: String = VALID_TOKEN,
                            expires: DateTime = DateTime.now().plusDays(1),
                            userId: String = VALID_USER_ID): String = {

    val expiryTime = tokenDateFormat(expires)

    s"""
      |{
      |    "access":{
      |        "token":{
      |            "id":"$token",
      |            "expires":"$expiryTime",
      |            "tenant":{
      |                "id": "345",
      |                "name": "My Project"
      |            }
      |        },
      |        "user":{
      |            "RAX-AUTH:domainId": "909989",
      |            "RAX-AUTH:defaultRegion": "DFW",
      |            "RAX-AUTH:contactId": "abc123",
      |            "id":"$userId",
      |            "name":"testuser",
      |            "roles":[{
      |                    "id":"123",
      |                    "name":"compute:admin"
      |                },
      |                {
      |                    "id":"234",
      |                    "name":"object-store:admin"
      |                }
      |            ]
      |        }
      |    }
      |}
    """.stripMargin
  }

  def validateTokenResponseTenantedRoles(token: String = VALID_TOKEN,
                            expires: DateTime = DateTime.now().plusDays(1),
                            userId: String = VALID_USER_ID): String = {

    val expiryTime = tokenDateFormat(expires)

    s"""
       |{
       |    "access":{
       |        "token":{
       |            "id":"$token",
       |            "expires":"$expiryTime",
       |            "tenant":{
       |                "id": "456",
       |                "name": "My Project"
       |            }
       |        },
       |        "user":{
       |            "RAX-AUTH:defaultRegion": "DFW",
       |            "RAX-AUTH:contactId": "abc123",
       |            "id":"$userId",
       |            "name":"testuser",
       |            "roles":[{
       |                    "id":"123",
       |                    "name":"role:123"
       |                },
       |                {
       |                    "id":"234",
       |                    "tenantId": "234",
       |                    "name":"role:234"
       |                },
       |                {
       |                    "id":"345",
       |                    "tenantId": "345",
       |                    "name":"role:345"
       |                },
       |                {
       |                    "id":"678",
       |                    "tenantId": "678",
       |                    "name":"identity:tenant-access"
       |                },
       |                {
       |                    "id":"789",
       |                    "tenantId": "345",
       |                    "name":"identity:tenant-access"
       |                }
       |            ]
       |        }
       |    }
       |}
    """.stripMargin
  }

  def validateImpersonatedTokenResponse(token: String = VALID_TOKEN,
                                        userId: String = VALID_USER_ID): String = {
    s"""
       |{
       |  "access":{
       |      "token":{
       |          "id":"$token",
       |          "expires":"2010-11-01T03:32:15-05:00",
       |          "tenant":{
       |              "id": "yourTenantID",
       |              "name": "My Project"
       |           }
       |       },
       |
       |      "user":{
       |          "id":"$userId",
       |          "name":"yourUsername",
       |          "roles":[{
       |                     "id":"123",
       |                     "name":"compute:admin"
       |                   },
       |                   {
       |                     "id":"234",
       |                     "name":"object-store:admin"
       |                   }
       |           ]
       |       },
       |
       |       "RAX-AUTH:impersonator":{
       |            "id":"567",
       |            "name":"rick",
       |            "roles":[{
       |                       "id":"123",
       |                       "name":"Racker"
       |                     },
       |                     {
       |                        "id":"234",
       |                        "name":"object-store:admin"
       |                     }
       |           ]
       |       }
       |  }
       |}
    """.stripMargin
  }

  def validateRackerTokenResponse(token: String = VALID_TOKEN,
                                  userId: String = VALID_USER_ID): String = {
    val expiryTime = tokenDateFormat(DateTime.now().plusDays(1))

    s"""
       |{
       |    "access":{
       |        "token":{
       |            "id":"$token",
       |            "expires":"$expiryTime",
       |            "tenant":{
       |                "id": "345",
       |                "name": "My Project"
       |            }
       |        },
       |        "user":{
       |            "RAX-AUTH:defaultRegion": "DFW",
       |            "RAX-AUTH:contactId": "abc123",
       |            "id":"$userId",
       |            "name":"testuser",
       |            "roles":[{
       |                    "id":"123",
       |                    "name":"racker"
       |                }
       |            ]
       |        }
       |    }
       |}
    """.stripMargin
  }

  def validateTokenResponseAuthenticatedBy(token: String = VALID_TOKEN,
                                           expires: DateTime = DateTime.now().plusDays(1),
                                           userId: String = VALID_USER_ID,
                                           authenticatedBy: Seq[String] = Seq.empty): String = {

    val expiryTime = tokenDateFormat(expires)

    s"""
       |{
       |    "access":{
       |        "token":{
       |            "id":"$token",
       |            "expires":"$expiryTime",
       |            "tenant":{
       |                "id": "345",
       |                "name": "My Project"
       |            },
       |            "RAX-AUTH:authenticatedBy":[${authenticatedBy.map('"' + _ + '"').mkString(",")}]
       |        },
       |        "user":{
       |            "RAX-AUTH:defaultRegion": "DFW",
       |            "RAX-AUTH:contactId": "abc123",
       |            "id":"$userId",
       |            "name":"testuser",
       |            "roles":[{
       |                    "id":"123",
       |                    "name":"compute:admin"
       |                },
       |                {
       |                    "id":"234",
       |                    "name":"object-store:admin"
       |                }
       |            ]
       |        }
       |    }
       |}
    """.stripMargin
  }

  def oneEndpointResponse(): String = {
    """
      |{
      |    "endpoints":[{
      |                "id":1,
      |                "tenantId":"1",
      |                "region":"North",
      |                "name": "Compute",
      |                "type":"compute",
      |                "publicURL":"https://compute.north.public.com/v1",
      |                "internalURL":"https://compute.north.internal.com/v1",
      |                "adminURL" : "https://compute.north.internal.com/v1",
      |                "versionId":"1",
      |                "versionInfo":"https://compute.north.public.com/v1/",
      |                "versionList":"https://compute.north.public.com/"
      |            }
      |        ],
      |    "endpoints_links":[]
      |}
    """.stripMargin
  }

  def endpointsResponse(): String = {
    """
      |{
      |    "endpoints":[{
      |                "id":1,
      |                "tenantId":"1",
      |                "region":"North",
      |                "name": "Compute",
      |                "type":"compute",
      |                "publicURL":"https://compute.north.public.com/v1",
      |                "internalURL":"https://compute.north.internal.com/v1",
      |                "adminURL" : "https://compute.north.internal.com/v1",
      |                "versionId":"1",
      |                "versionInfo":"https://compute.north.public.com/v1/",
      |                "versionList":"https://compute.north.public.com/"
      |            },
      |            {
      |                "id":2,
      |                "tenantId":"1",
      |                "region":"South",
      |                "name": "Compute",
      |                "type":"compute",
      |                "publicURL":"https://compute.north.public.com/v1",
      |                "internalURL":"https://compute.north.internal.com/v1",
      |                "adminURL" : "https://compute.north.internal.com/v1",
      |                "versionId":"1",
      |                "versionInfo":"https://compute.north.public.com/v1/",
      |                "versionList":"https://compute.north.public.com/"
      |            },
      |            {
      |                "id":3,
      |                "tenantId":"1",
      |                "region":"East",
      |                "name": "Compute",
      |                "type":"compute",
      |                "publicURL":"https://compute.north.public.com/v1",
      |                "internalURL":"https://compute.north.internal.com/v1",
      |                "adminURL" : "https://compute.north.internal.com/v1",
      |                "versionId":"1",
      |                "versionInfo":"https://compute.north.public.com/v1/",
      |                "versionList":"https://compute.north.public.com/"
      |            },
      |            {
      |                "id":4,
      |                "tenantId":"1",
      |                "region":"West",
      |                "name": "Compute",
      |                "type":"compute",
      |                "publicURL":"https://compute.north.public.com/v1",
      |                "internalURL":"https://compute.north.internal.com/v1",
      |                "adminURL" : "https://compute.north.internal.com/v1",
      |                "versionId":"1",
      |                "versionInfo":"https://compute.north.public.com/v1/",
      |                "versionList":"https://compute.north.public.com/"
      |            },
      |            {
      |                "id":5,
      |                "tenantId":"1",
      |                "region":"Global",
      |                "name": "Compute",
      |                "type":"compute",
      |                "publicURL":"https://compute.north.public.com/v1",
      |                "internalURL":"https://compute.north.internal.com/v1",
      |                "adminURL" : "https://compute.north.internal.com/v1",
      |                "versionId":"1",
      |                "versionInfo":"https://compute.north.public.com/v1/",
      |                "versionList":"https://compute.north.public.com/"
      |            },
      |            {
      |                "id":5,
      |                "tenantId":"1",
      |                "region":"Global",
      |                "name": "Compute",
      |                "type":"compute",
      |                "publicURL":"https://compute.north.public.com/v1/appended/tenantId",
      |                "internalURL":"https://compute.north.internal.com/v1",
      |                "adminURL" : "https://compute.north.internal.com/v1",
      |                "versionId":"1",
      |                "versionInfo":"https://compute.north.public.com/v1/",
      |                "versionList":"https://compute.north.public.com/"
      |            }
      |        ],
      |    "endpoints_links":[]
      |}
    """.stripMargin
  }

  def groupsResponse(): String = {
    """
      |{
      |  "RAX-KSGRP:groups": [
      |    {
      |      "id": "test-group-id",
      |      "description": "Test description"
      |    }
      |  ],
      |  "RAX-KSGRP:groups_links": []
      |}
    """.stripMargin
  }
}
