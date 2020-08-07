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
import play.api.libs.json.{JsPath, JsString, JsValue, Json}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.estates.config.AppConfig
import uk.gov.hmrc.estates.models.{EstateRegistration, EstateRegistrationNoDeclaration}
import uk.gov.hmrc.estates.models.auditing.EstatesAuditData
import uk.gov.hmrc.estates.models.getEstate.GetEstateProcessedResponse
import uk.gov.hmrc.estates.models.requests.IdentifierRequest
import uk.gov.hmrc.estates.models.variation.{VariationFailureResponse, VariationSuccessResponse}
import uk.gov.hmrc.estates.transformers.ComposedDeltaTransform
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.ExecutionContext

class AuditService @Inject()(auditConnector: AuditConnector, config : AppConfig)(implicit ec: ExecutionContext) {

  private object AuditEvent {

    val GET_REGISTRATION = "GetRegistration"
    val GET_REGISTRATION_FAILED = "GetRegistrationFailed"

    val GET_VARIATION = "GetVariation"
    val GET_VARIATION_FAILED = "GetVariationFailed"

    val REGISTRATION_PREPARATION_FAILED = "RegistrationPreparationFailed"
    val REGISTRATION_SUBMISSION_FAILED = "RegistrationSubmissionFailed"
    val REGISTRATION_SUBMITTED_BY_ORGANISATION = "RegistrationSubmittedByOrganisation"
    val REGISTRATION_SUBMITTED_BY_AGENT = "RegistrationSubmittedByAGent"

    val VARIATION_PREPARATION_FAILED = "VariationPreparationFailed"
    val VARIATION_SUBMISSION_FAILED = "VariationSubmissionFailed"
    val VARIATION_SUBMITTED_BY_ORGANISATION = "VariationSubmittedByOrganisation"
    val VARIATION_SUBMITTED_BY_AGENT = "VariationSubmittedByAgent"
    val CLOSURE_SUBMITTED_BY_ORGANISATION = "ClosureSubmittedByOrganisation"
    val CLOSURE_SUBMITTED_BY_AGENT = "ClosureSubmittedByAgent"

  }

  def auditGetRegistrationSuccess(result: EstateRegistrationNoDeclaration)
                                 (implicit hc: HeaderCarrier, request: IdentifierRequest[_]): Unit =
    audit(
      AuditEvent.GET_REGISTRATION,
      Json.obj(),
      request.identifier,
      Json.toJson(result)
    )

  def auditGetRegistrationFailed(
                                   transforms: ComposedDeltaTransform,
                                   errorReason: String,
                                   jsErrors: String = "")
                                (implicit hc: HeaderCarrier, request: IdentifierRequest[_]): Unit =
    auditTransformationError(
      AuditEvent.GET_REGISTRATION_FAILED,
      Json.obj(),
      Json.toJson(transforms),
      errorReason,
      jsErrors)

  def auditGetVariationSuccess(utr: String, result: GetEstateProcessedResponse)
                              (implicit hc: HeaderCarrier, request: IdentifierRequest[_]): Unit =
    audit(
      AuditEvent.GET_VARIATION,
      Json.obj("utr" -> utr),
      request.identifier,
      Json.toJson(result)
    )

  def auditGetVariationFailed(utr: String, errorReason: JsValue)
                             (implicit hc: HeaderCarrier, request: IdentifierRequest[_]): Unit =
    auditErrorResponse(
      AuditEvent.GET_VARIATION_FAILED,
      Json.obj("utr" -> utr),
      request.identifier,
      errorReason)

  def auditRegistrationSubmitted(payload: EstateRegistration, trn: String)
                                (implicit hc: HeaderCarrier, request: IdentifierRequest[_]): Unit = {

   val event = if (request.affinityGroup == Agent) {
      AuditEvent.REGISTRATION_SUBMITTED_BY_AGENT
    } else {
      AuditEvent.REGISTRATION_SUBMITTED_BY_ORGANISATION
    }

    audit(
      event = event,
      request = Json.toJson(payload),
      internalId = request.identifier,
      response = Json.obj("trn" -> trn)
    )
  }

  def auditRegistrationFailed(
                               payload: EstateRegistration,
                               errorReason: String,
                               jsErrors: String = "")
                             (implicit hc: HeaderCarrier, request: IdentifierRequest[_]): Unit =
    audit(
      event = AuditEvent.REGISTRATION_SUBMISSION_FAILED,
      request = Json.toJson(payload),
      internalId = request.identifier,
      response = Json.obj("errorReason" -> errorReason)
    )

  def auditRegistrationTransformationError(
                                            data: JsValue = Json.obj(),
                                            transforms: JsValue = Json.obj(),
                                            errorReason: String = "",
                                            jsErrors: String = ""
                                          )(implicit hc: HeaderCarrier, request: IdentifierRequest[_]): Unit =
    auditTransformationError(
      AuditEvent.REGISTRATION_PREPARATION_FAILED,
      data,
      transforms,
      errorReason,
      jsErrors)

  def auditTransformationError(eventName: String,
                               data: JsValue,
                               transforms: JsValue,
                               errorReason: String,
                               jsErrors: String
                               )(implicit hc: HeaderCarrier, request: IdentifierRequest[_]): Unit = {
    val requestData = Json.obj(
      "data" -> data,
      "transformations" -> transforms
    )

    val responseData = Json.obj(
      "errorReason" -> errorReason,
      "jsErrors" -> jsErrors
    )

    audit(
      event = eventName,
      request = requestData,
      internalId = request.identifier,
      response = responseData
    )
  }

  def auditVariationSubmitted(internalId: String,
                              payload: JsValue,
                              response: VariationSuccessResponse
                             )(implicit hc: HeaderCarrier): Unit = {
    val hasField = (field: String) =>
      payload.transform((JsPath \ field).json.pick).isSuccess

    val isAgent = hasField("agentDetails")
    val isClose = hasField("trustEndDate")

    val event = (isAgent, isClose) match {
      case (false, false) => AuditEvent.VARIATION_SUBMITTED_BY_ORGANISATION
      case (false, true) => AuditEvent.CLOSURE_SUBMITTED_BY_ORGANISATION
      case (true, false) => AuditEvent.VARIATION_SUBMITTED_BY_AGENT
      case (true, true) => AuditEvent.CLOSURE_SUBMITTED_BY_AGENT
    }

    audit(
      event = event,
      request = payload,
      internalId = internalId,
      response = Json.toJson(response)
    )
  }

  def auditVariationFailed(internalId: String,
                           payload: JsValue,
                           response: VariationFailureResponse)
                          (implicit hc: HeaderCarrier): Unit =
    auditErrorResponse(
      eventName = AuditEvent.VARIATION_SUBMISSION_FAILED,
      request = Json.toJson(payload),
      internalId = internalId,
      errorReason = Json.toJson(response.response)
    )

  def auditVariationError(internalId: String,
                           payload: JsValue,
                           errorReason: String)
                          (implicit hc: HeaderCarrier): Unit =
    auditErrorResponse(
      eventName = AuditEvent.VARIATION_SUBMISSION_FAILED,
      request = Json.toJson(payload),
      internalId = internalId,
      errorReason = JsString(errorReason)
    )

  def auditVariationTransformationError(internalId: String,
                                        utr: String,
                                        data: JsValue = Json.obj(),
                                        transforms: JsValue,
                                        errorReason: String = "",
                                        jsErrors: JsValue = Json.obj()
                                       )(implicit hc: HeaderCarrier): Unit = {
    val request = Json.obj(
      "utr" -> utr,
      "data" -> data,
      "transformations" -> transforms
    )

    val response = Json.obj(
      "errorReason" -> errorReason,
      "jsErrors" -> jsErrors
    )

    audit(
      event = AuditEvent.VARIATION_PREPARATION_FAILED,
      request = request,
      internalId = internalId,
      response = response
    )
  }

  private def audit(event: String, request: JsValue, internalId: String, response: JsValue)
                   (implicit hc: HeaderCarrier): Unit = {

    val auditPayload = EstatesAuditData(
      request = request,
      internalAuthId = internalId,
      response = Some(response)
    )

    auditConnector.sendExplicitAudit(
      event,
      auditPayload
    )
  }

  private def auditErrorResponse(eventName: String, request: JsValue, internalId: String, errorReason: JsValue)
                                (implicit hc: HeaderCarrier): Unit = {

    val response = Json.obj("errorReason" -> errorReason)

    audit(
      event = eventName,
      request = request,
      internalId = internalId,
      response = response
    )
  }
}
