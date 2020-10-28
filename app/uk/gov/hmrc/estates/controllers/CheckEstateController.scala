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
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.estates.config.AppConfig
import uk.gov.hmrc.estates.controllers.actions.IdentifierAction
import uk.gov.hmrc.estates.models.ApiResponse._
import uk.gov.hmrc.estates.models._
import uk.gov.hmrc.estates.models.ExistingCheckResponse._
import uk.gov.hmrc.estates.services.DesService
import uk.gov.hmrc.estates.utils.Session

import scala.concurrent.ExecutionContext

@Singleton()
class CheckEstateController @Inject()(desService: DesService, config: AppConfig,
                                      identify: IdentifierAction)
                                     (implicit val executionContext: ExecutionContext, cc: ControllerComponents)
  extends EstatesBaseController(cc) {

  private val logger: Logger = Logger(getClass)
  
  def checkExistingEstate(): Action[JsValue] = identify.async(parse.json) { implicit request =>
      withJsonBody[ExistingCheckRequest] {
        estatesCheckRequest =>
          desService.checkExistingEstate(estatesCheckRequest).map {
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