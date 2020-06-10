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

import java.time.LocalDate

import org.scalatest.{FreeSpec, MustMatchers, OptionValues}
import uk.gov.hmrc.estates.models.{EstatePerRepIndType, IdentificationType, NameType}
import uk.gov.hmrc.estates.utils.JsonUtils

class AddEstatePerRepIndTransformSpec extends FreeSpec with MustMatchers with OptionValues {

  val newPersonalRep = EstatePerRepIndType(
    name =  NameType("Alister", None, "Mc'Lovern"),
    dateOfBirth = LocalDate.of(1980,6,1),
    identification = IdentificationType(Some("JS123456A"), None, None),
    phoneNumber = "078888888",
    email = Some("test@abc.com")
  )

  "the add business settlor transformer should" - {

    "add a personal rep individual" - {

      "when there are existing personal reps" in {

        val trustJson = JsonUtils.getJsonValueFromFile("valid-estate-registration-01.json")

        val afterJson = JsonUtils.getJsonValueFromFile("transformed/valid-estate-registration-01-personal-rep-ind-transformed.json")

        val transformer = new AmendEstatePerRepIndTransform(newPersonalRep)

        val result = transformer.applyTransform(trustJson).get

        result mustBe afterJson
      }

      "when there are no existing personal reps" in {
        val trustJson = JsonUtils.getJsonValueFromFile("valid-estate-registration-01.json")

        val afterJson = JsonUtils.getJsonValueFromFile("transformed/valid-estate-registration-01-personal-rep-ind-transformed.json")

        val transformer = new AmendEstatePerRepIndTransform(newPersonalRep)

        val result = transformer.applyTransform(trustJson).get

        result mustBe afterJson
      }
    }

  }
}