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

package controllers

import java.time.LocalDate

import base.BaseSpec
import org.mockito.Matchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import exceptions._
import models._
import models.register.{RegistrationDeclaration, TaxAmount}
import org.scalatest.BeforeAndAfter
import services.{EstatesStoreService, RosmPatternService}
import services.register.RegistrationService
import utils.JsonRequests
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class RegisterEstateControllerSpec extends BaseSpec with GuiceOneServerPerSuite with JsonRequests {

  lazy val mockRegistrationService: RegistrationService = mock[RegistrationService]
  lazy val mockRosmPatternService: RosmPatternService = mock[RosmPatternService]
  lazy val mockEstatesStoreService: EstatesStoreService = mock[EstatesStoreService]

  private val estateTrnResponse = "XTRN123456"

  before {
    reset(mockRosmPatternService, mockRegistrationService, mockEstatesStoreService)
  }

  ".submit" should {

    val application = applicationBuilder()
      .overrides(
        bind[RegistrationService].toInstance(mockRegistrationService),
        bind[RosmPatternService].toInstance(mockRosmPatternService)
      )
      .configure()
      .build()

    val controller = application.injector.instanceOf[RegisterEstateController]

    val registrationDeclaration = RegistrationDeclaration(NameType("John", None, "Doe"))

    val request = FakeRequest("POST", "path")
      .withBody(Json.toJson(registrationDeclaration))
      .withHeaders(CONTENT_TYPE -> "application/json")

    "return 200 with TRN" when {

      "valid payload submitted" when {
        "4mld" in {

          when(mockRegistrationService.submit(any())(any(), any()))
            .thenReturn(Future.successful(RegistrationTrnResponse(estateTrnResponse)))

          when(mockRosmPatternService.enrol(any(), any(), any())(any())).thenReturn(Future.successful(TaxEnrolmentSuccess))

          val result = controller.register().apply(request)

          status(result) mustBe OK

          (contentAsJson(result) \ "trn").as[String] mustBe estateTrnResponse

          verify(mockRosmPatternService, times(1)).enrol(any(), any(), any())(any[HeaderCarrier])
        }

        "5mld" in {

          when(mockEstatesStoreService.is5mldEnabled()(any(), any()))
            .thenReturn(Future.successful(true))

          when(mockRegistrationService.submit(any())(any(), any()))
            .thenReturn(Future.successful(RegistrationTrnResponse(estateTrnResponse)))

          when(mockRosmPatternService.enrol(any(), any(), any())(any()))
            .thenReturn(Future.successful(TaxEnrolmentSuccess))

          val result = controller.register().apply(request)

          status(result) mustBe OK

          (contentAsJson(result) \ "trn").as[String] mustBe estateTrnResponse

          verify(mockRosmPatternService, times(1)).enrol(any(), any(), any())(any[HeaderCarrier])
        }
      }

    }

    "return conflict" when {

      "estate already registered" in {

        when(mockRegistrationService.submit(any())(any(), any()))
          .thenReturn(Future.successful(AlreadyRegisteredResponse))

        val result = controller.register().apply(request)

        status(result) mustBe CONFLICT
        val output = contentAsJson(result)
        (output \ "code").as[String] mustBe "DUPLICATE_SUBMISSION"
        (output \ "message").as[String] mustBe "Duplicate Correlation Id was submitted."
      }
    }

    "return internal server error" when {

      "error" in {

        when(mockRegistrationService.submit(any())(any(), any()))
          .thenReturn(Future.failed(ServiceNotAvailableException("dependent service is down")))

        val result = controller.register().apply(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
        val output = contentAsJson(result)
        (output \ "code").as[String] mustBe "INTERNAL_SERVER_ERROR"
        (output \ "message").as[String] mustBe "Internal server error."
      }
    }
  }

  ".get" should {

    val application = applicationBuilder()
      .overrides(
        bind[RegistrationService].toInstance(mockRegistrationService)
      ).build()

    val controller = application.injector.instanceOf[RegisterEstateController]

    val request = FakeRequest("GET", "path")
      .withHeaders(CONTENT_TYPE -> "application/json")

    val deceased: EstateWillType = EstateWillType(
      name = NameType("Mr TRS Reference 31", None, "TaxPayer 31"),
      dateOfBirth = None,
      dateOfDeath = LocalDate.parse("2013-04-07"),
      identification = Some(IdentificationType(Some("MT939555B"), None, None))
    )

    val personalRepInd: EstatePerRepIndType = EstatePerRepIndType(
      name =  NameType("Alister", None, "Mc'Lovern"),
      dateOfBirth = LocalDate.parse("1955-09-08"),
      identification = IdentificationType(
        Some("JS123456A"),
        None,
        Some(AddressType("AEstateAddress1", "AEstateAddress2", Some("AEstateAddress3"), Some("AEstateAddress4"), Some("TF3 4ER"), "GB"))
      ),
      phoneNumber = "078888888",
      email = Some("test@abc.com")
    )

    val estateName: String = "Estate of Mr A Deceased"

    val taxAmount: TaxAmount = TaxAmount.AmountMoreThanTwoHalfMillion

    val registration = EstateRegistrationNoDeclaration(
      None,
      CorrespondenceName(estateName),
      None,
      Estate(
        EntitiesType(
          PersonalRepresentativeType(
            Some(personalRepInd),
            None
          ),
          deceased
        ),
        None,
        taxAmount.toString
      ),
      None
    )

    "return registration" when {

      "document successfully built from transforms" in {
        when(mockRegistrationService.getRegistration()(any(), any()))
          .thenReturn(Future.successful(registration))

        val result = controller.get().apply(request)

        status(result) mustBe OK

        contentAsJson(result) mustBe Json.toJson(registration)
      }
    }

    "return internal server error" when {

      "there is an error" in {
        when(mockRegistrationService.getRegistration()(any(), any()))
          .thenReturn(Future.failed(new RuntimeException("Unable to parse transformed json as EstateRegistration")))

        val result = controller.get().apply(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
        val output = contentAsJson(result)
        (output \ "code").as[String] mustBe "INTERNAL_SERVER_ERROR"
        (output \ "message").as[String] mustBe "Internal server error."
      }

    }
  }
}
