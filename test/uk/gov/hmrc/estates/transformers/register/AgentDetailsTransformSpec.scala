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

package uk.gov.hmrc.estates.transformers.register

import org.scalatest.{FreeSpec, MustMatchers, OptionValues}
import uk.gov.hmrc.estates.models.register.{AddressType, AgentDetails}
import uk.gov.hmrc.estates.utils.JsonUtils

class AgentDetailsTransformSpec extends FreeSpec with MustMatchers with OptionValues {

  private val agentDetails = AgentDetails(
    arn = "SARN1234567",
    agentName = "Agent name",
    agentAddress = AddressType(
      line1 = "56",
      line2 = "Maple Street",
      line3 = Some("Northumberland"),
      line4 = None,
      postCode = Some("ne64 8hr"),
      country = "GB"
    ),
    agentTelephoneNumber =  "07701086492",
    clientReference = "Agent01"
  )

  private val agentDetailsUpdated = AgentDetails(
    arn = "SARN1234567",
    agentName = "Agent name",
    agentAddress = AddressType(
      line1 = "56",
      line2 = "Maple Street",
      line3 = Some("Northumberland"),
      line4 = Some("North East"),
      postCode = Some("ne64 8hr"),
      country = "GB"
    ),
    agentTelephoneNumber =  "07701086492",
    clientReference = "Agent01"
  )

  "the agent details transform should" - {

    "add agent details" - {

      "when there is an existing agent details" in {

        val trustJson = JsonUtils.getJsonValueFromFile("valid-estate-registration-01.json")

        val afterJson = JsonUtils.getJsonValueFromFile("transformed/valid-estate-registration-01-agent-details-transformed.json")

        val transformer = new AgentDetailsTransform(agentDetailsUpdated)

        val result = transformer.applyTransform(trustJson).get

        result mustBe afterJson
      }

      "when there is no existing agent details" in {
        val trustJson = JsonUtils.getJsonValueFromFile("valid-estate-registration-01-no-agent-details.json")

        val afterJson = JsonUtils.getJsonValueFromFile("valid-estate-registration-01.json")

        val transformer = new AgentDetailsTransform(agentDetails)

        val result = transformer.applyTransform(trustJson).get

        result mustBe afterJson
      }
    }

  }
}