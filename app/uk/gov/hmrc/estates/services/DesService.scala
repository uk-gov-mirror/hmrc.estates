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

package uk.gov.hmrc.estates.services

import javax.inject.Inject
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.estates.connectors.DesConnector
import uk.gov.hmrc.estates.exceptions.InternalServerErrorException
import uk.gov.hmrc.estates.models._
import uk.gov.hmrc.estates.models.getEstate.{GetEstateProcessedResponse, GetEstateResponse}
import uk.gov.hmrc.estates.models.variation.VariationResponse
import uk.gov.hmrc.estates.repositories.CacheRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DesService @Inject()(val desConnector: DesConnector, repository: CacheRepository) {

  def getEstateInfoFormBundleNo(utr: String)(implicit hc: HeaderCarrier): Future[String] =
    desConnector.getEstateInfo(utr).map {
      case response: GetEstateProcessedResponse =>
        response.responseHeader.formBundleNo
      case response =>
        val msg = s"[DesService] utr $utr: Failed to retrieve latest form bundle no from ETMP due to $response"
        Logger.warn(msg)
        throw InternalServerErrorException(s"Submission could not proceed, $msg")
    }

  def checkExistingEstate(existingEstateCheckRequest: ExistingCheckRequest): Future[ExistingCheckResponse] = {
    desConnector.checkExistingEstate(existingEstateCheckRequest)
  }

  def registerEstate(estateRegistration: EstateRegistration): Future[RegistrationResponse] = {
    desConnector.registerEstate(estateRegistration)
  }

  def getSubscriptionId(trn: String): Future[SubscriptionIdResponse] = {
    desConnector.getSubscriptionId(trn)
  }

  def refreshCacheAndGetEstateInfo(utr: String, internalId: String)(implicit hc: HeaderCarrier): Future[GetEstateResponse] = {

    Logger.info(s"[DesService][refreshCacheAndGetEstateInfo] refreshing cache for $utr")

    repository.resetCache(utr, internalId).flatMap { _ =>
      desConnector.getEstateInfo(utr).flatMap {
        case response: GetEstateProcessedResponse =>
          Logger.info(s"[DesService] setting cached record for $utr")
          repository.set(utr, internalId, Json.toJson(response)(GetEstateProcessedResponse.mongoWrites)).map{ _ =>
            response
          }
        case otherResponse =>
          Logger.info(s"[DesService] document returned for $utr was not in processed state")
          Future.successful(otherResponse)
      }
    }
  }

  def getEstateInfo(utr: String, internalId: String)(implicit hc: HeaderCarrier): Future[GetEstateResponse] = {
    Logger.info(s"getting cached record for utr $utr")
    repository.get(utr, internalId).flatMap {
      case Some(x) =>
        x.validate[GetEstateResponse].fold(
          errs => {
            Logger.error(s"[DesService] unable to parse json from cache for $utr as GetEstateResponse $errs")
            Future.failed[GetEstateResponse](new Exception(errs.toString))
        },
        response => {
          Logger.info(s"[DesService] found cached record for $utr")
          Future.successful(response)
        }
      )
      case None => refreshCacheAndGetEstateInfo(utr, internalId)
    }
  }

  def estateVariation(estateVariation: JsValue)(implicit hc: HeaderCarrier): Future[VariationResponse] =
    desConnector.estateVariation(estateVariation: JsValue)
}


