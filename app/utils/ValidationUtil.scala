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

package utils

import java.time.LocalDate

import services.EstatesValidationError

trait ValidationUtil {

  def isNotFutureDate(date: LocalDate, path: String, key: String): Option[EstatesValidationError] = {
    if (isAfterToday(date)) {
      Some(EstatesValidationError(s"$key must be today or in the past.", path))
    } else {
      None
    }
  }

  def isNotFutureDate(date: Option[LocalDate], path: String, key: String): Option[EstatesValidationError] = {
    date flatMap {
      d =>
        isNotFutureDate(d, path, key)
    }
  }

  def isAfterToday(date: LocalDate): Boolean = {
    date.isAfter(LocalDate.now)
  }
}
