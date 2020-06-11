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

package uk.gov.hmrc.estates.transformers

import play.api.libs.json._
import uk.gov.hmrc.estates.models.EstatePerRepOrgType

case class AmendCorrespondenceNameTransform(newCorrespondenceName: JsString)
  extends DeltaTransform with JsonOperations {

  private lazy val path = __ \ 'correspondence \ 'name

  override def applyTransform(input: JsValue): JsResult[JsValue] = {
    input.transform(
      path.json.prune andThen __.json.update(path.json.put(newCorrespondenceName))
    )
  }

}

object AmendCorrespondenceNameTransform {

  val key = "AmendCorrespondenceNameTransform"

  implicit val format: Format[AmendCorrespondenceNameTransform] = Json.format[AmendCorrespondenceNameTransform]
}
