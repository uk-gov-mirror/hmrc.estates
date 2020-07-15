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
import uk.gov.hmrc.estates.connectors.DesConnector
import uk.gov.hmrc.estates.models.getEstate.GetEstateResponse
import uk.gov.hmrc.estates.models.variation.{EstateVariation, VariationResponse}
import uk.gov.hmrc.estates.models._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class DesService @Inject()(val desConnector: DesConnector) {

  def checkExistingEstate(existingEstateCheckRequest: ExistingCheckRequest): Future[ExistingCheckResponse] = {
    desConnector.checkExistingEstate(existingEstateCheckRequest)
  }

  def registerEstate(estateRegistration: EstateRegistration): Future[RegistrationResponse] = {
    desConnector.registerEstate(estateRegistration)
  }

  def getSubscriptionId(trn: String): Future[SubscriptionIdResponse] = {
    desConnector.getSubscriptionId(trn)
  }

  def getEstateInfo(utr: String, internalId: String)(implicit hc: HeaderCarrier): Future[GetEstateResponse] = {
    desConnector.getEstateInfo(utr)
  }

  def estateVariation(estateVariation: EstateVariation): Future[VariationResponse] =
    desConnector.estateVariation(estateVariation: EstateVariation)
}


