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
import uk.gov.hmrc.estates.models.getEstate.{EtmpCacheDataStaleResponse, GetEstateProcessedResponse, GetEstateResponse, ResponseHeader}
import uk.gov.hmrc.estates.models.variation.{VariationFailureResponse, VariationResponse, VariationSuccessResponse}
import uk.gov.hmrc.estates.services.{AuditService, DesService, VariationsTransformationService}
import uk.gov.hmrc.estates.utils.ErrorResponses.{EtmpDataStaleErrorResponse, InternalServerErrorErrorResponse}
import uk.gov.hmrc.estates.utils.JsonOps._
import uk.gov.hmrc.estates.utils.Session
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class VariationService @Inject()(
                                  desService: DesService,
                                  transformationService: VariationsTransformationService,
                                  declarationService: VariationDeclarationService,
                                  auditService: AuditService) {

  private val logger: Logger = Logger(getClass)

  def submitDeclaration(utr: String,
                        internalId: String,
                        declaration: DeclarationForApi)
                       (implicit hc: HeaderCarrier): Future[VariationResponse] = {

    getCachedEstateData(utr, internalId).flatMap {
      case cached: GetEstateProcessedResponse =>

        val cachedEstate = cached.getEstate
        val responseHeader: ResponseHeader = cached.responseHeader

        transformationService.populatePersonalRepAddress(cachedEstate) match {
          case JsSuccess(cachedWithAmendedPerRepAddress, _) =>
            processPopulatedEstate(utr, internalId, cachedWithAmendedPerRepAddress, declaration, responseHeader)
          case e: JsError =>
            auditService.auditVariationTransformationError(
              utr,
              internalId,
              cached.getEstate,
              JsString("Copy address transform"),
              "Failed to populate personal rep address",
              JsError.toJson(e)
            )
            logger.error(s"[submitDeclaration][Session ID: ${Session.id(hc)}][UTR: $utr]" +
              s" Failed to populate personal rep address ${JsError.toJson(e)}")
            Future.failed(InternalServerErrorException("There was a problem transforming data for submission to ETMP"))
        }
      case EtmpCacheDataStaleResponse => Future.successful(VariationFailureResponse(EtmpDataStaleErrorResponse))
        // TODO: Do we need to be more specific?
      case _ => Future.successful(VariationFailureResponse(InternalServerErrorErrorResponse))
    }
  }

  private def processPopulatedEstate(utr: String,
                                     internalId: String,
                                     cachedWithAmendedPerRepAddress: JsValue,
                                     declaration: DeclarationForApi,
                                     responseHeader: ResponseHeader)
                                    (implicit hc: HeaderCarrier): Future[VariationResponse] = {
    transformationService.applyDeclarationTransformations(utr, internalId, cachedWithAmendedPerRepAddress).flatMap {
      case JsSuccess(transformedDocument, _) =>
        declarationService.transform(
          transformedDocument,
          responseHeader,
          cachedWithAmendedPerRepAddress,
          declaration
        ) match {
          case JsSuccess(value, _) =>
            logger.debug(s"[processPopulatedEstate][Session ID: ${Session.id(hc)}][UTR: $utr]" +
              s" submitting variation $value")
            logger.info(s"[processPopulatedEstate][Session ID: ${Session.id(hc)}][UTR: $utr]" +
              s" successfully transformed json for declaration")
            doSubmit(value, internalId)
          case e: JsError =>
            auditService.auditVariationTransformationError(
              utr,
              internalId,
              transformedDocument,
              transforms = JsString("Declaration transforms"),
              "Problem transforming data for ETMP submission",
              JsError.toJson(e)
            )
            logger.error(s"[processPopulatedEstate][Session ID: ${Session.id(hc)}][UTR: $utr]" +
              s" Problem transforming data for ETMP submission ${JsError.toJson(e)}")
            Future.failed(InternalServerErrorException("There was a problem transforming data for submission to ETMP"))
        }
      case e: JsError =>
        logger.error(s"[processPopulatedEstate][Session ID: ${Session.id(hc)}][UTR: $utr]" +
          s" Failed to transform estate info ${JsError.toJson(e)}")
        Future.failed(InternalServerErrorException("There was a problem transforming data for submission to ETMP"))
    }
  }

  private def getCachedEstateData(utr: String, internalId: String)(implicit hc: HeaderCarrier): Future[GetEstateResponse] = {
    for {
      response <- desService.getEstateInfo(utr, internalId)
      fbn <- desService.getEstateInfoFormBundleNo(utr)
    } yield response match {
      case tpr: GetEstateProcessedResponse if tpr.responseHeader.formBundleNo == fbn =>
        logger.info(s"[getCachedEstateData][Session ID: ${Session.id(hc)}][UTR: $utr]" +
          s" returning GetEstateProcessedResponse")
        response.asInstanceOf[GetEstateProcessedResponse]
      case _: GetEstateProcessedResponse =>
        logger.info(s"[getCachedEstateData][Session ID: ${Session.id(hc)}][UTR: $utr]" +
          s" ETMP cached data in mongo has become stale, rejecting submission")
        EtmpCacheDataStaleResponse
      case _ =>
        logger.warn(s"[getCachedEstateData][Session ID: ${Session.id(hc)}][UTR: $utr] Estate was not in a processed state")
        throw InternalServerErrorException("Submission could not proceed, Estate data was not in a processed state")
    }
  }

  private def doSubmit(value: JsValue, internalId: String)
                      (implicit hc: HeaderCarrier): Future[VariationResponse] = {

    val payload = value.applyRules

    desService.estateVariation(payload) map {
      case response: VariationSuccessResponse =>

        logger.info(s"[doSubmit][Session ID: ${Session.id(hc)}] variation submitted")

        auditService.auditVariationSubmitted(internalId, payload, response)

        response

      case response: VariationFailureResponse =>
        logger.error(s"[doSubmit][Session ID: ${Session.id(hc)}] variation failed: ${response.response}")

        auditService.auditVariationFailed(internalId, payload, response)

        response

      case response => response
    }
  }
}
