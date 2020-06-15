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

import javax.inject.Inject
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.estates.config.AppConfig
import uk.gov.hmrc.estates.controllers.actions.IdentifierAction
import uk.gov.hmrc.estates.exceptions._
import uk.gov.hmrc.estates.models.ApiResponse._
import uk.gov.hmrc.estates.models._
import uk.gov.hmrc.estates.models.auditing.Auditing
import uk.gov.hmrc.estates.services.{AuditService, DesService, RosmPatternService, ValidationService}
import uk.gov.hmrc.estates.utils.ErrorResponses._
import uk.gov.hmrc.estates.utils.JsonOps._
import uk.gov.hmrc.estates.models.JsonWithoutNulls._
import uk.gov.hmrc.http.BadRequestException

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class RegisterEstateController @Inject()(desService: DesService, config: AppConfig,
                                         validationService: ValidationService,
                                         identifierAction: IdentifierAction,
                                         rosmPatternService: RosmPatternService,
                                         auditService: AuditService)
                                        (implicit ec: ExecutionContext, cc: ControllerComponents) extends EstatesBaseController(cc) {

  def registration(): Action[JsValue] = identifierAction.async(parse.json) {
    implicit request =>

      val payload = request.body.applyRules.toString

      validationService
        .get(config.estatesApiRegistrationSchema)
        .validate[EstateRegistration](payload) match {

        case Right(estatesRegistrationRequest) =>
          desService.registerEstate(estatesRegistrationRequest).flatMap {
            case response: RegistrationTrnResponse =>

              auditService.audit(
                event = Auditing.ESTATE_REGISTRATION_SUBMITTED,
                registration = estatesRegistrationRequest,
                internalId = request.identifier,
                response = response
              )

              Logger.info("[RegisterEstateController] Estate registration completed successfully.")
              rosmPatternService.enrolAndLogResult(response.trn, request.affinityGroup) map {
                _ =>
                  Ok(Json.toJson(response))
              }
          } recover {
            case AlreadyRegisteredException =>

              auditService.audit(
                event = Auditing.ESTATE_REGISTRATION_SUBMITTED,
                registration = estatesRegistrationRequest,
                internalId = request.identifier,
                response = RegistrationFailureResponse(403, "ALREADY_REGISTERED", "Estate is already registered.")
              )

              Logger.info("[RegisterEstateController][registration] Returning already registered response.")
              Conflict(Json.toJson(alreadyRegisteredEstateResponse))
            case NoMatchException =>

              auditService.audit(
                event = Auditing.ESTATE_REGISTRATION_SUBMITTED,
                registration = estatesRegistrationRequest,
                internalId = request.identifier,
                response = RegistrationFailureResponse(403, "NO_MATCH", "There is no match in HMRC records.")
              )

              Logger.info("[RegisterEstateController][registration] Returning no match response.")
              Forbidden(Json.toJson(noMatchRegistrationResponse))
            case x: ServiceNotAvailableException =>

              auditService.audit(
                event = Auditing.ESTATE_REGISTRATION_SUBMITTED,
                registration = estatesRegistrationRequest,
                internalId = request.identifier,
                response = RegistrationFailureResponse(503, "SERVICE_UNAVAILABLE", "Dependent systems are currently not responding.")
              )

              Logger.error(s"[RegisterEstateController][registration] Service unavailable response from DES")
              InternalServerError(Json.toJson(internalServerErrorResponse))
            case x: BadRequestException =>

              auditService.audit(
                event = Auditing.ESTATE_REGISTRATION_SUBMITTED,
                registration = estatesRegistrationRequest,
                internalId = request.identifier,
                response = RegistrationFailureResponse(400, "INVALID_PAYLOAD", "Submission has not passed validation. Invalid payload..")
              )

              Logger.error(s"[RegisterEstateController][registration] bad request response from DES")
              InternalServerError(Json.toJson(internalServerErrorResponse))
            case NonFatal(e) =>
              Logger.error(s"[RegisterEstateController][registration] Exception received : $e.")
              InternalServerError(Json.toJson(internalServerErrorResponse))
          }
        case Left(_) =>
          Logger.error(s"[registration] estates validation errors, returning bad request.")
          Future.successful(invalidRequestErrorResponse)
      }

  }

}
