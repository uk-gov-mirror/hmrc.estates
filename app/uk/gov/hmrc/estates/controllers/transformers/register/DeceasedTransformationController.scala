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

import java.time.LocalDate

import javax.inject.Inject
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsBoolean, JsError, JsSuccess, JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.estates.controllers.EstatesBaseController
import uk.gov.hmrc.estates.controllers.actions.IdentifierAction
import uk.gov.hmrc.estates.models.EstateWillType
import uk.gov.hmrc.estates.services.TransformationService
import uk.gov.hmrc.estates.transformers.{ComposedDeltaTransform, DeltaTransform}
import uk.gov.hmrc.estates.transformers.register.DeceasedTransform
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.{ExecutionContext, Future}

class DeceasedTransformationController @Inject()(
                                                  identify: IdentifierAction,
                                                  cc: ControllerComponents,
                                                  transformationService: TransformationService
                                                )(implicit val executionContext: ExecutionContext)
  extends EstatesBaseController(cc) {

  private val logger = LoggerFactory.getLogger("application." + this.getClass.getCanonicalName)

  def get: Action[AnyContent] = identify.async {
    request =>
      val result = transformationService.getTransformedData(request.identifier) map {
        case Some(ComposedDeltaTransform(transforms: Seq[DeltaTransform])) =>
          transforms.flatMap {
            case DeceasedTransform(deceased) => Some(Json.toJson(deceased))
            case _ => None
          }.lastOption
        case _ => None
      }
      result.map {
        case Some(json) => Ok(json)
        case None => Ok(Json.obj())
      }
  }

  def getDateOfDeath: Action[AnyContent] = identify.async {
    request =>
      val result = transformationService.getTransformedData(request.identifier) map {
        case Some(ComposedDeltaTransform(transforms: Seq[DeltaTransform])) =>
          transforms.flatMap {
            case DeceasedTransform(deceased) => Some(Json.toJson(deceased.dateOfDeath))
            case _ => None
          }.lastOption
        case _ => None
      }
      result.map {
        case Some(json) => Ok(json)
        case None => Ok(Json.obj())
      }
  }

  def getIsTaxRequired: Action[AnyContent] = identify.async {
    request =>
      val result = transformationService.getTransformedData(request.identifier) map {
        case Some(ComposedDeltaTransform(transforms: Seq[DeltaTransform])) =>
          transforms.flatMap {
            case DeceasedTransform(deceased) =>

              Some(JsBoolean(deceased.dateOfDeath isBefore LocalDate.parse(TaxYear.current.starts.toString)))
            case _ => None
          }.lastOption
        case _ => None
      }
      result.map {
        case Some(json) => Ok(json)
        case None => Ok(JsBoolean(false))
      }
  }

  def save: Action[JsValue] = identify.async(parse.json) {
    request => {
      request.body.validate[EstateWillType] match {
        case JsSuccess(deceased, _) =>
          transformationService.addNewTransform(request.identifier, DeceasedTransform(deceased)) map {
            _ => Ok
          }
        case JsError(errs) =>
          logger.warn(s"Supplied amount could not be read as Deceased - $errs")
          Future.successful(BadRequest)
      }
    }
  }

}
