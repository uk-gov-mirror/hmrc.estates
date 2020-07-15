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

package uk.gov.hmrc.estates.controllers.transformers.variations

import javax.inject.Inject
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.estates.controllers.EstatesBaseController
import uk.gov.hmrc.estates.controllers.actions.IdentifierAction
import uk.gov.hmrc.estates.models.variation.PersonalRepresentativeType
import uk.gov.hmrc.estates.services.LocalDateService
import uk.gov.hmrc.estates.services.amend.PersonalRepTransformationService
import uk.gov.hmrc.estates.utils.ValidationUtil

import scala.concurrent.{ExecutionContext, Future}

class AmendPersonalRepTransformationController @Inject()(
                                                          identify: IdentifierAction,
                                                          cc: ControllerComponents,
                                                          personalRepTransformationService: PersonalRepTransformationService
                                        )(implicit val executionContext: ExecutionContext)
                                        extends EstatesBaseController(cc) with ValidationUtil {

  private val logger = LoggerFactory.getLogger("application." + this.getClass.getCanonicalName)

  def amendPersonalRep(utr: String): Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[PersonalRepresentativeType] match {
        case JsSuccess(model, _) =>
          personalRepTransformationService.addAmendPersonalRepTransformer(utr, request.identifier, model) map { _ =>
            Ok
          }
        case JsError(errors) =>
          logger.warn(s"Supplied personal representative could not be read as PersonalRepresentativeType - $errors")
          Future.successful(BadRequest)
      }
    }
  }

}
