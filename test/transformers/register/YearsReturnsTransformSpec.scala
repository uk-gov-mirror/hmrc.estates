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

import org.scalatest.{FreeSpec, MustMatchers, OptionValues}
import play.api.libs.json.Json
import models.{YearReturnType, YearsReturns}
import utils.JsonUtils

class YearsReturnsTransformSpec extends FreeSpec with MustMatchers with OptionValues {

  private val yearsReturns: YearsReturns = YearsReturns(
    List(
      YearReturnType("20", taxConsequence = true)
    )
  )

  "the years returns transform should" - {

    "set the years" - {

      "where there are existing years" in {

        val trustJson = JsonUtils.getJsonValueFromFile("mdtp/valid-estate-registration-01.json")

        val afterJson = JsonUtils.getJsonValueFromFile("transformed/valid-estate-registration-01-with-tax-years.json")

        val transformer = YearsReturnsTransform(yearsReturns)

        val result = transformer.applyTransform(trustJson).get

        result mustBe afterJson
      }

      "where there are no existing years" in {

        val trustJson = JsonUtils.getJsonValueFromFile("mdtp/valid-estate-registration-01-no-tax-years.json")

        val afterJson = JsonUtils.getJsonValueFromFile("transformed/valid-estate-registration-01-with-tax-years.json")

        val transformer = YearsReturnsTransform(yearsReturns)

        val result = transformer.applyTransform(trustJson).get

        result mustBe afterJson
      }

      "when the document is empty" in {
        val transformer = YearsReturnsTransform(yearsReturns)

        val result = transformer.applyTransform(Json.obj()).get

        result mustBe Json.obj(
          "yearsReturns" -> Json.toJson(yearsReturns)
        )
      }
    }
  }

}
