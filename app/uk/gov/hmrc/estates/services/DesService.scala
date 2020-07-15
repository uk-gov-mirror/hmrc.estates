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
import play.api.libs.json.Json
import uk.gov.hmrc.estates.connectors.DesConnector
import uk.gov.hmrc.estates.models.getEstate.{GetEstateProcessedResponse, GetEstateResponse}
import uk.gov.hmrc.estates.models.variation.{EstateVariation, VariationResponse}
import uk.gov.hmrc.estates.models._
import uk.gov.hmrc.estates.repositories.CacheRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DesService @Inject()(val desConnector: DesConnector, repository: CacheRepository) {

  def checkExistingEstate(existingEstateCheckRequest: ExistingCheckRequest): Future[ExistingCheckResponse] = {
    desConnector.checkExistingEstate(existingEstateCheckRequest)
  }

  def registerEstate(estateRegistration: EstateRegistration): Future[RegistrationResponse] = {
    desConnector.registerEstate(estateRegistration)
  }

  def getSubscriptionId(trn: String): Future[SubscriptionIdResponse] = {
    desConnector.getSubscriptionId(trn)
  }

  def refreshCacheAndGetTrustInfo(utr: String, internalId: String)(implicit hc: HeaderCarrier): Future[GetEstateResponse] = {
    Logger.debug("Retrieving Estate Info from DES")
    Logger.info(s"[DesService][refreshCacheAndGetTrustInfo] refreshing cache")

    repository.resetCache(utr, internalId).flatMap { _ =>
      desConnector.getEstateInfo(utr).map {
        case response: GetEstateProcessedResponse =>
          repository.set(utr, internalId, Json.toJson(response)(GetEstateProcessedResponse.mongoWrites))
          response
        case x => x
      }
    }
  }
  def getEstateInfo(utr: String, internalId: String)(implicit hc: HeaderCarrier): Future[GetEstateResponse] = {
    Logger.debug("Getting estate Info")
    repository.get(utr, internalId).flatMap {
      case Some(x) => x.validate[GetEstateResponse].fold(
        errs => {
          Logger.error(s"[DesService] unable to parse json from cache as GetEstateResponse $errs")
          Future.failed[GetEstateResponse](new Exception(errs.toString))
        },
        response => {
          Future.successful(response)
        }
      )
      case None => refreshCacheAndGetTrustInfo(utr, internalId)
    }
  }

  def estateVariation(estateVariation: EstateVariation): Future[VariationResponse] =
    desConnector.estateVariation(estateVariation: EstateVariation)
}


