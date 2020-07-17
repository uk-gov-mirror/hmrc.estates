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

package uk.gov.hmrc.estates.transformers.variations.add

import java.time.LocalDate

import org.scalatest.{FreeSpec, MustMatchers}
import uk.gov.hmrc.estates.models.IdentificationOrgType
import uk.gov.hmrc.estates.models.variation.EstatePerRepOrgType
import uk.gov.hmrc.estates.utils.JsonUtils

class AddBusinessPersonalRepTransformSpec extends FreeSpec with MustMatchers {

  "the add business personal rep transformer should" - {

    "successfully add a new personal rep and set the end date of the old one" in {

      val beforeJson = JsonUtils.getJsonValueFromFile("transformed/variations/estates-add-business-personal-rep-transform-before.json")
      val afterJson = JsonUtils.getJsonValueFromFile("transformed/variations/estates-add-business-personal-rep-transform-after.json")

      val newPersonalRep = EstatePerRepOrgType(
        lineNo = None,
        bpMatchStatus = None,
        orgName = "newName",
        phoneNumber = "newPhone",
        email = Some("newEmail"),
        identification = IdentificationOrgType(Some("newUtr"), None),
        LocalDate.parse("2020-02-03"),
        None
      )

      val transformer = AddBusinessPersonalRepTransform(newPersonalRep)

      val result = transformer.applyTransform(beforeJson).get
      result mustBe afterJson
    }

    "successfully replace the personal rep that has been added in session" in {

      val beforeJson = JsonUtils.getJsonValueFromFile("transformed/variations/estates-add-business-personal-rep-transform-after.json")
      val afterJson = JsonUtils.getJsonValueFromFile("transformed/variations/estates-add-business-personal-rep-transform-again.json")

      val newPersonalRep = EstatePerRepOrgType(
        lineNo = None,
        bpMatchStatus = None,
        orgName = "newerName",
        phoneNumber = "newerPhone",
        email = Some("newerEmail"),
        identification = IdentificationOrgType(Some("newerUtr"), None),
        LocalDate.parse("2021-02-03"),
        None
      )

      val transformer = AddBusinessPersonalRepTransform(newPersonalRep)

      val result = transformer.applyTransform(beforeJson).get
      result mustBe afterJson
    }

    "successfully remove address from personal rep with UTR and put in correspondence at declaration time" in {

    }

  }
}
