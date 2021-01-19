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

import java.time.LocalDate

import org.scalatest.{FreeSpec, MustMatchers, OptionValues}
import play.api.libs.json.Json
import models.{EstateWillType, IdentificationType, NameType}
import utils.JsonUtils

class DeceasedTransformSpec extends FreeSpec with MustMatchers with OptionValues {

  private val newDeceased = EstateWillType(
    NameType("New First", None, "New Last"),
    Some(LocalDate.of(1996, 4, 15)),
    LocalDate.of(2016, 7, 2),
    Some(IdentificationType(
      nino = Some("AA123456B"),
      address = None,
      passport = None
    ))
  )

  "the deceased transform should" - {

    "set the deceased" - {

      "when there is an existing deceased" in {

        val trustJson = JsonUtils.getJsonValueFromFile("mdtp/valid-estate-registration-01.json")

        val afterJson = JsonUtils.getJsonValueFromFile("transformed/valid-estate-registration-01-deceased-transformed.json")

        val transformer = DeceasedTransform(newDeceased)

        val result = transformer.applyTransform(trustJson).get

        result mustBe afterJson
      }

      "when the document is empty" in {
        val transformer = DeceasedTransform(newDeceased)

        val result = transformer.applyTransform(Json.obj()).get

        result mustBe Json.obj(
          "estate" -> Json.obj(
            "entities" -> Json.obj(
              "deceased" -> Json.toJson(newDeceased)
            )
          )
        )
      }
    }
  }
}
