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
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.estates.controllers.actions.{IdentifierAction, ValidateUTRActionFactory}
import uk.gov.hmrc.estates.models.auditing.Auditing
import uk.gov.hmrc.estates.models.getEstate._
import uk.gov.hmrc.estates.models.requests.IdentifierRequest
import uk.gov.hmrc.estates.services.{AuditService, DesService}
import uk.gov.hmrc.play.bootstrap.controller.BackendController

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class GetEstateController @Inject()(identify: IdentifierAction,
                                    auditService: AuditService,
                                    desService: DesService,
                                    validateUTRActionFactory: ValidateUTRActionFactory
                                   )(implicit cc: ControllerComponents) extends BackendController(cc) {

  def get(utr: String): Action[AnyContent] = (validateUTRActionFactory.create(utr) andThen identify).async {
    implicit request =>

    desService.getEstateInfo(utr) map {
      case processed : GetEstateProcessedResponse =>
        processedResponse(utr, processed)
      case status: GetEstateStatusResponse =>
        statusResponse(utr, status)
      case NotEnoughDataResponse(json, errors) =>
        notEnoughInformationResponse(utr, json, errors)
      case ResourceNotFoundResponse =>
        auditService.auditErrorResponse(Auditing.GET_ESTATE, Json.obj("utr" -> utr), request.identifier, "Not Found received from DES.")
        NotFound
      case errorResponse: GetEstateErrorResponse =>
        auditService.auditErrorResponse(Auditing.GET_ESTATE, Json.obj("utr" -> utr), request.identifier, errorResponse.toString)
        InternalServerError
      case _ =>
        auditService.auditErrorResponse(Auditing.GET_ESTATE, Json.obj("utr" -> utr), request.identifier, "UNKNOWN")
        InternalServerError
    }
  }

  private def notEnoughInformationResponse(utr: String, json: JsValue, errors: JsValue)(implicit request: IdentifierRequest[AnyContent]) = {
    val reason = Json.obj(
      "response" -> json,
      "reason" -> "Missing mandatory fields in response received from DES",
      "errors" -> errors
    )

    auditService.audit(
      event = Auditing.GET_ESTATE,
      request = Json.obj("utr" -> utr),
      internalId = request.identifier,
      response = reason
    )

    InternalServerError(errors)
  }

  private def statusResponse(utr: String, status: GetEstateStatusResponse)(implicit request: IdentifierRequest[AnyContent]) = {
    val response = Json.toJson(status)

    auditService.audit(
      event = Auditing.GET_ESTATE,
      request = Json.obj("utr" -> utr),
      internalId = request.identifier,
      response = response
    )

    Ok(response)
  }

  private def processedResponse(utr: String, processed: GetEstateProcessedResponse)(implicit request: IdentifierRequest[AnyContent]) = {
    val response = Json.toJson(processed)

    auditService.audit(
      event = Auditing.GET_ESTATE,
      request = Json.obj("utr" -> utr),
      internalId = request.identifier,
      response = response
    )

    Ok(response)
  }
}
