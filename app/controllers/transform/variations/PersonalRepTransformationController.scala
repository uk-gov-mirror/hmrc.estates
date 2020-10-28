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

package controllers.transform.variations

import javax.inject.Inject
import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import play.api.mvc.{Action, ControllerComponents}
import controllers.EstatesBaseController
import controllers.actions.IdentifierAction
import models.variation.PersonalRepresentativeType
import services.maintain.PersonalRepTransformationService
import utils.ValidationUtil
import utils.Session

import scala.concurrent.{ExecutionContext, Future}

class PersonalRepTransformationController @Inject()(
                                                          identify: IdentifierAction,
                                                          cc: ControllerComponents,
                                                          personalRepTransformationService: PersonalRepTransformationService
                                        )(implicit val executionContext: ExecutionContext)
                                        extends EstatesBaseController(cc) with ValidationUtil {

  private val logger: Logger = Logger(getClass)

  def addOrAmendPersonalRep(utr: String): Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[PersonalRepresentativeType] match {
        case JsSuccess(model, _) =>
          personalRepTransformationService.addAmendPersonalRepTransformer(utr, request.identifier, model) map { _ =>
            Ok
          }
        case JsError(errors) =>
          logger.warn(s"[Session ID: ${Session.id(hc)}][UTR: $utr]" +
            s" Supplied personal representative could not be read as PersonalRepresentativeType - $errors")
          Future.successful(BadRequest)
      }
    }
  }

}
