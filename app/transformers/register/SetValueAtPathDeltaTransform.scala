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

import play.api.libs.json.{JsPath, JsResult, JsValue, Json, __}
import transformers.DeltaTransform

abstract class SetValueAtPathDeltaTransform extends DeltaTransform {

  val path: JsPath

  val value: JsValue

  override def applyTransform(input: JsValue): JsResult[JsValue] = {
    if (input.transform(path.json.pick).isSuccess) {
      input.transform(
        path.json.prune andThen __.json.update(path.json.put(Json.toJson(value)))
      )
    } else {
      input.transform(
        __.json.update(path.json.put(Json.toJson(value)))
      )
    }
  }

}
