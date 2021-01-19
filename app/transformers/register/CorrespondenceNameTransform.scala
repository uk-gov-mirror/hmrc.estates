/*
 * Copyright 2021 HM Revenue & Customs
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

package transformers.register

import play.api.libs.json._
import transformers.{DeltaTransform, JsonOperations}

case class CorrespondenceNameTransform(newCorrespondenceName: JsString)
  extends DeltaTransform with JsonOperations {

  private val path: JsPath = __ \ 'correspondence \ 'name

  override def applyTransform(input: JsValue): JsResult[JsValue] = {
    input.transform(
      __.json.update(path.json.put(newCorrespondenceName))
    )
  }

}

object CorrespondenceNameTransform {

  val key = "AddCorrespondenceNameTransform"

  implicit val format: Format[CorrespondenceNameTransform] = Json.format[CorrespondenceNameTransform]
}
