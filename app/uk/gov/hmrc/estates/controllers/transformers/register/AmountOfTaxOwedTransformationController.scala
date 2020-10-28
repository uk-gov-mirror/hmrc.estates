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
import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.estates.controllers.EstatesBaseController
import uk.gov.hmrc.estates.controllers.actions.IdentifierAction
import uk.gov.hmrc.estates.models.register.AmountOfTaxOwed
import uk.gov.hmrc.estates.services.LocalDateService
import uk.gov.hmrc.estates.services.register.AmountOfTaxTransformationService
import uk.gov.hmrc.estates.utils.Session

import scala.concurrent.{ExecutionContext, Future}

class AmountOfTaxOwedTransformationController @Inject()(
                                                         identify: IdentifierAction,
                                                         cc: ControllerComponents,
                                                         localDateService: LocalDateService,
                                                         amountOfTaxService : AmountOfTaxTransformationService
                                                       )(implicit val executionContext: ExecutionContext)
  extends EstatesBaseController(cc) {

  private val logger: Logger = Logger(getClass)

  def get : Action[AnyContent] = identify.async {
    implicit request =>

      amountOfTaxService.get(request.identifier) map {
        case Some(x) => Ok(Json.toJson(x))
        case None => Ok(Json.obj())
      }
  }

  def save: Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[AmountOfTaxOwed] match {
        case JsSuccess(model, _) =>
          amountOfTaxService.addTransform(request.identifier, model) map { _ =>
            Ok
          }
        case JsError(errors) =>
          logger.warn(s"[Session ID: ${Session.id(hc)}] " +
            s"Supplied amount could not be read as AmountOfTaxOwed - $errors")
          Future.successful(BadRequest)
      }
    }
  }

}
