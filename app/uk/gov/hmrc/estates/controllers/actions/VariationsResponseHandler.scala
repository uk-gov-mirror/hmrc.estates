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

package uk.gov.hmrc.estates.controllers.actions

import javax.inject.Inject
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.mvc.Result
import uk.gov.hmrc.estates.models.requests.IdentifierRequest
import uk.gov.hmrc.estates.services.AuditService
import uk.gov.hmrc.estates.utils.ErrorResults._
import uk.gov.hmrc.estates.utils.Session
import uk.gov.hmrc.http.HeaderCarrier

class VariationsResponseHandler @Inject()(auditService: AuditService) {

  private val logger: Logger = Logger(getClass)

  def recoverFromException(implicit request: IdentifierRequest[JsValue],hc: HeaderCarrier): PartialFunction[Throwable, Result] = {

    case e =>
      logger.error(s"[ErrorHandler][Session ID: ${Session.id(hc)}] Exception returned ${e.getMessage}")

      auditService.auditVariationError(
        request.identifier,
        request.body,
        e.getMessage
      )
      internalServerErrorErrorResult
  }

}
