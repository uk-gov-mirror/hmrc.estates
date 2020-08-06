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
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.estates.BaseSpec
import uk.gov.hmrc.estates.config.AppConfig
import uk.gov.hmrc.estates.controllers.actions.{FakeIdentifierAction, VariationsResponseHandler}
import uk.gov.hmrc.estates.exceptions._
import uk.gov.hmrc.estates.models.variation.{VariationFailureResponse, VariationSuccessResponse}
import uk.gov.hmrc.estates.models.{DeclarationForApi, DeclarationName, NameType}
import uk.gov.hmrc.estates.services.maintain.VariationService
import uk.gov.hmrc.estates.services.{AuditService, DesService}
import uk.gov.hmrc.estates.utils.Headers
import uk.gov.hmrc.estates.utils.VariationErrorResponses._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EstateVariationsControllerSpec extends BaseSpec with BeforeAndAfter with BeforeAndAfterEach {

  private implicit val cc: ControllerComponents = stubControllerComponents()
  private val mockDesService: DesService = mock[DesService]

  private val mockAuditService: AuditService = mock[AuditService]

  private val mockConfig: AppConfig = mock[AppConfig]

  private val mockVariationService = mock[VariationService]

  private val responseHandler = new VariationsResponseHandler(mockAuditService)

  override def beforeEach(): Unit = {
    reset(mockDesService, mockConfig)
  }

  private def estateVariationsController = {
    val SUT = new EstateVariationsController(
      new FakeIdentifierAction(cc.parsers.default, Organisation),
      mockVariationService,
      responseHandler)
    SUT
  }

  val tvnResponse = "XXTVN1234567890"
  val utr = "1234567890"

  ".estateVariation" should {

    "return 200 with TVN" when {

      "individual user called the register endpoint with a valid json payload " in {

        when(mockVariationService.submitDeclaration(any(), any(), any())(any()))
          .thenReturn(Future.successful(VariationSuccessResponse(tvnResponse)))

        val requestPayLoad = Json.parse(validEstateVariationsRequestJson)

        val SUT = estateVariationsController

        val result = SUT.declare(utr)(
          postRequestWithPayload(requestPayLoad, withDraftId = false)
            .withHeaders(Headers.CORRELATION_HEADER -> UUID.randomUUID().toString)
        )

        status(result) mustBe OK

        (contentAsJson(result) \ "tvn").as[String] mustBe tvnResponse
      }
    }

    "return a BadRequest" when {

      "invalid correlation id is provided in the headers" in {

        when(mockVariationService.submitDeclaration(any(), any(), any())(any()))
          .thenReturn(Future.successful(VariationFailureResponse(InvalidCorrelationIdErrorResponse)))

        val SUT = estateVariationsController

        val request = postRequestWithPayload(Json.parse(validEstateVariationsRequestJson), withDraftId = false)

        val result = SUT.declare(utr)(request)

        status(result) mustBe INTERNAL_SERVER_ERROR

        val output = contentAsJson(result)

        (output \ "code").as[String] mustBe "INVALID_CORRELATIONID"
        (output \ "message").as[String] mustBe "Submission has not passed validation. Invalid CorrelationId."

      }

    }

    "return a Conflict" when {
      "submission with same correlation id is submitted." in {

        when(mockVariationService.submitDeclaration(any(), any(), any())(any()))
          .thenReturn(Future.successful(VariationFailureResponse(DuplicateSubmissionErrorResponse)))

        when(mockConfig.variationsApiSchema).thenReturn(appConfig.variationsApiSchema)

        val SUT = estateVariationsController

        val result = SUT.declare(utr)(
          postRequestWithPayload(Json.parse(validEstateVariationsRequestJson), withDraftId = false)
            .withHeaders(Headers.CORRELATION_HEADER -> UUID.randomUUID().toString)
        )

        status(result) mustBe CONFLICT

        val output = contentAsJson(result)

        (output \ "code").as[String] mustBe "DUPLICATE_SUBMISSION"
        (output \ "message").as[String] mustBe "Duplicate Correlation Id was submitted."

      }
    }

    "return an internal server error" when {

      "the register endpoint called and something goes wrong." in {

        when(mockVariationService.submitDeclaration(any(), any(), any())(any()))
          .thenReturn(Future.successful(VariationFailureResponse(InternalServerErrorErrorResponse)))

        when(mockConfig.variationsApiSchema).thenReturn(appConfig.variationsApiSchema)

        val SUT = estateVariationsController

        val result = SUT.declare(utr)(
          postRequestWithPayload(Json.parse(validEstateVariationsRequestJson))
            .withHeaders(Headers.CORRELATION_HEADER -> UUID.randomUUID().toString)
        )

        status(result) mustBe INTERNAL_SERVER_ERROR

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

//      verify(mockAuditService).auditErrorResponse(
//        Meq(estateVariationsAuditEvent),
//        any(),
//        Meq("id"),
//        Meq("Cached ETMP data stale.")
//      )(any())
    }

    "return service unavailable" when {
      "the des returns Service Unavailable as dependent service is down. " in {

        when(mockVariationService.submitDeclaration(any(), any(), any())(any()))
          .thenReturn(Future.successful(VariationFailureResponse(ServiceUnavailableErrorResponse)))

        when(mockConfig.variationsApiSchema).thenReturn(appConfig.variationsApiSchema)

        val SUT = estateVariationsController

        val result = SUT.declare(utr)(
          postRequestWithPayload(Json.parse(validEstateVariationsRequestJson))
            .withHeaders(Headers.CORRELATION_HEADER -> UUID.randomUUID().toString)
        )

        status(result) mustBe SERVICE_UNAVAILABLE

        val output = contentAsJson(result)

        (output \ "code").as[String] mustBe "SERVICE_UNAVAILABLE"
        (output \ "message").as[String] mustBe "Service unavailable."

      }
    }
  }
}

