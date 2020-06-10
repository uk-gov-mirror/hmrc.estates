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

package uk.gov.hmrc.estates.models.register

import uk.gov.hmrc.estates.models.{Enumerable, WithName}

sealed trait AmountOfTaxOwed

object AmountOfTaxOwed extends Enumerable.Implicits {

  case object AmountMoreThanThenThousand extends WithName("01") with AmountOfTaxOwed
  case object AmountMoreThanTwoFiftyThousand extends WithName("02") with AmountOfTaxOwed
  case object AmountMoreThanFiveHundredThousand extends WithName("03") with AmountOfTaxOwed
  case object AmountMoreThanTwoHalfMillion extends WithName("04") with AmountOfTaxOwed

  val values: Set[AmountOfTaxOwed] = Set(
    AmountMoreThanThenThousand, AmountMoreThanTwoFiftyThousand, AmountMoreThanFiveHundredThousand, AmountMoreThanTwoHalfMillion
  )

  implicit val enumerable: Enumerable[AmountOfTaxOwed] =
    Enumerable(values.toSeq.map(v => v.toString -> v): _*)
}