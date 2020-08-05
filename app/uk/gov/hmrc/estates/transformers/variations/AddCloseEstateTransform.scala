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

package uk.gov.hmrc.estates.transformers.variations

import java.time.LocalDate

import play.api.libs.json._
import uk.gov.hmrc.estates.transformers.register.SetValueAtPathDeltaTransform

case class AddCloseEstateTransform(closeDate: LocalDate) extends SetValueAtPathDeltaTransform {

  override val path: JsPath = __ \ 'trustEndDate

  override val value: JsValue = Json.toJson(closeDate)
}

object AddCloseEstateTransform {

  val key = "AddCloseEstateTransform"

  implicit val format: Format[AddCloseEstateTransform] = Json.format[AddCloseEstateTransform]
}
