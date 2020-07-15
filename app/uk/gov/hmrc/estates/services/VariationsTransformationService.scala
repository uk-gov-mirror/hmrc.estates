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
import play.api.libs.json._
import uk.gov.hmrc.estates.models.getEstate.{GetEstateProcessedResponse, GetEstateResponse, TransformationErrorResponse}
import uk.gov.hmrc.estates.repositories.{TransformationRepository, VariationsTransformationRepository}
import uk.gov.hmrc.estates.transformers.register.YearsReturnsTransform
import uk.gov.hmrc.estates.transformers.{ComposedDeltaTransform, DeltaTransform}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class VariationsTransformationService @Inject()(repository: VariationsTransformationRepository,
                                                desService: DesService,
                                                auditService: AuditService){

  def addNewTransform(utr: String, internalId: String, newTransform: DeltaTransform) : Future[Boolean] = {
    repository.get(utr, internalId) map {
      case None =>
        ComposedDeltaTransform(Seq(newTransform))

      case Some(composedTransform) =>
        composedTransform :+ newTransform

    } flatMap { newTransforms =>
      repository.set(utr, internalId, newTransforms)
    } recoverWith {
      case e =>
        Logger.error(s"[TransformationService] exception adding new transform: ${e.getMessage}")
        Future.failed(e)
    }
  }

  def getTransformations(utr: String, internalId: String): Future[Option[ComposedDeltaTransform]] =
    repository.get(utr, internalId)

  def removeAllTransformations(utr: String, internalId: String): Future[Option[JsObject]] =
    repository.resetCache(utr, internalId)

  def getTransformedData(utr: String, internalId: String)(implicit hc: HeaderCarrier): Future[GetEstateResponse] = {
    desService.getEstateInfo(utr, internalId).flatMap {
      case response: GetEstateProcessedResponse =>
        applyTransformations(utr, internalId, response.getEstate).map {
          case JsSuccess(transformed, _) =>
            GetEstateProcessedResponse(transformed, response.responseHeader)
          case JsError(errors) => TransformationErrorResponse(errors.toString)
        }
      case response => Future.successful(response)
    }
  }

  private def applyTransformations(utr: String, internalId: String, json: JsValue)(implicit hc : HeaderCarrier): Future[JsResult[JsValue]] = {
    repository.get(utr, internalId).map {
      case None =>
        JsSuccess(json)
      case Some(transformations) =>
        transformations.applyTransform(json)
    }
  }
}
