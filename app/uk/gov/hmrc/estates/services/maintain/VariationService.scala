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

package uk.gov.hmrc.estates.services.maintain

import javax.inject.Inject
import play.api.Logger
import play.api.libs.json._
import uk.gov.hmrc.estates.exceptions.InternalServerErrorException
import uk.gov.hmrc.estates.models.DeclarationForApi
import uk.gov.hmrc.estates.models.getEstate.{EtmpCacheDataStaleResponse, GetEstateProcessedResponse, GetEstateResponse}
import uk.gov.hmrc.estates.models.variation.{VariationFailureResponse, VariationResponse, VariationSuccessResponse}
import uk.gov.hmrc.estates.services.{AuditService, DesService, LocalDateService, VariationsTransformationService}
import uk.gov.hmrc.estates.utils.JsonOps._
import uk.gov.hmrc.estates.utils.VariationErrorResponses.EtmpDataStaleErrorResponse
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class VariationService @Inject()(
                                  desService: DesService,
                                  transformationService: VariationsTransformationService,
                                  declarationService: VariationDeclarationService,
                                  auditService: AuditService,
                                  localDateService: LocalDateService) {

  def submitDeclaration(utr: String,
                        internalId: String,
                        declaration: DeclarationForApi)
                       (implicit hc: HeaderCarrier): Future[VariationResponse] = {

    getCachedEstateData(utr, internalId).flatMap {
      case cached: GetEstateProcessedResponse =>

        val cachedEstate = cached.getEstate
        val responseHeader = cached.responseHeader

        transformationService.populatePersonalRepAddress(cachedEstate) match {
          case JsSuccess(cachedWithAmendedPerRepAddress, _) =>
            transformationService.applyDeclarationTransformations(utr, internalId, cachedWithAmendedPerRepAddress).flatMap {
              case JsSuccess(transformedDocument, _) =>
                val transformedWithHeader = GetEstateProcessedResponse(transformedDocument, responseHeader)
                declarationService.transform(transformedWithHeader, cachedWithAmendedPerRepAddress, declaration, localDateService.now) match {
                  case JsSuccess(value, _) =>
                    Logger.debug(s"[VariationDeclarationService] utr $utr submitting variation $value")
                    Logger.info(s"[VariationDeclarationService] utr $utr successfully transformed json for declaration")
                    doSubmit(value, internalId, declaration.agentDetails.isDefined)
                  case e: JsError =>
                    Logger.error(s"[VariationDeclarationService] utr $utr: Problem transforming data for ETMP submission ${JsError.toJson(e)}")
                    Future.failed(InternalServerErrorException("There was a problem transforming data for submission to ETMP"))
                }
              case e: JsError =>
                Logger.error(s"[VariationDeclarationService] utr $utr: Failed to transform estate info ${JsError.toJson(e)}")
                Future.failed(InternalServerErrorException("There was a problem transforming data for submission to ETMP"))
            }
          case e: JsError =>
            Logger.error(s"[VariationDeclarationService] utr $utr: Failed to populate personal rep address ${JsError.toJson(e)}")
            Future.failed(InternalServerErrorException("There was a problem transforming data for submission to ETMP"))
        }
      case EtmpCacheDataStaleResponse => Future.successful(VariationFailureResponse(EtmpDataStaleErrorResponse))
   }
  }

  private def getCachedEstateData(utr: String, internalId: String)(implicit hc: HeaderCarrier): Future[GetEstateResponse] = {
    for {
      response <- desService.getEstateInfo(utr, internalId)
      fbn <- desService.getEstateInfoFormBundleNo(utr)
    } yield response match {
      case tpr: GetEstateProcessedResponse if tpr.responseHeader.formBundleNo == fbn =>
        Logger.info(s"[VariationDeclarationService][submitDeclaration] utr $utr: returning GetEstateProcessedResponse")
        response.asInstanceOf[GetEstateProcessedResponse]
      case _: GetEstateProcessedResponse =>
        Logger.info(s"[VariationDeclarationService][submitDeclaration] utr $utr: ETMP cached data in mongo has become stale, rejecting submission")
        EtmpCacheDataStaleResponse
      case _ =>
        Logger.warn(s"[VariationDeclarationService][submitDeclaration] utr $utr: Estate was not in a processed state")
        throw InternalServerErrorException("Submission could not proceed, Estate data was not in a processed state")
    }
  }

  private def doSubmit(value: JsValue, internalId: String, isAgent: Boolean)
                      (implicit hc: HeaderCarrier): Future[VariationResponse] = {

    val payload = value.applyRules

    desService.estateVariation(payload) map {
      case response: VariationSuccessResponse =>

        Logger.info(s"[VariationService][doSubmit] variation submitted")

        auditService.auditVariationSubmitted(isAgent, internalId, payload, response)

        response

      case response: VariationFailureResponse =>
        Logger.error(s"[VariationService][doSubmit] variation failed: ${response.response}")

        auditService.auditVariationFailed(internalId, payload, response)

        response

      case response => response
    }
  }
}
