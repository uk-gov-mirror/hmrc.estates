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

package uk.gov.hmrc.estates.transformers.variations.add

import java.time.LocalDate

import play.api.libs.json._
import uk.gov.hmrc.estates.models.variation.EstatePerRepOrgType
import uk.gov.hmrc.estates.transformers.{DeltaTransform, JsonOperations}
import uk.gov.hmrc.estates.utils.JsonOps._

case class AddBusinessPersonalRepTransform(newPersonalRep: EstatePerRepOrgType) extends DeltaTransform with JsonOperations {

  private lazy val path = __ \ 'details \ 'estate \ 'entities \ 'personalRepresentative

  override def applyTransform(input: JsValue): JsResult[JsValue] = {

    val endDate: LocalDate = newPersonalRep.entityStart

    val oldPersonalRep: JsValue = (input.transform(path.json.pick) match {
      case JsSuccess(value, _) => value match {
        case JsArray(array) =>
          array.filter(rep => rep.transform((__ \ 'lineNo).json.pick).isSuccess).head
        case rep: JsObject => Json.toJson(rep)
        case _ => Json.obj()
      }
      case _ => Json.obj()
    }).transform(__.json.update(
      (__ \ 'entityEnd).json.put(Json.toJson(endDate))
    )) match {
      case JsSuccess(rep, _) => rep
      case _ => Json.obj()
    }

    val personalReps = JsArray(Seq(oldPersonalRep.applyRules, Json.toJson(newPersonalRep).applyRules))

    input.transform(
      path.json.prune andThen
      __.json.update(path.json.put(personalReps))
    )
  }

  override def applyDeclarationTransform(input: JsValue): JsResult[JsValue] = {
    // remove address and copy to correspondence
    super.applyDeclarationTransform(input)
  }
}

object AddBusinessPersonalRepTransform {

  val key = "AddBusinessPersonalRepTransform"

  implicit val format: Format[AddBusinessPersonalRepTransform] = Json.format[AddBusinessPersonalRepTransform]
}
