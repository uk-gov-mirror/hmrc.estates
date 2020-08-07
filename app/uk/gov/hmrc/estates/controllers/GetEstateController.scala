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

package uk.gov.hmrc.estates.controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsPath, JsString, JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import uk.gov.hmrc.estates.controllers.actions.{IdentifierAction, ValidateUTRActionFactory}
import uk.gov.hmrc.estates.models.getEstate._
import uk.gov.hmrc.estates.models.requests.IdentifierRequest
import uk.gov.hmrc.estates.services.{AuditService, DesService, VariationsTransformationService}
import uk.gov.hmrc.play.bootstrap.controller.BackendController

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class GetEstateController @Inject()(identify: IdentifierAction,
                                    auditService: AuditService,
                                    desService: DesService,
                                    variationsTransformationService: VariationsTransformationService,
                                    validateUTRActionFactory: ValidateUTRActionFactory
                                   )(implicit cc: ControllerComponents) extends BackendController(cc) {

  def get(utr: String, applyTransforms: Boolean): Action[AnyContent] =
    doGet(utr, applyTransforms) {
      result: GetEstateResponse => Ok(Json.toJson(result))
    }

  def getPersonalRepresentative(utr: String): Action[AnyContent] =
    doGet(utr, applyTransforms = true) {
      case processed: GetEstateProcessedResponse =>
        val pick = (JsPath \ 'details \ 'estate \ 'entities \ 'personalRepresentative).json.pick
        processed.getEstate.transform(pick).fold(
          _ => InternalServerError,
          json => {
            Ok(json.as[PersonalRepresentativeType] match {
              case PersonalRepresentativeType(Some(personalRepInd), None) => Json.toJson(personalRepInd)
              case PersonalRepresentativeType(None, Some(personalRepOrg)) => Json.toJson(personalRepOrg)
            })
          }
        )
      case _ => Forbidden
    }

  def getDateOfDeath(utr: String): Action[AnyContent] =
    getItemAtPath(utr, JsPath \ 'details \'estate \ 'entities \ 'deceased \ 'dateOfDeath)

  private def notEnoughInformationResponse(utr: String, json: JsValue, errors: JsValue)
                                          (implicit request: IdentifierRequest[AnyContent]): Result = {
    val reason = Json.obj(
      "response" -> json,
      "reason" -> "Missing mandatory fields in response received from DES",
      "errors" -> errors
    )

    auditService.auditGetVariationFailed(utr, reason)

    InternalServerError(errors)
  }

  private def statusResponse(utr: String, status: GetEstateStatusResponse)
                            (implicit request: IdentifierRequest[AnyContent]): Unit = {
    auditService.auditGetVariationFailed(utr, Json.obj("status" -> Json.toJson(status)))
  }

  private def processedResponse(utr: String, processed: GetEstateProcessedResponse)
                               (implicit request: IdentifierRequest[AnyContent]): Unit = {
    auditService.auditGetVariationSuccess(utr, processed)
  }

  private def getItemAtPath(utr: String, path: JsPath): Action[AnyContent] = {
    getElementAtPath(utr,
      path,
      Json.obj()) {
      json => json
    }
  }

  private def getElementAtPath(utr: String, path: JsPath, defaultValue: JsValue)
                              (insertIntoObject: JsValue => JsValue): Action[AnyContent] = {
    getEtmpData(utr) {
      data => data
        .transform(path.json.pick)
        .map(insertIntoObject)
        .getOrElse(defaultValue)
    }
  }

  private def getEtmpData(utr: String)
                         (processObject: JsValue => JsValue): Action[AnyContent] = {
    doGet(utr, applyTransforms = false) {
      case processed: GetEstateProcessedResponse =>
        Ok(processObject(processed.getEstate))
      case _ =>
        InternalServerError
    }
  }

  private def doGet(utr: String, applyTransforms: Boolean)
                   (handleResult: GetEstateResponse => Result): Action[AnyContent] = {

    (validateUTRActionFactory.create(utr) andThen identify).async {
      implicit request =>
        val data = if (applyTransforms) {
          variationsTransformationService.getTransformedData(utr, request.identifier)
        } else {
          desService.getEstateInfo(utr, request.identifier)
        }

        data map {
          case processed: GetEstateProcessedResponse =>
            processedResponse(utr, processed)
            handleResult(processed)
          case status: GetEstateStatusResponse =>
            statusResponse(utr, status)
            handleResult(status)
          case NotEnoughDataResponse(json, errors) =>
            notEnoughInformationResponse(utr, json, errors)
          case ResourceNotFoundResponse =>
            auditService.auditGetVariationFailed(utr, JsString(ResourceNotFoundResponse.toString))
            NotFound
          case errorResponse: GetEstateErrorResponse =>
            auditService.auditGetVariationFailed(utr, JsString(errorResponse.toString))
            InternalServerError
          case _ =>
            auditService.auditGetVariationFailed(utr, JsString("UNKNOWN"))
            InternalServerError
        }
    }
  }
}
