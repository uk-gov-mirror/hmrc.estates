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
import uk.gov.hmrc.estates.controllers.actions.{IdentifierAction, VariationsResponseHandler}
import uk.gov.hmrc.estates.models.DeclarationForApi
import uk.gov.hmrc.estates.models.auditing.Auditing
import uk.gov.hmrc.estates.services.{AuditService, VariationDeclarationService}

import scala.concurrent.{ExecutionContext, Future}

class EstateVariationsController @Inject()(
                                            identify: IdentifierAction,
                                            auditService: AuditService,
                                            variationService: VariationDeclarationService,
                                            responseHandler: VariationsResponseHandler
                                          )(implicit ec: ExecutionContext, cc: ControllerComponents) extends EstatesBaseController(cc) {


  def declare(utr: String): Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[DeclarationForApi].fold(
        errors => {

          auditService.audit(
            Auditing.ESTATE_VARIATION,
            Json.obj("declaration" -> request.body),
            request.identifier,
            Json.toJson(Json.obj())
          )

          Logger.error(s"[EstateVariationsDeclarationController][declare] unable to parse json as DeclarationForApi, $errors")
          Future.successful(BadRequest)
        },
        declarationForApi => {
          variationService
            .submitDeclaration(utr, request.identifier, declarationForApi)
            .map {
              response =>

                auditService.audit(
                  Auditing.ESTATE_VARIATION,
                  Json.toJson(declarationForApi),
                  request.identifier,
                  Json.toJson(response)
                )
                Ok(Json.toJson(response))
            }
        } recover responseHandler.recoverFromException(Auditing.ESTATE_VARIATION)
      )
    }
  }
}
