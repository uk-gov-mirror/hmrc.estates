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

package uk.gov.hmrc.estates.controllers

import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.estates.BaseSpec
import uk.gov.hmrc.estates.exceptions._
import uk.gov.hmrc.estates.models._
import uk.gov.hmrc.estates.models.register.RegistrationDeclaration
import uk.gov.hmrc.estates.services.register.RegistrationService
import uk.gov.hmrc.estates.utils.JsonRequests

import scala.concurrent.Future

class RegisterEstateControllerSpec extends BaseSpec with GuiceOneServerPerSuite with JsonRequests {

  lazy val mockRegistrationService: RegistrationService = mock[RegistrationService]

  private val estateTrnResponse = "XTRN123456"

  ".submit" should {

    val application = applicationBuilder()
      .overrides(
        bind[RegistrationService].toInstance(mockRegistrationService)
      ).build()

    val controller = application.injector.instanceOf[RegisterEstateController]

    val registrationDeclaration = RegistrationDeclaration(NameType("John", None, "Doe"))

    val request = FakeRequest("POST", "path")
      .withBody(Json.toJson(registrationDeclaration))
      .withHeaders(CONTENT_TYPE -> "application/json")

    "return 200 with TRN" when {

      "valid payload submitted" in {

        when(mockRegistrationService.submit(any())(any()))
          .thenReturn(Future.successful(RegistrationTrnResponse(estateTrnResponse)))

        val result = controller.register().apply(request)

        status(result) mustBe OK

        (contentAsJson(result) \ "trn").as[String] mustBe estateTrnResponse
      }

    }

    "return conflict" when {

      "estate already registered" in {

        when(mockRegistrationService.submit(any())(any()))
          .thenReturn(Future.failed(AlreadyRegisteredException))

        val result = controller.register().apply(request)

        status(result) mustBe CONFLICT
        val output = contentAsJson(result)
        (output \ "code").as[String] mustBe "DUPLICATE_SUBMISSION"
        (output \ "message").as[String] mustBe "Duplicate Correlation Id was submitted."
      }
    }

    "return internal server error" when {

      "error" in {

        when(mockRegistrationService.submit(any())(any()))
          .thenReturn(Future.failed(ServiceNotAvailableException("dependent service is down")))

        val result = controller.register().apply(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
        val output = contentAsJson(result)
        (output \ "code").as[String] mustBe "INTERNAL_SERVER_ERROR"
        (output \ "message").as[String] mustBe "Internal server error."
      }
    }
  }
}
