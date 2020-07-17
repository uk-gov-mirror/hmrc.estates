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
import uk.gov.hmrc.estates.exceptions.{EtmpCacheDataStaleException, InternalServerErrorException}
import uk.gov.hmrc.estates.models.DeclarationForApi
import uk.gov.hmrc.estates.models.auditing.Auditing
import uk.gov.hmrc.estates.models.getEstate.GetEstateProcessedResponse
import uk.gov.hmrc.estates.models.variation.VariationResponse
import uk.gov.hmrc.estates.transformers.register.VariationDeclarationTransform
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.estates.utils.JsonOps._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class VariationDeclarationService @Inject()(
                                  desService: DesService,
                                  transformationService: VariationsTransformationService,
                                  declarationTransformer: VariationDeclarationTransform,
                                  auditService: AuditService,
                                  localDateService: LocalDateService) {

  def submitDeclaration(utr: String, internalId: String, declaration: DeclarationForApi)
                       (implicit hc: HeaderCarrier): Future[VariationResponse] = {

    getCachedEstateData(utr, internalId).flatMap { originalResponse: GetEstateProcessedResponse =>
      transformationService.populatePersonalRepAddress(originalResponse.getEstate) match {
        case JsSuccess(originalJson, _) =>
          transformationService.applyDeclarationTransformations(utr, internalId, originalJson).flatMap {
            case JsSuccess(transformedJson, _) =>
              val response = GetEstateProcessedResponse(transformedJson, originalResponse.responseHeader)
              declarationTransformer.transform(response, originalJson, declaration, localDateService.now) match {
                case JsSuccess(value, _) =>
                  Logger.info(s"[VariationDeclarationService] successfully transformed json for declaration")
                  doSubmit(value, internalId)
                case JsError(errors) =>
                  Logger.error("Problem transforming data for ETMP submission " + errors.toString())
                  Future.failed(InternalServerErrorException("There was a problem transforming data for submission to ETMP"))
              }
            case JsError(errors) =>
              Logger.error(s"Failed to transform estate info $errors")
              Future.failed(InternalServerErrorException("There was a problem transforming data for submission to ETMP"))
          }
        case JsError(errors) =>
          Logger.error(s"Failed to populate personal rep address $errors")
          Future.failed(InternalServerErrorException("There was a problem transforming data for submission to ETMP"))
      }
   }
  }

  private def getCachedEstateData(utr: String, internalId: String)(implicit hc: HeaderCarrier) = {
    for {
      response <- desService.getEstateInfo(utr, internalId)
      fbn <- desService.getEstateInfoFormBundleNo(utr)
    } yield response match {
      case tpr: GetEstateProcessedResponse if tpr.responseHeader.formBundleNo == fbn =>
        Logger.info(s"[VariationDeclarationService][submitDeclaration] returning GetEstateProcessedResponse")
        response.asInstanceOf[GetEstateProcessedResponse]
      case _: GetEstateProcessedResponse =>
        Logger.info(s"[VariationDeclarationService][submitDeclaration] ETMP cached data in mongo has become stale, rejecting submission")
        throw EtmpCacheDataStaleException
      case _ =>
        Logger.warn(s"[VariationDeclarationService][submitDeclaration] Estate was not in a processed state")
        throw InternalServerErrorException("Submission could not proceed, Estate data was not in a processed state")
    }
  }

  private def doSubmit(value: JsValue, internalId: String)(implicit hc: HeaderCarrier): Future[VariationResponse] = {

    val payload = value.applyRules

    auditService.audit(
      Auditing.ESTATE_VARIATION_ATTEMPT,
      payload,
      internalId,
      Json.toJson(Json.obj())
    )

    desService.estateVariation(payload) map { response =>

      Logger.info(s"[VariationService][doSubmit] variation submitted")

      auditService.audit(
        Auditing.ESTATE_VARIATION,
        payload,
        internalId,
        Json.toJson(response)
      )

      response
    }
  }
}
