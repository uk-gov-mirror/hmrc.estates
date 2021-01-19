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

package controllers.transform.register

import base.BaseSpec
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.MustMatchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc.{BodyParsers, ControllerComponents}
import play.api.test.FakeRequest
import play.api.test.Helpers.{CONTENT_TYPE, _}
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import controllers.actions.FakeIdentifierAction
import models.{AddressType, AgentDetails, Success}
import services.register.AgentDetailsTransformationService

import scala.concurrent.Future

class AgentDetailsTransformationControllerSpec extends BaseSpec with MockitoSugar with ScalaFutures with MustMatchers {

  import scala.concurrent.ExecutionContext.Implicits._

  private implicit val cc: ControllerComponents = injector.instanceOf[ControllerComponents]
  private val bodyParsers = injector.instanceOf[BodyParsers.Default]

  private val agentDetails = AgentDetails(
    arn = "AARN1234567",
    agentName = "Mr. xys abcde",
    agentAddress = AddressType(
      line1 = "line1",
      line2 = "line2",
      line3 = None,
      line4 = None,
      postCode = Some("TF3 2BX"),
      country = "GB"
    ),
    agentTelephoneNumber =  "07912180120",
    clientReference = "clientReference"
  )

  val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)

  val mockTransformationService: AgentDetailsTransformationService = mock[AgentDetailsTransformationService]

  "agent details controller" when {

    ".get" must {

      "return agent details" in {
        val controller = new AgentDetailsTransformationController(identifierAction, cc, LocalDateServiceStub, mockTransformationService)

        when(mockTransformationService.get(any())).thenReturn(Future.successful(Some(agentDetails)))

        val request = FakeRequest("GET", "path")
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.get.apply(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(JSON)
        contentAsJson(result) mustBe Json.toJson(agentDetails)
      }

      "return an empty json object when there is no agent details" in {
        val controller = new AgentDetailsTransformationController(identifierAction, cc, LocalDateServiceStub, mockTransformationService)

        when(mockTransformationService.get(any())).thenReturn(Future.successful(None))

        val request = FakeRequest("GET", "path")
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.get.apply(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(JSON)
        contentAsJson(result) mustBe Json.obj()
      }

    }

    ".save" must {

      "add a transform" in {
        val controller = new AgentDetailsTransformationController(identifierAction, cc, LocalDateServiceStub, mockTransformationService)

        val details = agentDetails

        val request = FakeRequest("POST", "path")
          .withBody(Json.toJson(details))
          .withHeaders(CONTENT_TYPE -> "application/json")

        when(mockTransformationService.addTransform(any(), any())).thenReturn(Future.successful(Success))

        val result = controller.save().apply(request)

        status(result) mustBe OK
      }

      "must return an error for malformed json" in {
        val controller = new AgentDetailsTransformationController(identifierAction, cc, LocalDateServiceStub, mockTransformationService)

        val request = FakeRequest("POST", "path")
          .withBody(Json.toJson("{}"))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.save().apply(request)

        status(result) mustBe BAD_REQUEST
      }

    }

  }

}
