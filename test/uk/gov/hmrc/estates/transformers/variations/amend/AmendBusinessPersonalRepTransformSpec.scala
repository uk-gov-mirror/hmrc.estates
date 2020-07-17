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

package uk.gov.hmrc.estates.transformers.variations.amend

import java.time.LocalDate

import org.scalatest.{FreeSpec, MustMatchers}
import uk.gov.hmrc.estates.models.IdentificationOrgType
import uk.gov.hmrc.estates.models.variation.EstatePerRepOrgType
import uk.gov.hmrc.estates.utils.JsonUtils

class AmendBusinessPersonalRepTransformSpec extends FreeSpec with MustMatchers {

  "the modify business personal rep transformer should" - {
    "successfully set a new business personal rep details" in {
      val beforeJson = JsonUtils.getJsonValueFromFile("transformed/variations/estates-personal-rep-transform-before.json")
      val afterJson = JsonUtils.getJsonValueFromFile("transformed/variations/estates-personal-rep-transform-after-org.json")
      val newPersonalRep = EstatePerRepOrgType(
        lineNo = Some("newLineNo"),
        bpMatchStatus = Some("newMatchStatus"),
        orgName = "newName",
        phoneNumber = "newPhone",
        email = Some("newEmail"),
        identification = IdentificationOrgType(Some("newUtr"), None),
        LocalDate.now,
        None
      )
      val transformer = AmendBusinessPersonalRepTransform(newPersonalRep)

      val result = transformer.applyTransform(beforeJson).get
      result mustBe afterJson
    }

  }
}
