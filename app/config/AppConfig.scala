/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package config

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class AppConfig @Inject()(config: Configuration, servicesConfig: ServicesConfig) {

  val authBaseUrl: String = servicesConfig.baseUrl("auth")

  val graphiteHost: String = config.get[String]("microservice.metrics.graphite.host")

  val ttlInSeconds: Int = config.getOptional[Int]("mongodb.ttlSeconds").getOrElse(4*60*60)

  val estatesNonMigratingBaseUrl : String = servicesConfig.baseUrl("des-estates-non-migrating")
  val estatesBaseUrl : String = servicesConfig.baseUrl("des-estates")

  val getEstateBaseUrl : String = servicesConfig.baseUrl("des-estates-playback")
  val varyEstateBaseUrl : String = servicesConfig.baseUrl("des-estates-variation")
  val estatesStoreBaseUrl : String = servicesConfig.baseUrl("estates-store")

  val desEnvironmentNonMigrating : String = loadConfig("microservice.services.des-estates-non-migrating.environment")
  val desTokenNonMigrating : String = loadConfig("microservice.services.des-estates-non-migrating.token")

  val desEnvironment : String = loadConfig("microservice.services.des-estates.environment")
  val desToken : String = loadConfig("microservice.services.des-estates.token")

  val taxEnrolmentsBaseUrl : String = servicesConfig.baseUrl("tax-enrolments")
  val taxEnrolmentsPayloadBodyCallback : String = loadConfig("microservice.services.tax-enrolments.callback")

  val delayToConnectTaxEnrolment : Int = loadConfig("microservice.services.estates.delayToConnectTaxEnrolment").toInt

  val estatesApiRegistrationSchema : String  = "/resources/schemas/4MLD/estates-api-schema-5.0.json"
  val variationsApiSchema: String = "/resources/schemas/4MLD/variations-api-schema-4.0.json"

  val maxRetry : Int = loadConfig("microservice.services.estates.maxRetry").toInt

  val auditingEnabled : Boolean = loadConfig("microservice.services.estates.features.auditing.enabled").toBoolean

  private def loadConfig(key: String) = config.getOptional[String](key).getOrElse(
    throw new Exception(s"Missing configuration key : $key")
  )

}
