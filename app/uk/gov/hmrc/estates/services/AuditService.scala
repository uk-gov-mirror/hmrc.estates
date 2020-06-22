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
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.estates.config.AppConfig
import uk.gov.hmrc.estates.models.auditing.{EstateRegistrationSubmissionAuditEvent, GetTrustOrEstateAuditEvent}
import uk.gov.hmrc.estates.models.{EstateRegistration, RegistrationResponse}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.ExecutionContext

class AuditService @Inject()(auditConnector: AuditConnector, config : AppConfig)(implicit ec: ExecutionContext) {

  def audit(event: String, registration: EstateRegistration, internalId: String, response: RegistrationResponse)
           (implicit hc: HeaderCarrier): Unit = {

    val auditPayload = EstateRegistrationSubmissionAuditEvent(
      registration = registration,
      internalAuthId = internalId,
      response = response
    )

    auditConnector.sendExplicitAudit(
      event,
      auditPayload
    )
  }

  def audit(event: String, body: JsValue, internalId: String)(implicit hc: HeaderCarrier) = {

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

  def auditErrorResponse(eventName: String, request: JsValue, internalId: String, errorReason: String)
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
