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

package services.maintain

import connectors.BaseConnectorSpec
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{any, eq => equalTo}
import org.mockito.Mockito.{reset, times, verify, when}
import play.api.libs.json.{JsSuccess, JsValue, Json}
import models.getEstate.{GetEstateProcessedResponse, ResponseHeader}
import models.variation.{VariationFailureResponse, VariationResponse, VariationSuccessResponse}
import models.{DeclarationForApi, DeclarationName, NameType}
import services._
import utils.ErrorResponses.{DuplicateSubmissionErrorResponse, EtmpDataStaleErrorResponse}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import scala.concurrent.Future

class VariationServiceSpec extends BaseConnectorSpec {

  private val formBundleNo = "001234567890"
  private val utr = "1234567890"
  private val internalId = "InternalId"
  private val fullEtmpResponseJson = get4MLDEstateResponse
  private val transformedEtmpResponseJson = Json.parse("""{ "field": "Arbitrary transformed JSON" }""")
  private val estateInfoJson = (fullEtmpResponseJson \ "trustOrEstateDisplay").as[JsValue]
  private val transformedJson = Json.obj("field" -> "value")
  private val transformedJsonWithSubmission = Json.obj("field" -> "value", "submissionDate" -> LocalDate.now())
  private val declarationName = DeclarationName(NameType("Handy", None, "Andy"))
  private val declaration: DeclarationForApi = DeclarationForApi(declarationName, None)

  val desService = mock[DesService]
  val variationsTransformationService = mock[VariationsTransformationService]
  val auditService = mock[AuditService]
  val transformer = mock[VariationDeclarationService]
  val estatesStoreService = mock[EstatesStoreService]

  val estates5MLDService = new Estates5MLDService(estatesStoreService)

  before {
    reset(desService, variationsTransformationService, auditService, transformer, estatesStoreService)
  }

  def service = new VariationService(
    desService,
    variationsTransformationService,
    transformer,
    estates5MLDService,
    auditService
  )

  "submitDeclaration" should {

    "submit data correctly when the version matches, and then reset the cache" when {
      "4mld" in {

        val successfulResponse = VariationSuccessResponse("TVN34567890")

        val response = setupForTest(successfulResponse)

        val responseHeader = ResponseHeader("Processed", formBundleNo)

        whenReady(service.submitDeclaration(utr, internalId, declaration)) { variationResponse => {

          variationResponse mustBe successfulResponse

          verify(variationsTransformationService, times( 1))
            .applyDeclarationTransformations(equalTo(utr), equalTo(internalId), equalTo(estateInfoJson))(any[HeaderCarrier])

          verify(transformer, times(1))
            .transform(equalTo(transformedEtmpResponseJson), equalTo(responseHeader), equalTo(response.getEstate), equalTo(declaration))

          verify(auditService).auditVariationSubmitted(
            equalTo(internalId),
            equalTo(transformedJson),
            equalTo(successfulResponse))(any())

          val arg: ArgumentCaptor[JsValue] = ArgumentCaptor.forClass(classOf[JsValue])

          verify(desService, times(1)).estateVariation(arg.capture())

          arg.getValue mustBe transformedJson
        }}
      }

      "5mld" in {

        val successfulResponse = VariationSuccessResponse("TVN34567890")

        val response = setupForTest(successfulResponse)

        val responseHeader = ResponseHeader("Processed", formBundleNo)

        when(estatesStoreService.isFeatureEnabled(equalTo("5mld"))(any(), any()))
          .thenReturn(Future.successful(true))

        whenReady(service.submitDeclaration(utr, internalId, declaration)) { variationResponse => {

          variationResponse mustBe successfulResponse

          verify(variationsTransformationService, times( 1))
            .applyDeclarationTransformations(equalTo(utr), equalTo(internalId), equalTo(estateInfoJson))(any[HeaderCarrier])

          verify(transformer, times(1))
            .transform(equalTo(transformedEtmpResponseJson), equalTo(responseHeader), equalTo(response.getEstate), equalTo(declaration))

          verify(auditService).auditVariationSubmitted(
            equalTo(internalId),
            equalTo(transformedJsonWithSubmission),
            equalTo(successfulResponse))(any())

          val arg: ArgumentCaptor[JsValue] = ArgumentCaptor.forClass(classOf[JsValue])

          verify(desService, times(1)).estateVariation(arg.capture())

          arg.getValue mustBe transformedJsonWithSubmission
        }}
      }
    }
    "audit error when submission fails" in {

      val failedResponse = VariationFailureResponse(DuplicateSubmissionErrorResponse)

      setupForTest(failedResponse)

      whenReady(service.submitDeclaration(utr, internalId, declaration)) { variationResponse => {

        variationResponse mustBe failedResponse

        verify(auditService).auditVariationFailed(
          equalTo(internalId),
          equalTo(transformedJson),
          equalTo(failedResponse))(any())
      }}
    }
  }

  private def setupForTest(variationResponse: VariationResponse) = {

    when(estatesStoreService.isFeatureEnabled(equalTo("5mld"))(any(), any()))
      .thenReturn(Future.successful(false))

    when(variationsTransformationService.populatePersonalRepAddress(any[JsValue]))
      .thenReturn(JsSuccess(estateInfoJson))

    when(variationsTransformationService.applyDeclarationTransformations(any(), any(), any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(JsSuccess(transformedEtmpResponseJson)))

    when(desService.getEstateInfoFormBundleNo(utr))
      .thenReturn(Future.successful(formBundleNo))

    val response: GetEstateProcessedResponse = GetEstateProcessedResponse(estateInfoJson, ResponseHeader("Processed", formBundleNo))

    when(desService.getEstateInfo(equalTo(utr), equalTo(internalId))(any[HeaderCarrier]()))
      .thenReturn(Future.successful(response))

    when(transformer.transform(any(), any(), any(), any()))
      .thenReturn(JsSuccess(transformedJson))

    when(desService.estateVariation(any()))
      .thenReturn(Future.successful(variationResponse))

    response
  }

  "Fail if the etmp data version doesn't match our submission data" in {

    when(desService.getEstateInfoFormBundleNo(utr))
      .thenReturn(Future.successful("31415900000"))

    when(desService.getEstateInfo(equalTo(utr), equalTo(internalId))(any[HeaderCarrier]()))
      .thenReturn(Future.successful(GetEstateProcessedResponse(estateInfoJson, ResponseHeader("Processed", formBundleNo))))

    whenReady(service.submitDeclaration(utr, internalId, declaration)) { response =>
      response mustBe VariationFailureResponse(EtmpDataStaleErrorResponse)
      verify(desService, times(0)).estateVariation(any())
    }
  }
}
