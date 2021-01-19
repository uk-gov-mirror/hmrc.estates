/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers

import javax.inject.Inject
import play.api.Logging
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import controllers.actions.{IdentifierAction, VariationsResponseHandler}
import models.DeclarationForApi
import models.variation.{VariationFailureResponse, VariationSuccessResponse}
import services.maintain.VariationService
import utils.{ErrorResults, Session}

import scala.concurrent.{ExecutionContext, Future}

class EstateVariationsController @Inject()(
                                            identify: IdentifierAction,
                                            variationService: VariationService,
                                            responseHandler: VariationsResponseHandler
                                          )(implicit ec: ExecutionContext, cc: ControllerComponents
) extends EstatesBaseController(cc) with Logging {

  def declare(utr: String): Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[DeclarationForApi].fold(
        errors => {
          logger.error(s"[declare][Session ID: ${Session.id(hc)}][UTR: $utr] unable to parse json as DeclarationForApi, $errors")
          Future.successful(BadRequest)
        },
        declarationForApi => {
          variationService
            .submitDeclaration(utr, request.identifier, declarationForApi)
            .map {
              case response: VariationSuccessResponse => Ok(Json.toJson(response))
              case VariationFailureResponse(errorResponse) => ErrorResults.fromErrorResponse(errorResponse)
            }
        } recover responseHandler.recoverFromException
      )
    }
  }
}
