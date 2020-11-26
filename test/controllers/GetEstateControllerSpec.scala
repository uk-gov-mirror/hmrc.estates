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

import base.BaseSpec
import org.mockito.Matchers.{any, eq => mockEq}
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterEach}
import play.api.inject.bind
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import config.AppConfig
import models.getEstate._
import services.{AuditService, DesService, VariationsTransformationService}
import utils.JsonRequests
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future

class GetEstateControllerSpec extends BaseSpec with BeforeAndAfter with JsonRequests with BeforeAndAfterEach {

  val mockDesService: DesService = mock[DesService]
  val mockAuditService: AuditService = mock[AuditService]

  val mockAuditConnector = mock[AuditConnector]
  val mockConfig = mock[AppConfig]

  val auditService = new AuditService(mockAuditConnector, mockConfig)

  override def afterEach() =  {
    reset(mockAuditService, mockAuditConnector, mockConfig)
  }

  val invalidUTR = "1234567"
  val utr = "1234567890"

  ".get" should {

    "return 200" when {

      "estate is processed" in {
        val application = applicationBuilder().overrides(
          bind[DesService].toInstance(mockDesService),
          bind[AuditService].toInstance(mockAuditService)
        ).build()

        val expectedJson = getJsonValueFromFile("mdtp/playback/valid-estate-playback-01.json")

        val etmpJson = (getJsonValueFromFile("etmp/playback/valid-estate-playback-01.json") \ "trustOrEstateDisplay").get

        when(mockDesService.getEstateInfo(any(), any())(any())).thenReturn(Future.successful(GetEstateProcessedResponse(etmpJson, ResponseHeader("Processed", "1"))))

        val controller = application.injector.instanceOf[GetEstateController]

        val result = controller.get(utr, false).apply(FakeRequest(GET, s"/estates/$utr"))

        application.stop()

        whenReady(result) { _ =>
          contentAsJson(result) mustBe expectedJson
          verify(mockAuditService).auditGetVariationSuccess(mockEq(utr), any())(any(), any())
          status(result) mustBe OK
          contentType(result) mustBe Some(JSON)
        }
      }

      "estate is not processed" in {

        val application = applicationBuilder().overrides(
          bind[DesService].toInstance(mockDesService),
          bind[AuditService].toInstance(mockAuditService)
        ).build()

        when(mockDesService.getEstateInfo(any(), any())(any())).thenReturn(Future.successful(GetEstateStatusResponse(ResponseHeader("Parked", "1"))))

        val controller = application.injector.instanceOf[GetEstateController]

        val result = controller.get(utr, false).apply(FakeRequest(GET, s"/estates/$utr"))

        application.stop()

        whenReady(result) { _ =>
          verify(mockAuditService).auditGetVariationFailed(
            mockEq(utr),
            any[JsValue])(any(), any())
          status(result) mustBe OK
          contentType(result) mustBe Some(JSON)
        }
      }
    }

    "return 400 - BadRequest" when {
      "the UTR given is invalid" in {

        val application = applicationBuilder().overrides(
          bind[DesService].toInstance(mockDesService),
          bind[AuditService].toInstance(mockAuditService)
        ).build()

        val controller = application.injector.instanceOf[GetEstateController]

        val result = controller.get(invalidUTR, false).apply(FakeRequest(GET, s"/estates/$invalidUTR"))

        whenReady(result) { _ =>
          status(result) mustBe BAD_REQUEST
        }
      }
    }

    "return 500 - InternalServerError" when {
      "the get endpoint returns a InvalidUTRResponse" in {

        val application = applicationBuilder().overrides(
          bind[DesService].toInstance(mockDesService),
          bind[AuditService].toInstance(mockAuditService)
        ).build()

        when(mockDesService.getEstateInfo(any(), any())(any())).thenReturn(Future.successful(InvalidUTRResponse))

        val controller = application.injector.instanceOf[GetEstateController]

        val utr = "1234567890"
        val result = controller.get(utr, false).apply(FakeRequest(GET, s"/estates/$invalidUTR"))

        whenReady(result) { _ =>
          verify(mockAuditService).auditGetVariationFailed(
            mockEq(utr),
            any[JsValue])(any(), any())
          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }

      "the get endpoint returns an InvalidRegimeResponse" in {

        val application = applicationBuilder().overrides(
          bind[DesService].toInstance(mockDesService),
          bind[AuditService].toInstance(mockAuditService)
        ).build()

        when(mockDesService.getEstateInfo(any(), any())(any())).thenReturn(Future.successful(InvalidRegimeResponse))

        val controller = application.injector.instanceOf[GetEstateController]

        val utr = "1234567890"
        val result = controller.get(utr, false).apply(FakeRequest(GET, s"/estates/$utr"))

        whenReady(result) { _ =>
          verify(mockAuditService).auditGetVariationFailed(
            mockEq(utr),
            any[JsValue])(any(), any())
          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }

      "the get endpoint returns a BadRequestResponse" in {

        val application = applicationBuilder().overrides(
          bind[DesService].toInstance(mockDesService),
          bind[AuditService].toInstance(mockAuditService)
        ).build()

        when(mockDesService.getEstateInfo(any(), any())(any())).thenReturn(Future.successful(BadRequestResponse))

        val controller = application.injector.instanceOf[GetEstateController]

        val utr = "1234567890"
        val result = controller.get(utr, false).apply(FakeRequest(GET, s"/estates/$utr"))

        whenReady(result) { _ =>
          verify(mockAuditService).auditGetVariationFailed(
            mockEq(utr),
            any[JsValue])(any(), any())
          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }

      "the get endpoint returns a ResourceNotFoundResponse" in {

        val application = applicationBuilder().overrides(
          bind[DesService].toInstance(mockDesService),
          bind[AuditService].toInstance(mockAuditService)
        ).build()

        when(mockDesService.getEstateInfo(any(), any())(any())).thenReturn(Future.successful(ResourceNotFoundResponse))

        val controller = application.injector.instanceOf[GetEstateController]

        val utr = "1234567890"
        val result = controller.get(utr, false).apply(FakeRequest(GET, s"/estates/$utr"))

        whenReady(result) { _ =>
          verify(mockAuditService).auditGetVariationFailed(
            mockEq(utr),
            any[JsValue])(any(), any())
          status(result) mustBe NOT_FOUND
        }
      }

      "the get endpoint returns an InternalServerErrorResponse" in {

        val application = applicationBuilder().overrides(
          bind[DesService].toInstance(mockDesService),
          bind[AuditService].toInstance(mockAuditService)
        ).build()

        when(mockDesService.getEstateInfo(any(), any())(any())).thenReturn(Future.successful(InternalServerErrorResponse))

        val controller = application.injector.instanceOf[GetEstateController]

        val utr = "1234567890"
        val result = controller.get(utr, false).apply(FakeRequest(GET, s"/estates/$utr"))

        whenReady(result) { _ =>
          verify(mockAuditService).auditGetVariationFailed(
            mockEq(utr),
            any[JsValue])(any(), any())
          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }

      "the get endpoint returns a ServiceUnavailableResponse" in {

        val application = applicationBuilder().overrides(
          bind[DesService].toInstance(mockDesService),
          bind[AuditService].toInstance(mockAuditService)
        ).build()

        when(mockDesService.getEstateInfo(any(), any())(any())).thenReturn(Future.successful(ServiceUnavailableResponse))

        val controller = application.injector.instanceOf[GetEstateController]

        val utr = "1234567890"
        val result = controller.get(utr, false).apply(FakeRequest(GET, s"/estates/$utr"))

        whenReady(result) { _ =>
          verify(mockAuditService).auditGetVariationFailed(
            mockEq(utr),
            any[JsValue])(any(), any())
          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }
    }
  }

  ".getPersonalRepresentative" should {

    "return 403 - Forbidden with parked content" in {
      val variationsTransformationService = mock[VariationsTransformationService]

      val application = applicationBuilder().overrides(
        bind[VariationsTransformationService].toInstance(variationsTransformationService),
        bind[AuditService].toInstance(mockAuditService)
      ).build()

      when(variationsTransformationService.getTransformedData(any(), any())(any()))
        .thenReturn(Future.successful(GetEstateStatusResponse(ResponseHeader("Parked", "1"))))

      val controller = application.injector.instanceOf[GetEstateController]
      val result = controller.getPersonalRepresentative(utr).apply(FakeRequest(GET, s"/estates/$utr/transformed/personal-representative"))

      whenReady(result) { _ =>
        status(result) mustBe FORBIDDEN
      }
    }
    "return 200 - Ok with processed content" in {
      val variationsTransformationService = mock[VariationsTransformationService]

      val application = applicationBuilder().overrides(
        bind[VariationsTransformationService].toInstance(variationsTransformationService),
        bind[AuditService].toInstance(mockAuditService)
      ).build()

      val processedResponse = GetEstateProcessedResponse(getTransformedEstateResponse, ResponseHeader("Processed", "1"))

      when(variationsTransformationService.getTransformedData(any[String], any[String])(any[HeaderCarrier])).thenReturn(Future.successful(processedResponse))

      val controller = application.injector.instanceOf[GetEstateController]

      val result = controller.getPersonalRepresentative(utr).apply(FakeRequest(GET, s"/estates/$utr/transformed/personal-representative"))

      whenReady(result) { _ =>
        verify(mockAuditService).auditGetVariationSuccess(mockEq(utr), any())(any(), any())
        verify(variationsTransformationService).getTransformedData(mockEq(utr), mockEq("id"))(any[HeaderCarrier])
        status(result) mustBe OK
        contentType(result) mustBe Some(JSON)
        contentAsJson(result) mustBe getTransformedPersonalRepResponse
      }
    }
  }

  ".getEstateDetails" should {

    val route: String = s"/estates/$utr/date-of-death"

    "return 200 - Ok with processed content" in {

      val application = applicationBuilder().overrides(
        bind[DesService].toInstance(mockDesService),
        bind[AuditService].toInstance(mockAuditService)
      ).build()

      val expectedJson = Json.toJson("2016-04-06")

      val etmpJson = (getJsonValueFromFile("etmp/playback/valid-estate-playback-01.json") \ "trustOrEstateDisplay").get

      when(mockDesService.getEstateInfo(any(), any())(any())).thenReturn(Future.successful(GetEstateProcessedResponse(etmpJson, ResponseHeader("Processed", "1"))))

      val controller = application.injector.instanceOf[GetEstateController]

      val result = controller.getDateOfDeath(utr).apply(FakeRequest(GET, route))

      application.stop()

      whenReady(result) { _ =>
        contentAsJson(result) mustBe expectedJson
        verify(mockAuditService).auditGetVariationSuccess(mockEq(utr), any())(any(), any())
        status(result) mustBe OK
        contentType(result) mustBe Some(JSON)
      }
    }

    "return 500 - Internal server error for invalid content" in {

      val application = applicationBuilder().overrides(
        bind[DesService].toInstance(mockDesService),
        bind[AuditService].toInstance(mockAuditService)
      ).build()

      when(mockDesService.getEstateInfo(any(), any())(any())).thenReturn(Future.successful(GetEstateStatusResponse(ResponseHeader("Parked", "1"))))

      val controller = application.injector.instanceOf[GetEstateController]

      val result = controller.getDateOfDeath(utr).apply(FakeRequest(GET, route))

      application.stop()

      whenReady(result) { _ =>
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }
}