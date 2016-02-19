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
  private final val FOYER_STATUS = "foyerStatus"
  private final val FOYER_STATUS_DESCRIPTION = "foyerStatusDescription"

  override def getCommandToken: String = {
    "verify-try-it-now"
  }

  override def getCommandDescription: String = {
    "verifies that tenant-less try-it-now traffic is supported by your Repose configuration"
  }

  override def perform(lintConfig: LintConfig): Unit = {
    val reposeVersion = lintConfig.reposeVersion

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

    def checkAuthN(filters: NodeSeq): JsValue = {
      // These case classes are intermediate storage for data before it is transformed into JSON.
      // Defaults are provided to reduce unnecessary code. The default value for each individual check is set to the
      // failing state to prevent false positives in case a bug exists.
      case class AuthNFilterCheck(filteredByUriRegex: Boolean = true,
                                  missingConfiguration: Boolean = true,
                                  inTenantedMode: Boolean = false,
                                  foyerAsServiceAdmin: Boolean = false,
                                  foyerAsIgnoreTenant: Boolean = false,
                                  foyerStatus: FoyerStatus.FoyerStatus = FoyerStatus.Unknown)

      case class AuthNCheck(listedInSystemModel: Boolean = false,
                            authNFilterChecks: Seq[AuthNFilterCheck] = Seq.empty,
                            foyerStatus: FoyerStatus.FoyerStatus = FoyerStatus.Unknown)

      // These Writes define the transformation from the above case classes into Play JSON objects which in turn can
      // be written as strings.
      implicit val AuthNFilterCheckWrites = new Writes[AuthNFilterCheck] {
        override def writes(anfc: AuthNFilterCheck): JsValue = Json.obj(
          FOYER_STATUS -> anfc.foyerStatus.toString,
          FOYER_STATUS_DESCRIPTION -> describeFoyerStatus(anfc.foyerStatus),
          "filteredByUriRegex" -> anfc.filteredByUriRegex,
          "missingConfiguration" -> anfc.missingConfiguration,
          "inTenantedMode" -> anfc.inTenantedMode,
          "foyerAsServiceAdmin" -> anfc.foyerAsServiceAdmin,
          "foyerAsIgnoreTenant" -> anfc.foyerAsIgnoreTenant
        )
      }

      implicit val AuthNCheckWrites = new Writes[AuthNCheck] {
        override def writes(anc: AuthNCheck): JsValue = Json.obj(
          FOYER_STATUS -> anc.foyerStatus.toString,
          FOYER_STATUS_DESCRIPTION -> describeFoyerStatus(anc.foyerStatus),
          "filterName" -> AUTH_N_FILTER_NAME,
          "filters" -> anc.authNFilterChecks.map(anfc => Json.arr(Json.toJson(anfc))).fold(JsArray())((l, r) => l ++ r)
        )
      }

      var check = AuthNCheck(foyerStatus = FoyerStatus.Allowed)
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

            val isFoyerServiceAdmin = (configRoot \ "openstack-auth" \ "service-admin-roles" \ "role").exists(node => node.text.equals("foyer"))
            if (isFoyerServiceAdmin) {
              filterCheck = filterCheck.copy(foyerAsServiceAdmin = true)
            }

            val isFoyerIgnoreTenant = (configRoot \ "openstack-auth" \ "ignore-tenant-roles" \ "role").exists(node => node.text.equals("foyer"))
            if (isFoyerIgnoreTenant) {
              filterCheck = filterCheck.copy(foyerAsIgnoreTenant = true)
            }
          }

          // This is our truth table, defining "good" and "bad" states across versions.
          if (filterCheck.filteredByUriRegex) {
            filterCheck = filterCheck.copy(foyerStatus = FoyerStatus.Unknown)
          } else if (filterCheck.missingConfiguration) {
            filterCheck = filterCheck.copy(foyerStatus = FoyerStatus.NotAllowed)
          } else if (!filterCheck.inTenantedMode) {
            filterCheck = filterCheck.copy(foyerStatus = FoyerStatus.Allowed)
          } else if (versionLessThan("4.1.0") && filterCheck.foyerAsServiceAdmin) {
            filterCheck = filterCheck.copy(foyerStatus = FoyerStatus.Allowed)
          } else if (versionGreaterThanOrEqualTo("4.1.0") && versionLessThan("7.1.4.0") && filterCheck.foyerAsServiceAdmin && filterCheck.foyerAsIgnoreTenant) {
            filterCheck = filterCheck.copy(foyerStatus = FoyerStatus.Allowed)
          } else if (versionGreaterThanOrEqualTo("7.1.4.0") && filterCheck.foyerAsServiceAdmin || filterCheck.foyerAsIgnoreTenant) {
            filterCheck = filterCheck.copy(foyerStatus = FoyerStatus.Allowed)
          } else {
            filterCheck = filterCheck.copy(foyerStatus = FoyerStatus.NotAllowed)
          }

          filterCheck
        }

        // This provides a status for the component (i.e., client-auth-n) as a whole, while the status for each
        // filter is provided above.
        val checkFoyerStatus = determineFoyerStatus(authNFilterChecks.map(_.foyerStatus))

        check = check.copy(authNFilterChecks = authNFilterChecks, foyerStatus = checkFoyerStatus)
      }

      // Transform the data into JSON.
      Json.toJson(check)
    }

    def checkAuthZ(filters: NodeSeq): JsValue = {
      case class AuthZFilterCheck(filteredByUriRegex: Boolean = true,
                                  missingConfiguration: Boolean = true,
                                  foyerAsIgnoreTenant: Boolean = false,
                                  foyerStatus: FoyerStatus.FoyerStatus = FoyerStatus.Unknown)

      case class AuthZCheck(listedInSystemModel: Boolean = false,
                            authZFilterChecks: Seq[AuthZFilterCheck] = Seq.empty,
                            foyerStatus: FoyerStatus.FoyerStatus = FoyerStatus.Unknown)

      implicit val AuthZFilterCheckWrites = new Writes[AuthZFilterCheck] {
        override def writes(azfc: AuthZFilterCheck): JsValue = Json.obj(
          FOYER_STATUS -> azfc.foyerStatus.toString,
          FOYER_STATUS_DESCRIPTION -> describeFoyerStatus(azfc.foyerStatus),
          "filteredByUriRegex" -> azfc.filteredByUriRegex,
          "missingConfiguration" -> azfc.missingConfiguration,
          "foyerAsIgnoreTenant" -> azfc.foyerAsIgnoreTenant
        )
      }

      implicit val AuthZCheckWrites = new Writes[AuthZCheck] {
        override def writes(azc: AuthZCheck): JsValue = Json.obj(
          FOYER_STATUS -> azc.foyerStatus.toString,
          FOYER_STATUS_DESCRIPTION -> describeFoyerStatus(azc.foyerStatus),
          "filterName" -> AUTH_Z_FILTER_NAME,
          "filters" -> azc.authZFilterChecks.map(anfc => Json.arr(Json.toJson(anfc))).fold(JsArray())((l, r) => l ++ r)
        )
      }

      var check = AuthZCheck(foyerStatus = FoyerStatus.Allowed)
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
            if (ignoreTenantRoles.exists(node => node.text.equals("foyer"))) {
              filterCheck = filterCheck.copy(foyerAsIgnoreTenant = true)
            }
          }

          if (filterCheck.filteredByUriRegex) {
            filterCheck = filterCheck.copy(foyerStatus = FoyerStatus.Unknown)
          } else if (filterCheck.missingConfiguration) {
            filterCheck = filterCheck.copy(foyerStatus = FoyerStatus.NotAllowed)
          } else if (versionLessThan("4.1.0")) {
            filterCheck = filterCheck.copy(foyerStatus = FoyerStatus.AllowedWithAuthorization)
          } else if (filterCheck.foyerAsIgnoreTenant) {
            filterCheck = filterCheck.copy(foyerStatus = FoyerStatus.AllowedWithoutAuthorization)
          } else if (!filterCheck.foyerAsIgnoreTenant) {
            filterCheck = filterCheck.copy(foyerStatus = FoyerStatus.AllowedWithAuthorization)
          }

          filterCheck
        }

        val checkFoyerStatus = determineFoyerStatus(authZFilterChecks.map(_.foyerStatus))

        check = check.copy(authZFilterChecks = authZFilterChecks, foyerStatus = checkFoyerStatus)
      }

      Json.toJson(check)
    }

    def checkKeystoneV2(filters: NodeSeq): JsValue = {
      case class KeystoneV2FilterCheck(filteredByUriRegex: Boolean = true,
                                       missingConfiguration: Boolean = true,
                                       inTenantedMode: Boolean = false,
                                       foyerAsPreAuth: Boolean = false,
                                       catalogAuthorization: Boolean = false,
                                       foyerStatus: FoyerStatus.FoyerStatus = FoyerStatus.Unknown)

      case class KeystoneV2Check(listedInSystemModel: Boolean = false,
                                 keystoneV2FilterChecks: Seq[KeystoneV2FilterCheck] = Seq.empty,
                                 foyerStatus: FoyerStatus.FoyerStatus = FoyerStatus.Unknown)

      implicit val KeystoneV2FilterCheckWrites = new Writes[KeystoneV2FilterCheck] {
        override def writes(kfc: KeystoneV2FilterCheck): JsValue = Json.obj(
          FOYER_STATUS -> kfc.foyerStatus.toString,
          FOYER_STATUS_DESCRIPTION -> describeFoyerStatus(kfc.foyerStatus),
          "filteredByUriRegex" -> kfc.filteredByUriRegex,
          "missingConfiguration" -> kfc.missingConfiguration,
          "inTenantedMode" -> kfc.inTenantedMode,
          "foyerAsPreAuthorized" -> kfc.foyerAsPreAuth,
          "catalogAuthorization" -> kfc.catalogAuthorization
        )
      }

      implicit val KeystoneV2CheckWrites = new Writes[KeystoneV2Check] {
        override def writes(kc: KeystoneV2Check): JsValue = Json.obj(
          FOYER_STATUS -> kc.foyerStatus.toString,
          FOYER_STATUS_DESCRIPTION -> describeFoyerStatus(kc.foyerStatus),
          "filterName" -> KEYSTONEV2_FILTER_NAME,
          "filters" -> kc.keystoneV2FilterChecks.map(anfc => Json.arr(Json.toJson(anfc))).fold(JsArray())((l, r) => l ++ r)
        )
      }

      var check = KeystoneV2Check(foyerStatus = FoyerStatus.Allowed)
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

            if ((configRoot \ "pre-authorized-roles" \ "role").exists(node => node.text.equals("foyer"))) {
              filterCheck = filterCheck.copy(foyerAsPreAuth = true)
            }

            if ((configRoot \ "require-service-endpoint").nonEmpty) {
              filterCheck = filterCheck.copy(catalogAuthorization = true)
            }
          }

          if (filterCheck.filteredByUriRegex) {
            filterCheck = filterCheck.copy(foyerStatus = FoyerStatus.Unknown)
          } else if (filterCheck.missingConfiguration || versionLessThan("7.1.5.1")) {
            filterCheck = filterCheck.copy(foyerStatus = FoyerStatus.NotAllowed)
          } else if (!filterCheck.inTenantedMode && !filterCheck.catalogAuthorization) {
            filterCheck = filterCheck.copy(foyerStatus = FoyerStatus.AllowedWithoutAuthorization)
          } else if (filterCheck.foyerAsPreAuth) {
            filterCheck = filterCheck.copy(foyerStatus = FoyerStatus.AllowedWithoutAuthorization)
          } else if (filterCheck.inTenantedMode && !filterCheck.foyerAsPreAuth) {
            filterCheck = filterCheck.copy(foyerStatus = FoyerStatus.NotAllowed)
          } else if (!filterCheck.inTenantedMode && !filterCheck.foyerAsPreAuth) {
            filterCheck = filterCheck.copy(foyerStatus = FoyerStatus.AllowedWithAuthorization)
          }

          filterCheck
        }

        val checkFoyerStatus = determineFoyerStatus(keystoneV2FilterChecks.map(_.foyerStatus))

        check = check.copy(keystoneV2FilterChecks = keystoneV2FilterChecks, foyerStatus = checkFoyerStatus)
      }

      Json.toJson(check)
    }

    def checkIdentityV3(filters: NodeSeq): JsValue = {
      case class IdentityV3FilterCheck(filteredByUriRegex: Boolean = true,
                                       missingConfiguration: Boolean = true,
                                       inTenantedMode: Boolean = false,
                                       foyerAsBypassTenant: Boolean = false,
                                       catalogAuthorization: Boolean = false,
                                       foyerStatus: FoyerStatus.FoyerStatus = FoyerStatus.Unknown)

      case class IdentityV3Check(listedInSystemModel: Boolean = false,
                                 identityV3FilterChecks: Seq[IdentityV3FilterCheck] = Seq.empty,
                                 foyerStatus: FoyerStatus.FoyerStatus = FoyerStatus.Unknown)

      implicit val IdentityV3FilterCheckWrites = new Writes[IdentityV3FilterCheck] {
        override def writes(ifc: IdentityV3FilterCheck): JsValue = Json.obj(
          FOYER_STATUS -> ifc.foyerStatus.toString,
          FOYER_STATUS_DESCRIPTION -> describeFoyerStatus(ifc.foyerStatus),
          "filteredByUriRegex" -> ifc.filteredByUriRegex,
          "missingConfiguration" -> ifc.missingConfiguration,
          "inTenantedMode" -> ifc.inTenantedMode,
          "foyerAsBypassTenant" -> ifc.foyerAsBypassTenant,
          "catalogAuthorization" -> ifc.catalogAuthorization
        )
      }

      implicit val IdentityV3CheckWrites = new Writes[IdentityV3Check] {
        override def writes(ic: IdentityV3Check): JsValue = Json.obj(
          FOYER_STATUS -> ic.foyerStatus.toString,
          FOYER_STATUS_DESCRIPTION -> describeFoyerStatus(ic.foyerStatus),
          "filterName" -> IDENTITYV3_FILTER_NAME,
          "filters" -> ic.identityV3FilterChecks.map(anfc => Json.arr(Json.toJson(anfc))).fold(JsArray())((l, r) => l ++ r)
        )
      }

      var check = IdentityV3Check(foyerStatus = FoyerStatus.Allowed)
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

            if ((configRoot \ "roles-which-bypass-project-id-check" \ "role").exists(node => node.text.equals("foyer"))) {
              filterCheck = filterCheck.copy(foyerAsBypassTenant = true)
            }

            if ((configRoot \ "service-endpoint").nonEmpty) {
              filterCheck = filterCheck.copy(catalogAuthorization = true)
            }
          }

          if (filterCheck.filteredByUriRegex) {
            filterCheck = filterCheck.copy(foyerStatus = FoyerStatus.Unknown)
          } else if (filterCheck.missingConfiguration || versionLessThan("7.0.0.0")) {
            filterCheck = filterCheck.copy(foyerStatus = FoyerStatus.NotAllowed)
          } else if (!filterCheck.inTenantedMode && !filterCheck.catalogAuthorization) {
            filterCheck = filterCheck.copy(foyerStatus = FoyerStatus.AllowedWithoutAuthorization)
          } else if (!filterCheck.foyerAsBypassTenant) {
            filterCheck = filterCheck.copy(foyerStatus = FoyerStatus.NotAllowed)
          } else if (filterCheck.foyerAsBypassTenant && !filterCheck.catalogAuthorization) {
            filterCheck = filterCheck.copy(foyerStatus = FoyerStatus.AllowedWithoutAuthorization)
          } else if (filterCheck.catalogAuthorization) {
            filterCheck = filterCheck.copy(foyerStatus = FoyerStatus.AllowedWithAuthorization)
          }

          filterCheck
        }

        val checkFoyerStatus = determineFoyerStatus(identityV3FilterChecks.map(_.foyerStatus))

        check = check.copy(identityV3FilterChecks = identityV3FilterChecks, foyerStatus = checkFoyerStatus)
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
      val clusterFoyerStatus = determineFoyerStatus(Seq(
        FoyerStatus.withName((authNCheckResult \ FOYER_STATUS).as[String]),
        FoyerStatus.withName((authZCheckResult \ FOYER_STATUS).as[String]),
        FoyerStatus.withName((keystoneV2CheckResult \ FOYER_STATUS).as[String]),
        FoyerStatus.withName((identityV3CheckResult \ FOYER_STATUS).as[String])
      ))

      Json.obj("clusterId" -> (cluster \ "@id").head.text,
        FOYER_STATUS -> clusterFoyerStatus.toString,
        FOYER_STATUS_DESCRIPTION -> describeFoyerStatus(clusterFoyerStatus),
        "authNCheck" -> authNCheckResult,
        "authZCheck" -> authZCheckResult,
        "keystoneV2Check" -> keystoneV2CheckResult,
        "identityV3Check" -> identityV3CheckResult
      )
    }
    val clustersArray = clusterJsonObjects.foldLeft(Json.arr())((arr, obj) => arr :+ obj)
    val fullSystemFoyerStatus = determineFoyerStatus(clusterJsonObjects.map(cluster =>
      FoyerStatus.withName((cluster \ FOYER_STATUS).as[String])))
    val fullSystemFoyerStatusDescription = describeFoyerStatus(fullSystemFoyerStatus)
    val fullSystemJson = Json.obj(
      FOYER_STATUS -> fullSystemFoyerStatus.toString,
      FOYER_STATUS_DESCRIPTION -> fullSystemFoyerStatusDescription,
      "clusters" -> clustersArray
    )

    if (lintConfig.verbose) {
      println(Json.prettyPrint(fullSystemJson))
    } else {
      println(fullSystemFoyerStatusDescription)
    }
  }

  private def determineFoyerStatus(foyerStatuses: Seq[FoyerStatus.FoyerStatus]): FoyerStatus.FoyerStatus = {
    if (foyerStatuses.contains(FoyerStatus.NotAllowed)) {
      FoyerStatus.NotAllowed
    } else if (foyerStatuses.contains(FoyerStatus.Unknown)) {
      FoyerStatus.Unknown
    } else if (foyerStatuses.contains(FoyerStatus.AllowedWithAuthorization)) {
      FoyerStatus.AllowedWithAuthorization
    } else if (foyerStatuses.contains(FoyerStatus.AllowedWithoutAuthorization)) {
      FoyerStatus.AllowedWithoutAuthorization
    } else if (foyerStatuses.contains(FoyerStatus.Allowed)) {
      FoyerStatus.Allowed
    } else {
      FoyerStatus.Unknown
    }
  }

  private def describeFoyerStatus(foyerStatus: FoyerStatus.FoyerStatus): String = {
    foyerStatus match {
      case FoyerStatus.Allowed =>
        "Users with the 'foyer' Identity role WILL pass through this component"
      case FoyerStatus.AllowedWithAuthorization =>
        "Users with the 'foyer' Identity role WILL pass through this component IF AND ONLY IF their Identity " +
          "service catalog contains an endpoint required by the authorization component"
      case FoyerStatus.AllowedWithoutAuthorization =>
        "Users with the 'foyer' Identity role WILL pass through this component BUT authorization checks will not " +
          "be performed"
      case FoyerStatus.NotAllowed =>
        "Users with the 'foyer' Identity role WILL NOT pass through this component"
      case FoyerStatus.Unknown =>
        "Users with the 'foyer' Identity role MAY OR MAY NOT pass through this component"
    }
  }

  object FoyerStatus extends Enumeration {
    type FoyerStatus = Value
    val Allowed, AllowedWithAuthorization, AllowedWithoutAuthorization, NotAllowed, Unknown = Value
  }

}
