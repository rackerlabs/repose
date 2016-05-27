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
package org.openrepose.lint.commands

import java.io.{File, FileNotFoundException}

import org.openrepose.lint.LintConfig
import play.api.libs.json._

import scala.xml.{NodeSeq, XML}

/**
  * Ensures that the Repose configuration within a provided directory supports "Try-It-Now" traffic.
  */
object VerifyTryItNowCommand extends Command {

  private final val SYSTEM_MODEL_FILENAME = "system-model.cfg.xml"
  private final val AUTH_Z_FILTER_NAME = "client-authorization"
  private final val DEFAULT_AUTH_Z_FILENAME = "openstack-authorization.cfg.xml"
  private final val AUTH_N_FILTER_NAME = "client-auth"
  private final val DEFAULT_AUTH_N_FILENAME = "client-auth-n.cfg.xml"
  private final val KEYSTONEV2_FILTER_NAME = "keystone-v2"
  private final val DEFAULT_KEYSTONEV2_FILENAME = "keystone-v2.cfg.xml"
  private final val IDENTITYV3_FILTER_NAME = "openstack-identity-v3"
  private final val DEFAULT_IDENTITYV3_FILENAME = "openstack-identity-v3.cfg.xml"

  override def getCommandToken: String = {
    "verify-try-it-now"
  }

  override def getCommandDescription: String = {
    "verifies that tenant-less try-it-now traffic is supported by your Repose configuration"
  }

  override def perform(lintConfig: LintConfig): Unit = {
    val reposeVersion = lintConfig.reposeVersion
    val roleName = lintConfig.roleName
    val roleStatus = s"${roleName}Status"
    val roleStatusDescription = s"${roleName}StatusDescription"

    def versionGreaterThanOrEqualTo(otherVersion: String): Boolean = {
      val reposeVersionSplit = reposeVersion.split('.').map(_.toInt)
      val otherVersionSplit = otherVersion.split('.').map(_.toInt)

      val isReposeVersionShorter = reposeVersionSplit.length < otherVersionSplit.length
      val comparisonIdx = if (isReposeVersionShorter) {
        reposeVersionSplit.indices
          .dropWhile(i => reposeVersionSplit(i) == otherVersionSplit(i))
          .headOption
      } else {
        otherVersionSplit.indices
          .dropWhile(i => reposeVersionSplit(i) == otherVersionSplit(i))
          .headOption
      }

      comparisonIdx.map(i => reposeVersionSplit(i) > otherVersionSplit(i)).getOrElse(!isReposeVersionShorter)
    }

    def versionLessThan(otherVersion: String): Boolean = !versionGreaterThanOrEqualTo(otherVersion)

    def getFile(filename: String): Option[File] = {
      val file = new File(lintConfig.configDir, filename)

      if (file.exists && file.isFile && file.canRead) {
        Some(file)
      } else {
        None
      }
    }

    def getClusters(systemModel: File): NodeSeq = {
      val systemModelRoot = XML.loadFile(systemModel)
      systemModelRoot \ "repose-cluster"
    }

    def getFiltersNamed(filterName: String)(filters: NodeSeq): NodeSeq = {
      filters.filter(filter => (filter \ "@name").text.equals(filterName))
    }

    def determineRoleStatus(roleStatuses: Seq[RoleStatus.RoleStatus]): RoleStatus.RoleStatus = {
      if (roleStatuses.contains(RoleStatus.NotAllowed)) {
        RoleStatus.NotAllowed
      } else if (roleStatuses.contains(RoleStatus.Unknown)) {
        RoleStatus.Unknown
      } else if (roleStatuses.contains(RoleStatus.AllowedWithAuthorization)) {
        RoleStatus.AllowedWithAuthorization
      } else if (roleStatuses.contains(RoleStatus.AllowedWithoutAuthorization)) {
        RoleStatus.AllowedWithoutAuthorization
      } else if (roleStatuses.contains(RoleStatus.Allowed)) {
        RoleStatus.Allowed
      } else {
        RoleStatus.Unknown
      }
    }

    def describeRoleStatus(roleStatus: RoleStatus.RoleStatus): String = {
      roleStatus match {
        case RoleStatus.Allowed =>
          s"Users with the '$roleName' Identity role WILL pass through this component"
        case RoleStatus.AllowedWithAuthorization =>
          s"Users with the '$roleName' Identity role WILL pass through this component IF AND ONLY IF their Identity " +
            "service catalog contains an endpoint required by the authorization component"
        case RoleStatus.AllowedWithoutAuthorization =>
          s"Users with the '$roleName' Identity role WILL pass through this component BUT authorization checks will not " +
            "be performed"
        case RoleStatus.NotAllowed =>
          s"Users with the '$roleName' Identity role WILL NOT pass through this component"
        case RoleStatus.Unknown =>
          s"Users with the '$roleName' Identity role MAY OR MAY NOT pass through this component"
      }
    }

    def checkAuthN(filters: NodeSeq): JsValue = {
      // These case classes are intermediate storage for data before it is transformed into JSON.
      // Defaults are provided to reduce unnecessary code. The default value for each individual check is set to the
      // failing state to prevent false positives in case a bug exists.
      case class AuthNFilterCheck(filteredByUriRegex: Boolean = true,
                                  missingConfiguration: Boolean = true,
                                  inTenantedMode: Boolean = false,
                                  roleAsServiceAdmin: Boolean = false,
                                  roleAsIgnoreTenant: Boolean = false,
                                  roleStatus: RoleStatus.RoleStatus = RoleStatus.Unknown)

      case class AuthNCheck(listedInSystemModel: Boolean = false,
                            authNFilterChecks: Seq[AuthNFilterCheck] = Seq.empty,
                            roleStatus: RoleStatus.RoleStatus = RoleStatus.Unknown)

      // These Writes define the transformation from the above case classes into Play JSON objects which in turn can
      // be written as strings.
      implicit val AuthNFilterCheckWrites = new Writes[AuthNFilterCheck] {
        override def writes(anfc: AuthNFilterCheck): JsValue = Json.obj(
          roleStatus -> anfc.roleStatus.toString,
          roleStatusDescription -> describeRoleStatus(anfc.roleStatus),
          "filteredByUriRegex" -> anfc.filteredByUriRegex,
          "missingConfiguration" -> anfc.missingConfiguration,
          "inTenantedMode" -> anfc.inTenantedMode,
          s"${roleName}AsServiceAdmin" -> anfc.roleAsServiceAdmin,
          s"${roleName}AsIgnoreTenant" -> anfc.roleAsIgnoreTenant
        )
      }

      implicit val AuthNCheckWrites = new Writes[AuthNCheck] {
        override def writes(anc: AuthNCheck): JsValue = Json.obj(
          roleStatus -> anc.roleStatus.toString,
          roleStatusDescription -> describeRoleStatus(anc.roleStatus),
          "filterName" -> AUTH_N_FILTER_NAME,
          "filters" -> anc.authNFilterChecks.map(anfc => Json.arr(Json.toJson(anfc))).fold(JsArray())((l, r) => l ++ r)
        )
      }

      var check = AuthNCheck(roleStatus = RoleStatus.Allowed)
      val authNFilters = getFiltersNamed(AUTH_N_FILTER_NAME)(filters)

      // If we have filters, perform a set of checks on their respective configurations.
      if (authNFilters.nonEmpty) {
        val authNFilterChecks = authNFilters map { filter =>
          var filterCheck = AuthNFilterCheck()

          val declaredConfig = (filter \ "@configuration").map(_.text)
          val configFile = declaredConfig.headOption.getOrElse(DEFAULT_AUTH_N_FILENAME)
          val config = getFile(configFile).map(XML.loadFile)

          if ((filter \ "@uri-regex").isEmpty) {
            filterCheck = filterCheck.copy(filteredByUriRegex = false)
          }

          config foreach { configRoot =>
            filterCheck = filterCheck.copy(missingConfiguration = false)

            val tenanted = (configRoot \ "openstack-auth" \ "@tenanted").map(node => node.text.toBoolean).headOption.getOrElse(true)
            if (tenanted) {
              filterCheck = filterCheck.copy(inTenantedMode = true)
            }

            val isRoleServiceAdmin = (configRoot \ "openstack-auth" \ "service-admin-roles" \ "role").exists(node => node.text.equals(roleName))
            if (isRoleServiceAdmin) {
              filterCheck = filterCheck.copy(roleAsServiceAdmin = true)
            }

            val isRoleIgnoreTenant = (configRoot \ "openstack-auth" \ "ignore-tenant-roles" \ "role").exists(node => node.text.equals(roleName))
            if (isRoleIgnoreTenant) {
              filterCheck = filterCheck.copy(roleAsIgnoreTenant = true)
            }
          }

          // This is our truth table, defining "good" and "bad" states across versions.
          if (filterCheck.filteredByUriRegex) {
            filterCheck = filterCheck.copy(roleStatus = RoleStatus.Unknown)
          } else if (filterCheck.missingConfiguration) {
            filterCheck = filterCheck.copy(roleStatus = RoleStatus.NotAllowed)
          } else if (!filterCheck.inTenantedMode) {
            filterCheck = filterCheck.copy(roleStatus = RoleStatus.Allowed)
          } else if (versionLessThan("4.1.0") && filterCheck.roleAsServiceAdmin) {
            filterCheck = filterCheck.copy(roleStatus = RoleStatus.Allowed)
          } else if (versionGreaterThanOrEqualTo("4.1.0") && versionLessThan("7.1.4.0") && filterCheck.roleAsServiceAdmin && filterCheck.roleAsIgnoreTenant) {
            filterCheck = filterCheck.copy(roleStatus = RoleStatus.Allowed)
          } else if (versionGreaterThanOrEqualTo("7.1.4.0") && filterCheck.roleAsServiceAdmin || filterCheck.roleAsIgnoreTenant) {
            filterCheck = filterCheck.copy(roleStatus = RoleStatus.Allowed)
          } else {
            filterCheck = filterCheck.copy(roleStatus = RoleStatus.NotAllowed)
          }

          filterCheck
        }

        // This provides a status for the component (i.e., client-auth-n) as a whole, while the status for each
        // filter is provided above.
        val checkRoleStatus = determineRoleStatus(authNFilterChecks.map(_.roleStatus))

        check = check.copy(authNFilterChecks = authNFilterChecks, roleStatus = checkRoleStatus)
      }

      // Transform the data into JSON.
      Json.toJson(check)
    }

    def checkAuthZ(filters: NodeSeq): JsValue = {
      case class AuthZFilterCheck(filteredByUriRegex: Boolean = true,
                                  missingConfiguration: Boolean = true,
                                  roleAsIgnoreTenant: Boolean = false,
                                  roleStatus: RoleStatus.RoleStatus = RoleStatus.Unknown)

      case class AuthZCheck(listedInSystemModel: Boolean = false,
                            authZFilterChecks: Seq[AuthZFilterCheck] = Seq.empty,
                            roleStatus: RoleStatus.RoleStatus = RoleStatus.Unknown)

      implicit val AuthZFilterCheckWrites = new Writes[AuthZFilterCheck] {
        override def writes(azfc: AuthZFilterCheck): JsValue = Json.obj(
          roleStatus -> azfc.roleStatus.toString,
          roleStatusDescription -> describeRoleStatus(azfc.roleStatus),
          "filteredByUriRegex" -> azfc.filteredByUriRegex,
          "missingConfiguration" -> azfc.missingConfiguration,
          s"${roleName}AsIgnoreTenant" -> azfc.roleAsIgnoreTenant
        )
      }

      implicit val AuthZCheckWrites = new Writes[AuthZCheck] {
        override def writes(azc: AuthZCheck): JsValue = Json.obj(
          roleStatus -> azc.roleStatus.toString,
          roleStatusDescription -> describeRoleStatus(azc.roleStatus),
          "filterName" -> AUTH_Z_FILTER_NAME,
          "filters" -> azc.authZFilterChecks.map(anfc => Json.arr(Json.toJson(anfc))).fold(JsArray())((l, r) => l ++ r)
        )
      }

      var check = AuthZCheck(roleStatus = RoleStatus.Allowed)
      val authZFilters = getFiltersNamed(AUTH_Z_FILTER_NAME)(filters)

      if (authZFilters.nonEmpty) {
        val authZFilterChecks = authZFilters map { filter =>
          var filterCheck = AuthZFilterCheck()

          val declaredConfig = (filter \ "@configuration").map(_.text)
          val configFile = declaredConfig.headOption.getOrElse(DEFAULT_AUTH_Z_FILENAME)
          val config = getFile(configFile).map(XML.loadFile)

          if ((filter \ "@uri-regex").isEmpty) {
            filterCheck = filterCheck.copy(filteredByUriRegex = false)
          }

          config foreach { configRoot =>
            filterCheck = filterCheck.copy(missingConfiguration = false)

            val ignoreTenantRoles = (configRoot \ "ignore-tenant-roles" \ "role") ++ (configRoot \ "ignore-tenant-roles" \ "ignore-tenant-role")
            if (ignoreTenantRoles.exists(node => node.text.equals(roleName))) {
              filterCheck = filterCheck.copy(roleAsIgnoreTenant = true)
            }
          }

          if (filterCheck.filteredByUriRegex) {
            filterCheck = filterCheck.copy(roleStatus = RoleStatus.Unknown)
          } else if (filterCheck.missingConfiguration) {
            filterCheck = filterCheck.copy(roleStatus = RoleStatus.NotAllowed)
          } else if (versionLessThan("4.1.0")) {
            filterCheck = filterCheck.copy(roleStatus = RoleStatus.AllowedWithAuthorization)
          } else if (filterCheck.roleAsIgnoreTenant) {
            filterCheck = filterCheck.copy(roleStatus = RoleStatus.AllowedWithoutAuthorization)
          } else if (!filterCheck.roleAsIgnoreTenant) {
            filterCheck = filterCheck.copy(roleStatus = RoleStatus.AllowedWithAuthorization)
          }

          filterCheck
        }

        val checkRoleStatus = determineRoleStatus(authZFilterChecks.map(_.roleStatus))

        check = check.copy(authZFilterChecks = authZFilterChecks, roleStatus = checkRoleStatus)
      }

      Json.toJson(check)
    }

    def checkKeystoneV2(filters: NodeSeq): JsValue = {
      case class KeystoneV2FilterCheck(filteredByUriRegex: Boolean = true,
                                       missingConfiguration: Boolean = true,
                                       inTenantedMode: Boolean = false,
                                       roleAsPreAuth: Boolean = false,
                                       catalogAuthorization: Boolean = false,
                                       roleStatus: RoleStatus.RoleStatus = RoleStatus.Unknown)

      case class KeystoneV2Check(listedInSystemModel: Boolean = false,
                                 keystoneV2FilterChecks: Seq[KeystoneV2FilterCheck] = Seq.empty,
                                 roleStatus: RoleStatus.RoleStatus = RoleStatus.Unknown)

      implicit val KeystoneV2FilterCheckWrites = new Writes[KeystoneV2FilterCheck] {
        override def writes(kfc: KeystoneV2FilterCheck): JsValue = Json.obj(
          roleStatus -> kfc.roleStatus.toString,
          roleStatusDescription -> describeRoleStatus(kfc.roleStatus),
          "filteredByUriRegex" -> kfc.filteredByUriRegex,
          "missingConfiguration" -> kfc.missingConfiguration,
          "inTenantedMode" -> kfc.inTenantedMode,
          s"${roleName}AsPreAuthorized" -> kfc.roleAsPreAuth,
          "catalogAuthorization" -> kfc.catalogAuthorization
        )
      }

      implicit val KeystoneV2CheckWrites = new Writes[KeystoneV2Check] {
        override def writes(kc: KeystoneV2Check): JsValue = Json.obj(
          roleStatus -> kc.roleStatus.toString,
          roleStatusDescription -> describeRoleStatus(kc.roleStatus),
          "filterName" -> KEYSTONEV2_FILTER_NAME,
          "filters" -> kc.keystoneV2FilterChecks.map(anfc => Json.arr(Json.toJson(anfc))).fold(JsArray())((l, r) => l ++ r)
        )
      }

      var check = KeystoneV2Check(roleStatus = RoleStatus.Allowed)
      val keystoneV2Filters = getFiltersNamed(KEYSTONEV2_FILTER_NAME)(filters)

      if (keystoneV2Filters.nonEmpty) {
        val keystoneV2FilterChecks = keystoneV2Filters map { filter =>
          var filterCheck = KeystoneV2FilterCheck()

          val declaredConfig = (filter \ "@configuration").map(_.text)
          val configFile = declaredConfig.headOption.getOrElse(DEFAULT_KEYSTONEV2_FILENAME)
          val config = getFile(configFile).map(XML.loadFile)

          if ((filter \ "@uri-regex").isEmpty) {
            filterCheck = filterCheck.copy(filteredByUriRegex = false)
          }

          config foreach { configRoot =>
            filterCheck = filterCheck.copy(missingConfiguration = false)

            if ((configRoot \ "tenant-handling" \ "validate-tenant").nonEmpty) {
              filterCheck = filterCheck.copy(inTenantedMode = true)
            }

            if ((configRoot \ "pre-authorized-roles" \ "role").exists(node => node.text.equals(roleName))) {
              filterCheck = filterCheck.copy(roleAsPreAuth = true)
            }

            if ((configRoot \ "require-service-endpoint").nonEmpty) {
              filterCheck = filterCheck.copy(catalogAuthorization = true)
            }
          }

          if (filterCheck.filteredByUriRegex) {
            filterCheck = filterCheck.copy(roleStatus = RoleStatus.Unknown)
          } else if (filterCheck.missingConfiguration || versionLessThan("7.1.5.1")) {
            filterCheck = filterCheck.copy(roleStatus = RoleStatus.NotAllowed)
          } else if (!filterCheck.inTenantedMode && !filterCheck.catalogAuthorization) {
            filterCheck = filterCheck.copy(roleStatus = RoleStatus.AllowedWithoutAuthorization)
          } else if (filterCheck.roleAsPreAuth) {
            filterCheck = filterCheck.copy(roleStatus = RoleStatus.AllowedWithoutAuthorization)
          } else if (filterCheck.inTenantedMode && !filterCheck.roleAsPreAuth) {
            filterCheck = filterCheck.copy(roleStatus = RoleStatus.NotAllowed)
          } else if (!filterCheck.inTenantedMode && !filterCheck.roleAsPreAuth) {
            filterCheck = filterCheck.copy(roleStatus = RoleStatus.AllowedWithAuthorization)
          }

          filterCheck
        }

        val checkRoleStatus = determineRoleStatus(keystoneV2FilterChecks.map(_.roleStatus))

        check = check.copy(keystoneV2FilterChecks = keystoneV2FilterChecks, roleStatus = checkRoleStatus)
      }

      Json.toJson(check)
    }

    def checkIdentityV3(filters: NodeSeq): JsValue = {
      case class IdentityV3FilterCheck(filteredByUriRegex: Boolean = true,
                                       missingConfiguration: Boolean = true,
                                       inTenantedMode: Boolean = false,
                                       roleAsBypassTenant: Boolean = false,
                                       catalogAuthorization: Boolean = false,
                                       roleStatus: RoleStatus.RoleStatus = RoleStatus.Unknown)

      case class IdentityV3Check(listedInSystemModel: Boolean = false,
                                 identityV3FilterChecks: Seq[IdentityV3FilterCheck] = Seq.empty,
                                 roleStatus: RoleStatus.RoleStatus = RoleStatus.Unknown)

      implicit val IdentityV3FilterCheckWrites = new Writes[IdentityV3FilterCheck] {
        override def writes(ifc: IdentityV3FilterCheck): JsValue = Json.obj(
          roleStatus -> ifc.roleStatus.toString,
          roleStatusDescription -> describeRoleStatus(ifc.roleStatus),
          "filteredByUriRegex" -> ifc.filteredByUriRegex,
          "missingConfiguration" -> ifc.missingConfiguration,
          "inTenantedMode" -> ifc.inTenantedMode,
          s"${roleName}AsBypassTenant" -> ifc.roleAsBypassTenant,
          "catalogAuthorization" -> ifc.catalogAuthorization
        )
      }

      implicit val IdentityV3CheckWrites = new Writes[IdentityV3Check] {
        override def writes(ic: IdentityV3Check): JsValue = Json.obj(
          roleStatus -> ic.roleStatus.toString,
          roleStatusDescription -> describeRoleStatus(ic.roleStatus),
          "filterName" -> IDENTITYV3_FILTER_NAME,
          "filters" -> ic.identityV3FilterChecks.map(anfc => Json.arr(Json.toJson(anfc))).fold(JsArray())((l, r) => l ++ r)
        )
      }

      var check = IdentityV3Check(roleStatus = RoleStatus.Allowed)
      val identityV3Filters = getFiltersNamed(IDENTITYV3_FILTER_NAME)(filters)

      if (identityV3Filters.nonEmpty) {
        val identityV3FilterChecks = identityV3Filters map { filter =>
          var filterCheck = IdentityV3FilterCheck()

          val declaredConfig = (filter \ "@configuration").map(_.text)
          val configFile = declaredConfig.headOption.getOrElse(DEFAULT_IDENTITYV3_FILENAME)
          val config = getFile(configFile).map(XML.loadFile)

          if ((filter \ "@uri-regex").isEmpty) {
            filterCheck = filterCheck.copy(filteredByUriRegex = false)
          }

          config foreach { configRoot =>
            filterCheck = filterCheck.copy(missingConfiguration = false)

            if ((configRoot \ "validate-project-id-in-uri").nonEmpty) {
              filterCheck = filterCheck.copy(inTenantedMode = true)
            }

            if ((configRoot \ "roles-which-bypass-project-id-check" \ "role").exists(node => node.text.equals(roleName))) {
              filterCheck = filterCheck.copy(roleAsBypassTenant = true)
            }

            if ((configRoot \ "service-endpoint").nonEmpty) {
              filterCheck = filterCheck.copy(catalogAuthorization = true)
            }
          }

          if (filterCheck.filteredByUriRegex) {
            filterCheck = filterCheck.copy(roleStatus = RoleStatus.Unknown)
          } else if (filterCheck.missingConfiguration || versionLessThan("7.0.0.0")) {
            filterCheck = filterCheck.copy(roleStatus = RoleStatus.NotAllowed)
          } else if (!filterCheck.inTenantedMode && !filterCheck.catalogAuthorization) {
            filterCheck = filterCheck.copy(roleStatus = RoleStatus.AllowedWithoutAuthorization)
          } else if (!filterCheck.roleAsBypassTenant) {
            filterCheck = filterCheck.copy(roleStatus = RoleStatus.NotAllowed)
          } else if (filterCheck.roleAsBypassTenant && !filterCheck.catalogAuthorization) {
            filterCheck = filterCheck.copy(roleStatus = RoleStatus.AllowedWithoutAuthorization)
          } else if (filterCheck.catalogAuthorization) {
            filterCheck = filterCheck.copy(roleStatus = RoleStatus.AllowedWithAuthorization)
          }

          filterCheck
        }

        val checkRoleStatus = determineRoleStatus(identityV3FilterChecks.map(_.roleStatus))

        check = check.copy(identityV3FilterChecks = identityV3FilterChecks, roleStatus = checkRoleStatus)
      }

      Json.toJson(check)
    }

    val clusters = getFile(SYSTEM_MODEL_FILENAME).map(getClusters) match {
      case Some(c) => c
      case None => throw new FileNotFoundException("System model configuration file not found")
    }

    val clusterJsonObjects = clusters map { cluster =>
      val filters = cluster \ "filters" \ "filter"

      val authNCheckResult = checkAuthN(filters)
      val authZCheckResult = checkAuthZ(filters)
      val keystoneV2CheckResult = checkKeystoneV2(filters)
      val identityV3CheckResult = checkIdentityV3(filters)
      val clusterRoleStatus = determineRoleStatus(Seq(
        RoleStatus.withName((authNCheckResult \ roleStatus).as[String]),
        RoleStatus.withName((authZCheckResult \ roleStatus).as[String]),
        RoleStatus.withName((keystoneV2CheckResult \ roleStatus).as[String]),
        RoleStatus.withName((identityV3CheckResult \ roleStatus).as[String])
      ))

      Json.obj("clusterId" -> (cluster \ "@id").head.text,
        roleStatus -> clusterRoleStatus.toString,
        roleStatusDescription -> describeRoleStatus(clusterRoleStatus),
        "authNCheck" -> authNCheckResult,
        "authZCheck" -> authZCheckResult,
        "keystoneV2Check" -> keystoneV2CheckResult,
        "identityV3Check" -> identityV3CheckResult
      )
    }
    val clustersArray = clusterJsonObjects.foldLeft(Json.arr())((arr, obj) => arr :+ obj)
    val fullSystemRoleStatus = determineRoleStatus(clusterJsonObjects.map(cluster =>
      RoleStatus.withName((cluster \ roleStatus).as[String])))
    val fullSystemRoleStatusDescription = describeRoleStatus(fullSystemRoleStatus)
    val fullSystemJson = Json.obj(
      roleStatus -> fullSystemRoleStatus.toString,
      roleStatusDescription -> fullSystemRoleStatusDescription,
      "clusters" -> clustersArray
    )

    if (lintConfig.verbose) {
      println(Json.prettyPrint(fullSystemJson))
    } else {
      println(fullSystemRoleStatusDescription)
    }
  }

  object RoleStatus extends Enumeration {
    type RoleStatus = Value
    val Allowed, AllowedWithAuthorization, AllowedWithoutAuthorization, NotAllowed, Unknown = Value
  }

}
