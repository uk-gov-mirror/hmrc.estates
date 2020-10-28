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

package controllers

import javax.inject.Inject
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import controllers.actions.IdentifierAction
import services.CorrespondenceTransformationService
import utils.{Session, ValidationUtil}

import scala.concurrent.{ExecutionContext, Future}

class CorrespondenceTransformationController @Inject()(
                                                        identify: IdentifierAction,
                                                        personalRepTransformationService: CorrespondenceTransformationService,
                                                        cc: ControllerComponents
                                        )(implicit val executionContext: ExecutionContext) extends EstatesBaseController(cc) with ValidationUtil {

  private val logger: Logger = Logger(getClass)

  def getCorrespondenceName(): Action[AnyContent] = identify.async {
    implicit request =>
      personalRepTransformationService.getCorrespondenceName(request.identifier) map { correspondenceName =>
        Ok(
          correspondenceName.map(name => Json.obj("name" -> name)).getOrElse(Json.obj())
        )
      }
  }

  def addCorrespondenceName(): Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[JsString] match {
        case JsSuccess(model, _) =>
          personalRepTransformationService.addAmendCorrespondenceNameTransformer(request.identifier, model) map { _ =>
            Ok
          }
        case JsError(errors) =>
          logger.warn(s"[addCorrespondenceName][Session ID: ${Session.id(hc)}]" +
            s" Supplied details could not be read as JsString - $errors")
          Future.successful(BadRequest)
      }
    }
  }

}
