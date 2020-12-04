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

package connectors

import java.util.UUID
import javax.inject.Inject
import play.api.Logging
import play.api.http.HeaderNames
import play.api.libs.json._
import config.AppConfig
import models._
import models.getEstate.GetEstateResponse
import models.variation.VariationResponse
import services.Estates5MLDService
import utils.Constants._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future

class EstatesConnector @Inject()(http: HttpClient, config: AppConfig, estatesStoreService: Estates5MLDService) extends Logging {

  private lazy val estatesServiceUrl : String = s"${config.registrationBaseUrl}/estates"

  private lazy val matchEstatesEndpoint : String = s"$estatesServiceUrl/match"

  private lazy val estateRegistrationEndpoint : String = s"$estatesServiceUrl/registration"

  // When reading estates from DES, it's the same endpoint as for trusts.
  // So this must remain "trusts" even though we're reading an estate.
  private lazy val getEstateUrl: String =  s"${config.getEstateBaseUrl}/trusts"

  private def create4MLDEstateEndpointForUtr(utr: String): String = s"$getEstateUrl/registration/$utr"
  private def create5MLDEstateEndpointForUtr(utr: String): String = s"$getEstateUrl/registration/UTR/$utr"

  private lazy val estateVariationsEndpoint : String = s"${config.varyEstateBaseUrl}/estates/variation"

  private val ENVIRONMENT_HEADER = "Environment"
  private val CORRELATION_HEADER = "CorrelationId"

  private def registrationHeaders(correlationId : String) : Seq[(String, String)] =
    Seq(
      HeaderNames.AUTHORIZATION -> s"Bearer ${config.registrationToken}",
      CONTENT_TYPE -> CONTENT_TYPE_JSON,
      ENVIRONMENT_HEADER -> config.registrationEnvironment,
      CORRELATION_HEADER -> correlationId
    )

  def checkExistingEstate(existingEstateCheckRequest: ExistingCheckRequest)
  : Future[ExistingCheckResponse] = {
    val correlationId = UUID.randomUUID().toString

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = registrationHeaders(correlationId))

    logger.info(s"[checkExistingEstate] matching estate for correlationId: $correlationId")

    http.POST[JsValue, ExistingCheckResponse](matchEstatesEndpoint, Json.toJson(existingEstateCheckRequest))
  }

  def registerEstate(registration: EstateRegistration): Future[RegistrationResponse] = {
    val correlationId = UUID.randomUUID().toString

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = registrationHeaders(correlationId))

    logger.info(s"[registerEstate] registering estate for correlationId: $correlationId")

    http.POST[JsValue, RegistrationResponse](estateRegistrationEndpoint, Json.toJson(registration)(EstateRegistration.estateRegistrationWriteToDes))
  }

  def getEstateInfo(utr: String): Future[GetEstateResponse] = {
    val correlationId = UUID.randomUUID().toString

    implicit val hc : HeaderCarrier = HeaderCarrier(extraHeaders = registrationHeaders(correlationId))

    logger.info(s"[getEstateInfo][UTR: $utr] getting playback for estate for correlationId: $correlationId")

    estatesStoreService.is5mldEnabled.flatMap { is5MLD =>
      if (is5MLD) {
        http.GET[GetEstateResponse](create5MLDEstateEndpointForUtr(utr))(GetEstateResponse.httpReads(utr), implicitly[HeaderCarrier](hc), global)
      } else {
        http.GET[GetEstateResponse](create4MLDEstateEndpointForUtr(utr))(GetEstateResponse.httpReads(utr), implicitly[HeaderCarrier](hc), global)
      }
    }
  }

  def estateVariation(estateVariations: JsValue): Future[VariationResponse] = {
    val correlationId = UUID.randomUUID().toString

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = registrationHeaders(correlationId))

    logger.info(s"[estateVariation] submitting estate variation for correlationId: $correlationId")

    http.POST[JsValue, VariationResponse](estateVariationsEndpoint, Json.toJson(estateVariations))
  }
}
