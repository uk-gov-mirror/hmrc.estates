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

import java.util.UUID

import org.mockito.Matchers.{eq => Meq, _}
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfter, BeforeAndAfterEach}
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.estates.BaseSpec
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.estates.config.AppConfig
import uk.gov.hmrc.estates.controllers.actions.{FakeIdentifierAction, VariationsResponseHandler}
import uk.gov.hmrc.estates.exceptions._
import uk.gov.hmrc.estates.models.variation.VariationResponse
import uk.gov.hmrc.estates.models.{DeclarationForApi, DeclarationName, NameType}
import uk.gov.hmrc.estates.services.{AuditService, DesService, ValidationService, VariationDeclarationService}
import uk.gov.hmrc.estates.utils.Headers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EstateVariationsControllerSpec extends BaseSpec with BeforeAndAfter with BeforeAndAfterEach {

  implicit val cc = stubControllerComponents()
  lazy val mockDesService: DesService = mock[DesService]

  lazy val mockAuditService: AuditService = mock[AuditService]

  val mockAuditConnector: AuditConnector = mock[AuditConnector]
  val mockConfig: AppConfig = mock[AppConfig]

  val auditService = new AuditService(mockAuditConnector, mockConfig)
  val validationService = new ValidationService()

  val mockVariationService = mock[VariationDeclarationService]

  val responseHandler = new VariationsResponseHandler(mockAuditService)

  override def beforeEach() = {
    reset(mockDesService, mockAuditService, mockAuditConnector, mockConfig)
  }


  private def estateVariationsController = {
    val SUT = new EstateVariationsController(
      new FakeIdentifierAction(cc.parsers.default, Organisation),
      mockAuditService,
      mockVariationService,
      responseHandler)
    SUT
  }

  val tvnResponse = "XXTVN1234567890"
  val estateVariationsAuditEvent = "EstateVariation"
  val utr = "1234567890"

  ".estateVariation" should {

    "not perform auditing" when {
      "the feature toggle is set to false" in {

        when(mockVariationService.submitDeclaration(any(), any(), any())(any()))
          .thenReturn(Future.successful(VariationResponse(tvnResponse)))

        when(mockConfig.auditingEnabled).thenReturn(false)

        val requestPayLoad = Json.parse(validEstateVariationsRequestJson)

        val SUT = new EstateVariationsController(new FakeIdentifierAction(cc.parsers.default, Organisation), mockAuditService, mockVariationService, responseHandler)

        val result = SUT.declare(utr)(
          postRequestWithPayload(requestPayLoad, withDraftId = false)
            .withHeaders(Headers.CORRELATION_HEADER -> UUID.randomUUID().toString)
        )

        whenReady(result) { _ =>

          verify(mockAuditConnector, times(0)).sendExplicitAudit[Any](any(), any())(any(), any(), any())
        }
      }
    }

    "perform auditing" when {

      "the feature toggle is set to true" in {

        when(mockVariationService.submitDeclaration(any(), any(), any())(any()))
          .thenReturn(Future.successful(VariationResponse(tvnResponse)))

        when(mockConfig.auditingEnabled).thenReturn(true)

        val requestPayLoad = Json.parse(validEstateVariationsRequestJson)

        val SUT = new EstateVariationsController(new FakeIdentifierAction(cc.parsers.default, Organisation), auditService, mockVariationService, responseHandler)

        val result = SUT.declare(utr)(
          postRequestWithPayload(requestPayLoad, withDraftId = false)
            .withHeaders(Headers.CORRELATION_HEADER -> UUID.randomUUID().toString)
        )

        whenReady(result) { _ =>
          verify(mockAuditConnector, times(1)).sendExplicitAudit[Any](any(), any())(any(), any(), any())
        }
      }
    }

    "return 200 with TVN" when {

      "individual user called the register endpoint with a valid json payload " in {

        when(mockVariationService.submitDeclaration(any(), any(), any())(any()))
          .thenReturn(Future.successful(VariationResponse(tvnResponse)))

        val requestPayLoad = Json.parse(validEstateVariationsRequestJson)

        val SUT = estateVariationsController

        val result = SUT.declare(utr)(
          postRequestWithPayload(requestPayLoad, withDraftId = false)
            .withHeaders(Headers.CORRELATION_HEADER -> UUID.randomUUID().toString)
        )

        status(result) mustBe OK

        verify(mockAuditService).audit(
          Meq(estateVariationsAuditEvent),
          any(),
          Meq("id"),
          Meq(Json.obj("tvn" -> tvnResponse))
        )(any())

        (contentAsJson(result) \ "tvn").as[String] mustBe tvnResponse

      }

    }

    "return a BadRequest" when {

      "invalid correlation id is provided in the headers" in {

        when(mockVariationService.submitDeclaration(any(), any(), any())(any()))
          .thenReturn(Future.failed(InvalidCorrelationIdException))

        val SUT = estateVariationsController

        val request = postRequestWithPayload(Json.parse(validEstateVariationsRequestJson), withDraftId = false)

        val result = SUT.declare(utr)(request)

        status(result) mustBe INTERNAL_SERVER_ERROR

        verify(mockAuditService).auditErrorResponse(
          Meq(estateVariationsAuditEvent),
          any(),
          Meq("id"),
          Meq("Submission has not passed validation. Invalid CorrelationId.")
        )(any())

        val output = contentAsJson(result)

        (output \ "code").as[String] mustBe "INVALID_CORRELATIONID"
        (output \ "message").as[String] mustBe "Submission has not passed validation. Invalid CorrelationId."

      }

    }

    "return a Conflict" when {
      "submission with same correlation id is submitted." in {

        when(mockVariationService.submitDeclaration(any(), any(), any())(any()))
          .thenReturn(Future.failed(DuplicateSubmissionException))

        when(mockConfig.variationsApiSchema).thenReturn(appConfig.variationsApiSchema)

        val SUT = estateVariationsController

        val result = SUT.declare(utr)(
          postRequestWithPayload(Json.parse(validEstateVariationsRequestJson), withDraftId = false)
            .withHeaders(Headers.CORRELATION_HEADER -> UUID.randomUUID().toString)
        )

        status(result) mustBe CONFLICT

        verify(mockAuditService).auditErrorResponse(
          Meq(estateVariationsAuditEvent),
          any(),
          Meq("id"),
          Meq("Duplicate Correlation Id was submitted.")
        )(any())

        val output = contentAsJson(result)

        (output \ "code").as[String] mustBe "DUPLICATE_SUBMISSION"
        (output \ "message").as[String] mustBe "Duplicate Correlation Id was submitted."

      }
    }

    "return an internal server error" when {

      "the register endpoint called and something goes wrong." in {

        when(mockVariationService.submitDeclaration(any(), any(), any())(any()))
          .thenReturn(Future.failed(InternalServerErrorException("some error")))

        when(mockConfig.variationsApiSchema).thenReturn(appConfig.variationsApiSchema)

        val SUT = estateVariationsController

        val result = SUT.declare(utr)(
          postRequestWithPayload(Json.parse(validEstateVariationsRequestJson))
            .withHeaders(Headers.CORRELATION_HEADER -> UUID.randomUUID().toString)
        )

        status(result) mustBe INTERNAL_SERVER_ERROR

        verify(mockAuditService).auditErrorResponse(
          Meq(estateVariationsAuditEvent),
          any(),
          Meq("id"),
          any()
        )(any())

        val output = contentAsJson(result)

        (output \ "code").as[String] mustBe "INTERNAL_SERVER_ERROR"
        (output \ "message").as[String] mustBe "Internal server error."

      }

    }

    "Return bad request when declaring No change and there is a form bundle number mismatch" in {
      val SUT = estateVariationsController
      val declaration = DeclarationName(
        NameType("firstname", None, "Surname")
      )

      val declarationForApi = DeclarationForApi(declaration, None, None)

      when(mockVariationService.submitDeclaration(any(), any(), any())(any()))
        .thenReturn(Future.failed(EtmpCacheDataStaleException))

      val result = SUT.declare("1234567890")(
        FakeRequest("POST", "/no-change/1234567890").withBody(Json.toJson(declarationForApi))
      )

      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.obj(
        "code" -> "ETMP_DATA_STALE",
        "message" -> "ETMP returned a changed form bundle number for the estate."
      )

      verify(mockAuditService).auditErrorResponse(
        Meq(estateVariationsAuditEvent),
        any(),
        Meq("id"),
        Meq("Cached ETMP data stale.")
      )(any())
    }

    "return service unavailable" when {
      "the des returns Service Unavailable as dependent service is down. " in {

        when(mockVariationService.submitDeclaration(any(), any(), any())(any()))
          .thenReturn(Future.failed(ServiceNotAvailableException("dependent service is down")))

        when(mockConfig.variationsApiSchema).thenReturn(appConfig.variationsApiSchema)

        val SUT = estateVariationsController

        val result = SUT.declare(utr)(
          postRequestWithPayload(Json.parse(validEstateVariationsRequestJson))
            .withHeaders(Headers.CORRELATION_HEADER -> UUID.randomUUID().toString)
        )

        status(result) mustBe SERVICE_UNAVAILABLE

        verify(mockAuditService).auditErrorResponse(
          Meq(estateVariationsAuditEvent),
          any(),
          Meq("id"),
          Meq("Service unavailable.")
        )(any())

        val output = contentAsJson(result)

        (output \ "code").as[String] mustBe "SERVICE_UNAVAILABLE"
        (output \ "message").as[String] mustBe "Service unavailable."

      }
    }
  }
}

