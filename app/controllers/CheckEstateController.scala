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

import javax.inject.{Inject, Singleton}
import play.api.Logging
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import config.AppConfig
import controllers.actions.IdentifierAction
import models.ApiResponse._
import models._
import models.ExistingCheckResponse._
import services.EstatesService
import utils.Session

import scala.concurrent.ExecutionContext

@Singleton()
class CheckEstateController @Inject()(estateService: EstatesService, config: AppConfig,
                                      identify: IdentifierAction)
                                     (implicit val executionContext: ExecutionContext, cc: ControllerComponents)
  extends EstatesBaseController(cc) with Logging {

  def checkExistingEstate(): Action[JsValue] = identify.async(parse.json) { implicit request =>
      withJsonBody[ExistingCheckRequest] {
        estatesCheckRequest =>
          estateService.checkExistingEstate(estatesCheckRequest).map {
            result =>
              logger.info(s"[checkExistingEstate][Session ID: ${Session.id(hc)}] response: $result")
              result match {
                case Matched => Ok(matchResponse)
                case NotMatched => Ok(noMatchResponse)
                case AlreadyRegistered => Conflict(Json.toJson(alreadyRegisteredEstateResponse))
                case _ => InternalServerError(Json.toJson(internalServerErrorResponse))
              }
          }
      }
  }

}