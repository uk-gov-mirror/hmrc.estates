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

package transformers.variations

import java.time.LocalDate

import org.scalatest.{FreeSpec, MustMatchers}
import models.IdentificationOrgType
import models.variation.EstatePerRepOrgType
import utils.JsonUtils

class AddAmendBusinessPersonalRepTransformSpec extends FreeSpec with MustMatchers {

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
        LocalDate.of(2011, 11, 21),
        None
      )
      val transformer = AddAmendBusinessPersonalRepTransform(newPersonalRep)

      val result = transformer.applyTransform(beforeJson).get
      result mustBe afterJson
    }

  }
}
