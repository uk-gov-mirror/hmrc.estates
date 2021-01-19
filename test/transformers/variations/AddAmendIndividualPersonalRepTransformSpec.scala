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
import models.{IdentificationType, NameType}
import models.variation.EstatePerRepIndType
import utils.JsonUtils

class AddAmendIndividualPersonalRepTransformSpec extends FreeSpec with MustMatchers {
  "the modify individual personal rep transformer should" - {
    "successfully set a new ind individual personal rep details" in {
      val beforeJson = JsonUtils.getJsonValueFromFile("transformed/variations/estates-personal-rep-transform-before.json")
      val afterJson = JsonUtils.getJsonValueFromFile("transformed/variations/estates-personal-rep-transform-after-ind.json")
      val newTrusteeInfo = EstatePerRepIndType(
        lineNo = Some("newLineNo"),
        bpMatchStatus = Some("MatchStatus"),
        name = NameType("newFirstName", Some("newMiddleName"), "newLastName"),
        dateOfBirth = LocalDate.of(1965, 2, 10),
        phoneNumber = "newPhone",
        email = Some("newEmail"),
        identification = IdentificationType(Some("newNino"), None, None),
        entityStart = LocalDate.parse("2012-03-14"),
        entityEnd = None
      )
      val transformer = AddAmendIndividualPersonalRepTransform(newTrusteeInfo)

      val result = transformer.applyTransform(beforeJson).get
      result mustBe afterJson
    }
  }
}
