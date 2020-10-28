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

package transformers.register

import org.scalatest.{FreeSpec, MustMatchers, OptionValues}
import play.api.libs.json.Json
import models.register.TaxAmount.AmountMoreThanTwoFiftyThousand
import utils.JsonUtils

class AmountOfTaxOwedTransformSpec extends FreeSpec with MustMatchers with OptionValues {

  "the amount of tax transform should" - {

    "add an amount" - {

      "when there is an existing period tax dues" in {

        val trustJson = JsonUtils.getJsonValueFromFile("mdtp/valid-estate-registration-01.json")

        val afterJson = JsonUtils.getJsonValueFromFile("transformed/valid-estate-registration-01-period-tax-dues-transformed.json")

        val transformer = new AmountOfTaxOwedTransform(AmountMoreThanTwoFiftyThousand)

        val result = transformer.applyTransform(trustJson).get

        result mustBe afterJson
      }

      "when there is no existing period tax dues" in {
        val trustJson = JsonUtils.getJsonValueFromFile("mdtp/valid-estate-registration-01-no-tax-dues.json")

        val afterJson = JsonUtils.getJsonValueFromFile("transformed/valid-estate-registration-01-period-tax-dues-transformed.json")

        val transformer = new AmountOfTaxOwedTransform(AmountMoreThanTwoFiftyThousand)

        val result = transformer.applyTransform(trustJson).get

        result mustBe afterJson
      }

      "when the document is empty" in {
        val transformer = AmountOfTaxOwedTransform(AmountMoreThanTwoFiftyThousand)

        val result = transformer.applyTransform(Json.obj()).get

        result mustBe Json.obj(
          "estate" -> Json.obj(
            "periodTaxDues" -> Json.toJson(AmountMoreThanTwoFiftyThousand.toString)
          )
        )
      }
    }

  }
}