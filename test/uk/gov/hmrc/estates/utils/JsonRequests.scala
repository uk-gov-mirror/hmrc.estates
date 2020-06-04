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

package uk.gov.hmrc.estates.utils

import play.api.libs.json.JsValue
import uk.gov.hmrc.estates.models.EstateRegistration

trait JsonRequests extends JsonUtils {
  lazy val invalidEstateRegistrationJson: String =  getJsonFromFile("invalid-estate-registration-01.json")

  lazy val estateRegRequest: EstateRegistration = getJsonValueFromFile("valid-estate-registration-01.json").validate[EstateRegistration].get
  lazy val estateRegistration01: String =  getJsonFromFile("valid-estate-registration-01.json")
  lazy val estateRegistration03: String =  getJsonFromFile("valid-estate-registration-03.json")
}
