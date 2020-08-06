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

package uk.gov.hmrc.estates.transformers.variations

import java.time.LocalDate

import org.scalatest.{FreeSpec, MustMatchers}
import play.api.libs.json.Json

class AddCloseEstateTransformSpec extends FreeSpec with MustMatchers {

  "the close estate transformer should" - {

    val closeDate: LocalDate = LocalDate.parse("2000-01-01")

    "successfully set the estate close date" in {

      val beforeJson = Json.obj()
      val afterJson = Json.obj("trustEndDate" -> closeDate)

      val transformer = AddCloseEstateTransform(closeDate)

      val result = transformer.applyTransform(beforeJson).get

      result mustBe afterJson
    }
  }
}
