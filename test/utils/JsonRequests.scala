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

import play.api.libs.json.JsValue
import models.EstateRegistration

trait JsonRequests extends JsonUtils {
  lazy val invalidEstateRegistrationJson: String =  getJsonFromFile("mdtp/invalid-estate-registration-01.json")

  lazy val estateRegRequest: EstateRegistration = getJsonValueFromFile("mdtp/valid-estate-registration-01.json").validate[EstateRegistration].get
  lazy val estateRegistration01: String =  getJsonFromFile("mdtp/valid-estate-registration-01.json")
  lazy val estateRegistration03: String =  getJsonFromFile("mdtp/valid-estate-registration-03.json")

  lazy val validEstateVariationsRequestJson: String = getJsonFromFile("mdtp/valid-estate-variation-api.json")
  lazy val estateVariationsRequest: JsValue = getJsonValueFromFile("mdtp/valid-estate-variation-api.json").validate[JsValue].get

  lazy val invalidEstateVariationsRequestJson: String = getJsonFromFile("etmp/invalid-estate-variation-api.json")
  lazy val invalidEstateVariationsRequest: JsValue = getJsonValueFromFile("etmp/invalid-estate-variation-api.json")

  lazy val get4MLDEstateResponseJson: String = getJsonFromFile("etmp/valid-get-estate-response.json")
  lazy val get4MLDEstateResponse: JsValue = getJsonValueFromFile("etmp/valid-get-estate-response.json")

  lazy val getTransformedEstateResponse: JsValue = getJsonValueFromFile("transformed/variations/valid-get-estate-response-transformed.json")
  lazy val getTransformedPersonalRepResponse: JsValue = getJsonValueFromFile("transformed/variations/valid-get-estate-response-transformed-personal-rep-only.json")

  lazy val getEstateInvalidResponseJson: JsValue = getJsonValueFromFile("etmp/valid-get-estate-invalid-response.json")
  lazy val getEstateExpectedResponse: JsValue = getJsonValueFromFile("mdtp/valid-get-estate-expected-response.json")

  lazy val getTrustOrEstateProcessingResponseJson: String = getJsonFromFile("etmp/valid-get-trust-or-estate-in-processing-response.json")
  lazy val getTrustOrEstateProcessingResponse: JsValue = getJsonValueFromFile("etmp/valid-get-trust-or-estate-in-processing-response.json")

  lazy val getTrustOrEstatePendingClosureResponseJson: String = getJsonFromFile("etmp/valid-get-trust-or-estate-pending-closure-response.json")
  lazy val getTrustOrEstatePendingClosureResponse: JsValue = getJsonValueFromFile("etmp/valid-get-trust-or-estate-pending-closure-response.json")

  lazy val getTrustOrEstateClosedResponseJson: String = getJsonFromFile("etmp/valid-get-trust-or-estate-closed-response.json")
  lazy val getTrustOrEstateClosedResponse: JsValue = getJsonValueFromFile("etmp/valid-get-trust-or-estate-closed-response.json")

  lazy val getTrustOrEstateSuspendedResponseJson: String = getJsonFromFile("etmp/valid-get-trust-or-estate-suspended-response.json")
  lazy val getTrustOrEstateSuspendedResponse: JsValue = getJsonValueFromFile("etmp/valid-get-trust-or-estate-suspended-response.json")

  lazy val getTrustOrEstateParkedResponseJson: String = getJsonFromFile("etmp/valid-get-trust-or-estate-parked-response.json")
  lazy val getTrustOrEstateParkedResponse: JsValue = getJsonValueFromFile("etmp/valid-get-trust-or-estate-parked-response.json")

  lazy val getTrustOrEstateObsoletedResponseJson: String = getJsonFromFile("etmp/valid-get-trust-or-estate-obsoleted-response.json")
  lazy val getTrustOrEstateObsoletedResponse: JsValue = getJsonValueFromFile("etmp/valid-get-trust-or-estate-obsoleted-response.json")


}
