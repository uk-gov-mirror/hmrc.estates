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

package uk.gov.hmrc.estates.config

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class AppConfig @Inject()(config: Configuration, servicesConfig: ServicesConfig) {

  val authBaseUrl: String = servicesConfig.baseUrl("auth")

  val auditingEnabled: Boolean = config.get[Boolean]("auditing.enabled")
  val graphiteHost: String     = config.get[String]("microservice.metrics.graphite.host")

  val ttlInSeconds: Int = config.getOptional[Int]("mongodb.ttlSeconds").getOrElse(4*60*60)

  val desTrustsUrl : String = servicesConfig.baseUrl("des-trusts")
  val desEstatesUrl : String = servicesConfig.baseUrl("des-estates")

  val getTrustOrEstateUrl : String = servicesConfig.baseUrl("des-display-trust-or-estate")

  val varyTrustOrEstateUrl : String = servicesConfig.baseUrl("des-vary-trust-or-estate")

  val desEnvironment : String = loadConfig("microservice.services.des-estates.environment")
  val desToken : String = loadConfig("microservice.services.des-estates.token")

  val taxEnrolmentsUrl : String = servicesConfig.baseUrl("tax-enrolments")
  val taxEnrolmentsPayloadBodyServiceName : String = loadConfig("microservice.services.tax-enrolments.serviceName")
  val taxEnrolmentsPayloadBodyCallback : String = loadConfig("microservice.services.tax-enrolments.callback")
  val delayToConnectTaxEnrolment : Int = loadConfig("microservice.services.estates.delayToConnectTaxEnrolment").toInt

  val estatesApiRegistrationSchema : String  = "/resources/schemas/estates-api-schema-5.0.json"

  val maxRetry : Int = loadConfig("microservice.services.estates.maxRetry").toInt

  private def loadConfig(key: String) = config.getOptional[String](key).getOrElse(
    throw new Exception(s"Missing configuration key : $key")
  )

}
