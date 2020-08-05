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
import play.api.libs.json.{JsString, JsValue, Json}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.estates.config.AppConfig
import uk.gov.hmrc.estates.models.{EstateRegistration, EstateRegistrationNoDeclaration}
import uk.gov.hmrc.estates.models.auditing.{Auditing, GetTrustOrEstateAuditEvent}
import uk.gov.hmrc.estates.models.getEstate.GetEstateProcessedResponse
import uk.gov.hmrc.estates.models.requests.IdentifierRequest
import uk.gov.hmrc.estates.transformers.ComposedDeltaTransform
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.ExecutionContext

class AuditService @Inject()(auditConnector: AuditConnector, config : AppConfig)(implicit ec: ExecutionContext) {

  def audit(event: String, body: JsValue, internalId: String)(implicit hc: HeaderCarrier): Unit = {

    val auditPayload = GetTrustOrEstateAuditEvent(
      request = body,
      internalAuthId = internalId,
      response = None
    )

    auditConnector.sendExplicitAudit(
      event,
      auditPayload
    )
  }

  def audit(event: String, request: JsValue, internalId: String, response: JsValue)
           (implicit hc: HeaderCarrier): Unit = {

    val auditPayload = GetTrustOrEstateAuditEvent(
      request = request,
      internalAuthId = internalId,
      response = Some(response)
    )

    auditConnector.sendExplicitAudit(
      event,
      auditPayload
    )
  }

  // TODO: Get rid of this
  def auditErrorResponse(eventName: String, request: JsValue, internalId: String, errorReason: String)
                        (implicit hc: HeaderCarrier): Unit = {
    auditErrorResponse(eventName, request, internalId, JsString(errorReason))
  }

  // TODO: Make this private
  def auditErrorResponse(eventName: String, request: JsValue, internalId: String, errorReason: JsValue)
                        (implicit hc: HeaderCarrier): Unit = {

    val response = Json.obj("errorReason" -> errorReason)

    audit(
      event = eventName,
      request = request,
      internalId = internalId,
      response = response
    )
  }

  def auditGetRegistrationSuccess(result: EstateRegistrationNoDeclaration)
                                 (implicit hc: HeaderCarrier, request: IdentifierRequest[_]): Unit = {
    audit(
      Auditing.GET_REGISTRATION,
      Json.obj(),
      request.identifier,
      Json.toJson(result)
    )
  }

  def auditGetRegistrationFailed(
                                   transforms: ComposedDeltaTransform,
                                   errorReason: String,
                                   jsErrors: String = "")
                                (implicit hc: HeaderCarrier, request: IdentifierRequest[_]): Unit = {
    auditTransformationError(
      Auditing.GET_REGISTRATION_FAILED,
      Json.obj(),
      Json.toJson(transforms),
      errorReason,
      jsErrors)
  }

  def auditGetVariationSuccess(utr: String, result: GetEstateProcessedResponse)
                              (implicit hc: HeaderCarrier, request: IdentifierRequest[_]): Unit = {
    audit(
      Auditing.GET_VARIATION,
      Json.obj("utr" -> utr),
      request.identifier,
      Json.toJson(result)
    )
  }

  def auditGetVariationFailed(utr: String, errorReason: JsValue)
                             (implicit hc: HeaderCarrier, request: IdentifierRequest[_]): Unit = {
    auditErrorResponse(
      Auditing.GET_VARIATION_FAILED,
      Json.obj("utr" -> utr),
      request.identifier,
      errorReason)
  }

  def auditRegistrationSubmitted(payload: EstateRegistration, trn: String)
                                (implicit hc: HeaderCarrier, request: IdentifierRequest[_]): Unit = {

   val event = if (request.affinityGroup == Agent) {
      Auditing.REGISTRATION_SUBMITTED_BY_AGENT
    }
    else {
      Auditing.REGISTRATION_SUBMITTED_BY_ORGANISATION
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
                             (implicit hc: HeaderCarrier, request: IdentifierRequest[_]): Unit = {

    audit(
      event = Auditing.REGISTRATION_SUBMISSION_FAILED,
      request = Json.toJson(payload),
      internalId = request.identifier,
      response = Json.obj("errorReason" -> errorReason)
    )
  }

  def auditRegistrationTransformationError(
                                            data: JsValue = Json.obj(),
                                            transforms: JsValue = Json.obj(),
                                            errorReason: String = "",
                                            jsErrors: String = ""
                                          )(implicit hc: HeaderCarrier, request: IdentifierRequest[_]): Unit =
    auditTransformationError(
      Auditing.REGISTRATION_PREPARATION_FAILED,
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
}
