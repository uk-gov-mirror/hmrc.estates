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

package transformers

import base.BaseSpec
import play.api.libs.json.Json

class DeltaTransformSpec extends BaseSpec {

  "DeltaTransform" must {

    "not throw a match error when parsing a transform with an unrecognised key" in {
      val json = Json.parse(
        s"""{
           |  "deltaTransforms": [
           |    {
           |      "SomeUnknownTransformKey": {
           |        "key": "value"
           |      }
           |    }
           |  ]
           |}
           |""".stripMargin)

      val e = intercept[Exception] {
        json.as[ComposedDeltaTransform]
      }
      e.getMessage mustBe "Don't know how to de-serialise transform"
    }
  }
}
