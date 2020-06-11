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

package uk.gov.hmrc.estates.controllers.transformers.register

import javax.inject.Inject
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.estates.controllers.EstatesBaseController
import uk.gov.hmrc.estates.controllers.actions.IdentifierAction
import uk.gov.hmrc.estates.models.register.AgentDetails
import uk.gov.hmrc.estates.services.LocalDateService
import uk.gov.hmrc.estates.services.register.AgentDetailsTransformationService

import scala.concurrent.{ExecutionContext, Future}

class AgentDetailsTransformationController @Inject()(
                                                         identify: IdentifierAction,
                                                         cc: ControllerComponents,
                                                         localDateService: LocalDateService,
                                                         agentDetailsTransformationService : AgentDetailsTransformationService
                                                       )(implicit val executionContext: ExecutionContext)
  extends EstatesBaseController(cc) {

  private val logger = LoggerFactory.getLogger("application." + this.getClass.getCanonicalName)

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
          logger.warn(s"Agent details could not be read as AgentDetails - $errors")
          Future.successful(BadRequest)
      }
    }
  }

}
