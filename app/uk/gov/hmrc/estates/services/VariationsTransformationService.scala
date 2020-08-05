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
import uk.gov.hmrc.estates.models.auditing.Auditing
import uk.gov.hmrc.estates.models.getEstate.{GetEstateProcessedResponse, GetEstateResponse, TransformationErrorResponse}
import uk.gov.hmrc.estates.repositories.VariationsTransformationRepository
import uk.gov.hmrc.estates.transformers.{ComposedDeltaTransform, DeltaTransform}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class VariationsTransformationService @Inject()(transformRepository: VariationsTransformationRepository,
                                                desService: DesService,
                                                auditService: AuditService){

  def addNewTransform(utr: String, internalId: String, newTransform: DeltaTransform) : Future[Boolean] = {
    transformRepository.get(utr, internalId) map {
      case None =>
        ComposedDeltaTransform(Seq(newTransform))

      case Some(composedTransform) =>
        composedTransform :+ newTransform

    } flatMap { newTransforms =>
      transformRepository.set(utr, internalId, newTransforms)
    } recoverWith {
      case e =>
        Logger.error(s"[TransformationService] utr $utr exception adding new transform: ${e.getMessage}")
        Future.failed(e)
    }
  }

  def getTransformations(utr: String, internalId: String): Future[Option[ComposedDeltaTransform]] =
    transformRepository.get(utr, internalId)

  def removeAllTransformations(utr: String, internalId: String): Future[Option[JsObject]] =
    transformRepository.resetCache(utr, internalId)

  def getTransformedData(utr: String, internalId: String)(implicit hc: HeaderCarrier): Future[GetEstateResponse] = {
    desService.getEstateInfo(utr, internalId).flatMap {
      case response: GetEstateProcessedResponse =>
        populatePersonalRepAddress(response.getEstate) match {
          case JsSuccess(fixed, _) =>
            applyTransformations(utr, internalId, fixed).map {
              case JsSuccess(transformed, _) => GetEstateProcessedResponse(transformed, response.responseHeader)
              case JsError(errors) => TransformationErrorResponse(errors.toString)
          }
          case JsError(errors) => Future.successful(TransformationErrorResponse(errors.toString))
        }
      case response => Future.successful(response)
    }
  }

  private def applyTransformations(utr: String, internalId: String, json: JsValue)(implicit hc : HeaderCarrier): Future[JsResult[JsValue]] = {
    transformRepository.get(utr, internalId).map {
      case None =>
        JsSuccess(json)
      case Some(transformations) =>
        transformations.applyTransform(json)
    }
  }

  def applyDeclarationTransformations(utr: String, internalId: String, json: JsValue)
                                     (implicit hc : HeaderCarrier): Future[JsResult[JsValue]] = {

    transformRepository.get(utr, internalId).map {
      case None =>
        Logger.info(s"[VariationsTransformationService] utr $utr no transformations to apply")
        JsSuccess(json)
      case Some(transformations) =>

        auditService.audit(
          event = Auditing.ESTATE_TRANSFORMATIONS,
          request = Json.toJson(Json.obj()),
          internalId = internalId,
          response = Json.obj(
            "transformations" -> transformations,
            "data" -> json
          )
        )

        Logger.debug(s"[VariationsTransformationService] utr $utr applying the following transforms $transformations")

        for {
          initial <- {
            Logger.info(s"[VariationsTransformationService] utr $utr applying transformations")
            transformations.applyTransform(json)
          }
          transformed <- {
            Logger.info(s"[VariationsTransformationService] utr $utr applying declaration transformations")
            transformations.applyDeclarationTransform(initial)
          }
        } yield transformed
    }
  }

  def populatePersonalRepAddress(beforeJson: JsValue): JsResult[JsValue] = {
    val pathToPersonalRepAddress = __ \ 'details \ 'estate \ 'entities \ 'personalRepresentative \ 'identification \ 'address

    if (beforeJson.transform(pathToPersonalRepAddress.json.pick).isSuccess) {
      Logger.info(s"[VariationTransformationService] record already has an address for the personal representative, not modifying")
      JsSuccess(beforeJson)
    } else {
      Logger.info(s"[VariationTransformationService] record does not have an address for personal rep, adding one from correspondence")
      val pathToCorrespondenceAddress = __ \ 'correspondence \ 'address
      val copyAddress = __.json.update(pathToPersonalRepAddress.json.copyFrom(pathToCorrespondenceAddress.json.pick))
      beforeJson.transform(copyAddress)
    }
  }
}
