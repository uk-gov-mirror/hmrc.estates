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

package uk.gov.hmrc.estates.transformers.register

import play.api.libs.json._
import uk.gov.hmrc.estates.models.register.TaxAmount
import uk.gov.hmrc.estates.transformers.{DeltaTransform, JsonOperations}

case class AmountOfTaxOwedTransform(amount: TaxAmount)
    extends DeltaTransform with JsonOperations {

  private val path = __ \ 'estate \ 'periodTaxDues

  override def applyTransform(input: JsValue): JsResult[JsValue] = {
    input.transform(
      path.json.prune andThen __.json.update(path.json.put(Json.toJson(amount)))
    )
  }

}

object AmountOfTaxOwedTransform {

  val key = "AmountOfTaxOwedTransform"

  implicit val format: Format[AmountOfTaxOwedTransform] = Json.format[AmountOfTaxOwedTransform]
}

