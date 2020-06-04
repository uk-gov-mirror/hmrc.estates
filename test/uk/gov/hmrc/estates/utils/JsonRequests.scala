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
import uk.gov.hmrc.estates.models.variation.EstateVariation

trait JsonRequests extends JsonUtils {
  lazy val invalidEstateRegistrationJson: String =  getJsonFromFile("invalid-estate-registration-01.json")

  lazy val estateRegRequest: EstateRegistration = getJsonValueFromFile("valid-estate-registration-01.json").validate[EstateRegistration].get
  lazy val estateRegistration01: String =  getJsonFromFile("valid-estate-registration-01.json")
  lazy val estateRegistration03: String =  getJsonFromFile("valid-estate-registration-03.json")

  lazy val validEstateVariationsRequestJson: String = getJsonFromFile("valid-estate-variation-api.json")
  lazy val estateVariationsRequest: EstateVariation = getJsonValueFromFile("valid-estate-variation-api.json").validate[EstateVariation].get

  lazy val invalidEstateVariationsRequestJson: String = getJsonFromFile("invalid-estate-variation-api.json")
  lazy val invalidEstateVariationsRequest: JsValue = getJsonValueFromFile("invalid-estate-variation-api.json")

  lazy val getEstateResponseJson: String = getJsonFromFile("valid-get-estate-response.json")
  lazy val getEstateExpectedResponse: JsValue = getJsonValueFromFile("valid-get-estate-expected-response.json")

  lazy val getTrustOrEstateProcessingResponseJson: String = getJsonFromFile("valid-get-trust-or-estate-in-processing-response.json")
  lazy val getTrustOrEstateProcessingResponse: JsValue = getJsonValueFromFile("valid-get-trust-or-estate-in-processing-response.json")

  lazy val getTrustOrEstatePendingClosureResponseJson: String = getJsonFromFile("valid-get-trust-or-estate-pending-closure-response.json")
  lazy val getTrustOrEstatePendingClosureResponse: JsValue = getJsonValueFromFile("valid-get-trust-or-estate-pending-closure-response.json")

  lazy val getTrustOrEstateClosedResponseJson: String = getJsonFromFile("valid-get-trust-or-estate-closed-response.json")
  lazy val getTrustOrEstateClosedResponse: JsValue = getJsonValueFromFile("valid-get-trust-or-estate-closed-response.json")

  lazy val getTrustOrEstateSuspendedResponseJson: String = getJsonFromFile("valid-get-trust-or-estate-suspended-response.json")
  lazy val getTrustOrEstateSuspendedResponse: JsValue = getJsonValueFromFile("valid-get-trust-or-estate-suspended-response.json")

  lazy val getTrustOrEstateParkedResponseJson: String = getJsonFromFile("valid-get-trust-or-estate-parked-response.json")
  lazy val getTrustOrEstateParkedResponse: JsValue = getJsonValueFromFile("valid-get-trust-or-estate-parked-response.json")

  lazy val getTrustOrEstateObsoletedResponseJson: String = getJsonFromFile("valid-get-trust-or-estate-obsoleted-response.json")
  lazy val getTrustOrEstateObsoletedResponse: JsValue = getJsonValueFromFile("valid-get-trust-or-estate-obsoleted-response.json")


}
