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
import uk.gov.hmrc.estates.models.{AddressType, EstatePerRepIndType, EstatePerRepOrgType, IdentificationOrgType, IdentificationType, NameType}
import uk.gov.hmrc.estates.utils.JsonUtils

class AddEstatePerRepTransformSpec extends FreeSpec with MustMatchers with OptionValues {

  val newPersonalRepInd = EstatePerRepIndType(
    name =  NameType("Alister", None, "Mc'Lovern"),
    dateOfBirth = LocalDate.of(1980,6,1),
    identification = IdentificationType(Some("JS123456A"), None, None),
    phoneNumber = "078888888",
    email = Some("test@abc.com")
  )

  val newPersonalRepOrg = EstatePerRepOrgType(
    orgName =  "Lovely Organisation",
    identification = IdentificationOrgType(
      None,
      Some(AddressType("line1", "line2", Some("line3"), Some("line4"), Some("postCode"), "Country"))
    ),
    phoneNumber = "078888888",
    email = Some("testy@xyz.org")
  )

  "the add personal rep transformer should" - {

    "add a personal rep individual" - {

      "when there is an existing personal rep" in {

        val trustJson = JsonUtils.getJsonValueFromFile("valid-estate-registration-01.json")

        val afterJson = JsonUtils.getJsonValueFromFile("transformed/valid-estate-registration-01-personal-rep-ind-transformed.json")

        val transformer = new AddEstatePerRepTransform(Some(newPersonalRepInd), None)

        val result = transformer.applyTransform(trustJson).get

        result mustBe afterJson
      }

      "when there are no existing personal reps" in {
        val trustJson = JsonUtils.getJsonValueFromFile("valid-estate-registration-01.json")

        val afterJson = JsonUtils.getJsonValueFromFile("transformed/valid-estate-registration-01-personal-rep-ind-transformed.json")

        val transformer = new AddEstatePerRepTransform(Some(newPersonalRepInd), None)

        val result = transformer.applyTransform(trustJson).get

        result mustBe afterJson
      }
    }
    "add a personal rep organisation" - {

      "when there is an existing personal rep" in {

        val trustJson = JsonUtils.getJsonValueFromFile("valid-estate-registration-01.json")

        val afterJson = JsonUtils.getJsonValueFromFile("transformed/valid-estate-registration-01-personal-rep-org-transformed.json")

        val transformer = new AddEstatePerRepTransform(None, Some(newPersonalRepOrg))

        val result = transformer.applyTransform(trustJson).get

        result mustBe afterJson
      }

      "when there are no existing personal reps" in {
        val trustJson = JsonUtils.getJsonValueFromFile("valid-estate-registration-01.json")

        val afterJson = JsonUtils.getJsonValueFromFile("transformed/valid-estate-registration-01-personal-rep-org-transformed.json")

        val transformer = new AddEstatePerRepTransform(None, Some(newPersonalRepOrg))

        val result = transformer.applyTransform(trustJson).get

        result mustBe afterJson
      }
    }

  }
}