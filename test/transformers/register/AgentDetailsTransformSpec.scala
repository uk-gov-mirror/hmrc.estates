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

package transformers.register

import org.scalatest.{FreeSpec, MustMatchers, OptionValues}
import play.api.libs.json.Json
import models.{AddressType, AgentDetails}
import utils.JsonUtils

class AgentDetailsTransformSpec extends FreeSpec with MustMatchers with OptionValues {

  private def agentDetails(phoneNumber: String) = AgentDetails(
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

        val trustJson = JsonUtils.getJsonValueFromFile("mdtp/valid-estate-registration-01.json")

        val afterJson = JsonUtils.getJsonValueFromFile("transformed/valid-estate-registration-01-agent-details-transformed.json")

        val transformer = new AgentDetailsTransform(agentDetailsUpdated)

        val result = transformer.applyTransform(trustJson).get

        result mustBe afterJson
      }

      "when there is no existing agent details" in {
        val details = agentDetails("07701086492")

        val trustJson = JsonUtils.getJsonValueFromFile("mdtp/valid-estate-registration-01-no-agent-details.json")

        val afterJson = JsonUtils.getJsonValueFromFile("mdtp/valid-estate-registration-01.json")

        val transformer = new AgentDetailsTransform(details)

        val result = transformer.applyTransform(trustJson).get

        result mustBe afterJson
      }

      "when the document is empty" in {
        val details = agentDetails("07701086492")

        val transformer = AgentDetailsTransform(details)

        val result = transformer.applyTransform(Json.obj()).get

        result mustBe Json.obj(
          "agentDetails" -> Json.toJson(details)
        )
      }
    }

  }

  "the agent details declaration transform should" - {
    "reformat agent telephone numbers" in {
      val details = agentDetails("(0)07701086492")

      val transformer = AgentDetailsTransform(details)

      val result = transformer.applyDeclarationTransform(Json.toJson(details)).get

      result.as[AgentDetails].agentTelephoneNumber mustBe "07701086492"
    }
  }
}