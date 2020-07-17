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

package transforms

import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.estates.models.{AddressType, AgentDetails}
import uk.gov.hmrc.repositories.TransformIntegrationTest

class AgentDetailsSpec extends WordSpec with MustMatchers with MockitoSugar with TransformIntegrationTest {

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
    agentTelephoneNumber =  "07912180120",
    clientReference = "clientReference"
  )

  "an add agentDetails call" must {
    "return added data in a subsequent 'GET' call" in assertMongoTest(createApplication) { app =>

          val amendRequest = FakeRequest(POST, "/estates/agent-details")
            .withBody(Json.toJson(agentDetails))
            .withHeaders(CONTENT_TYPE -> "application/json")

          val amendResult = route(app, amendRequest).get
          status(amendResult) mustBe OK

          val newResult = route(app, FakeRequest(GET, "/estates/agent-details")).get
          status(newResult) mustBe OK
          contentAsJson(newResult) mustBe Json.toJson(agentDetails)
    }
  }
}
