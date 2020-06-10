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
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.estates.controllers.EstatesBaseController
import uk.gov.hmrc.estates.controllers.actions.IdentifierAction
import uk.gov.hmrc.estates.services.LocalDateService
import uk.gov.hmrc.estates.services.register.AmountOfTaxTransformationService

import scala.concurrent.ExecutionContext

class AmountOfTaxOwedTransformationController @Inject()(
                                                         identify: IdentifierAction,
                                                         cc: ControllerComponents,
                                                         localDateService: LocalDateService,
                                                         amountOfTaxService : AmountOfTaxTransformationService
                                                       )(implicit val executionContext: ExecutionContext)
  extends EstatesBaseController(cc) {

  def get : Action[AnyContent] = identify.async {
    implicit request =>

      amountOfTaxService.get(request.identifier) map {
        case Some(x) => Ok(Json.obj("amount" -> x))
        case None => Ok(Json.obj())
      }
  }

}
