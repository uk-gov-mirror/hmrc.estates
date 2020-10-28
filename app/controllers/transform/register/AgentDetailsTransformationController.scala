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

package controllers.transform.register

import javax.inject.Inject
import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import controllers.EstatesBaseController
import controllers.actions.IdentifierAction
import models.AgentDetails
import services.LocalDateService
import services.register.AgentDetailsTransformationService
import utils.Session

import scala.concurrent.{ExecutionContext, Future}

class AgentDetailsTransformationController @Inject()(
                                                         identify: IdentifierAction,
                                                         cc: ControllerComponents,
                                                         localDateService: LocalDateService,
                                                         agentDetailsTransformationService : AgentDetailsTransformationService
                                                       )(implicit val executionContext: ExecutionContext)
  extends EstatesBaseController(cc) {

  private val logger: Logger = Logger(getClass)

  def get : Action[AnyContent] = identify.async {
    implicit request =>

      agentDetailsTransformationService.get(request.identifier) map {
        case Some(x) => Ok(Json.toJson(x))
        case None => Ok(Json.obj())
      }
  }

  def save: Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[AgentDetails] match {
        case JsSuccess(model, _) =>
          agentDetailsTransformationService.addTransform(request.identifier, model) map { _ =>
            Ok
          }
        case JsError(errors) =>
          logger.warn(s"[Session ID: ${Session.id(hc)}] Agent details could not be read as AgentDetails - $errors")
          Future.successful(BadRequest)
      }
    }
  }

}
