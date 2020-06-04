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
import uk.gov.hmrc.estates.controllers.actions.{IdentifierAction, VariationsResponseHandler}
import uk.gov.hmrc.estates.models.auditing.Auditing
import uk.gov.hmrc.estates.models.variation.EstateVariation
import uk.gov.hmrc.estates.services.{AuditService, DesService, ValidationService}
import uk.gov.hmrc.estates.utils.ValidationUtil
import uk.gov.hmrc.estates.utils.ErrorResponses._

import scala.concurrent.{ExecutionContext, Future}

class EstateVariationsController @Inject()(
                                            identify: IdentifierAction,
                                            desService: DesService,
                                            auditService: AuditService,
                                            validator: ValidationService,
                                            config : AppConfig,
                                            responseHandler: VariationsResponseHandler)
                                          (implicit ec: ExecutionContext, cc: ControllerComponents) extends EstatesBaseController(cc) with ValidationUtil {

  def estateVariation(): Action[JsValue] = identify.async(parse.json) {
    implicit request =>

      val payload = request.body.toString()

      validator.get(config.variationsApiSchema).validate[EstateVariation](payload).fold(
        errors => {
          Logger.error(s"[variations] estate validation errors from request body $errors.")

          auditService.auditErrorResponse(
            Auditing.ESTATE_VARIATION,
            request.body,
            request.identifier,
            errorReason = "Provided request is invalid."
          )


          Future.successful(invalidRequestErrorResponse)
        },

        variationRequest => {
          desService.estateVariation(variationRequest) map { response =>

            auditService.audit(
              Auditing.ESTATE_VARIATION,
              request.body,
              request.identifier,
              response = Json.toJson(response)
            )

            Ok(Json.toJson(response))

          } recover responseHandler.recoverFromException(Auditing.ESTATE_VARIATION)
        }
      )
  }

}

