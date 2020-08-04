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

package uk.gov.hmrc.estates.services

import java.time.LocalDate

import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{any, eq => equalTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsSuccess, JsValue, Json}
import uk.gov.hmrc.estates.exceptions.EtmpCacheDataStaleException
import uk.gov.hmrc.estates.models.getEstate.{GetEstateProcessedResponse, ResponseHeader}
import uk.gov.hmrc.estates.models.variation.VariationResponse
import uk.gov.hmrc.estates.models.{DeclarationForApi, DeclarationName, NameType}
import uk.gov.hmrc.estates.repositories.{CacheRepository, VariationsTransformationRepository}
import uk.gov.hmrc.estates.transformers.register.VariationDeclarationTransform
import uk.gov.hmrc.estates.utils.JsonRequests
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class VariationDeclarationServiceSpec extends WordSpec with JsonRequests with MockitoSugar with ScalaFutures with MustMatchers with GuiceOneAppPerSuite {

  private implicit  val hc: HeaderCarrier = new HeaderCarrier
  private val formBundleNo = "001234567890"
  private val utr = "1234567890"
  private val internalId = "InternalId"
  private val fullEtmpResponseJson = getEstateResponse
  private val transformedEtmpResponseJson = Json.parse("""{ "field": "Arbitrary transformed JSON" }""")
  private val estateInfoJson = (fullEtmpResponseJson \ "trustOrEstateDisplay").as[JsValue]
  private val transformedJson = Json.obj("field" -> "value")

  private val declarationName = DeclarationName(NameType("Handy", None, "Andy"))

  private val declaration: DeclarationForApi = DeclarationForApi(declarationName, None, None)

  object LocalDateServiceStub extends LocalDateService {
    override def now: LocalDate = LocalDate.of(1999, 3, 14)
  }

  "Declare no change" should {

    "submit data correctly when the version matches, and then reset the cache" in {

      val desService = mock[DesService]

      val variationsTransformationService = mock[VariationsTransformationService]
      val auditService = app.injector.instanceOf[FakeAuditService]
      val transformer = mock[VariationDeclarationTransform]

      when(variationsTransformationService.populatePersonalRepAddress(any[JsValue]))
        .thenReturn(JsSuccess(estateInfoJson))

      when(variationsTransformationService.applyDeclarationTransformations(any(), any(), any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(JsSuccess(transformedEtmpResponseJson)))

      when(desService.getEstateInfoFormBundleNo(utr))
        .thenReturn(Future.successful(formBundleNo))

      val response = GetEstateProcessedResponse(estateInfoJson, ResponseHeader("Processed", formBundleNo))

      when(desService.getEstateInfo(equalTo(utr), equalTo(internalId))(any[HeaderCarrier]()))
        .thenReturn(Future.successful(response))

      when(transformer.transform(any(),any(),any(),any()))
        .thenReturn(JsSuccess(transformedJson))

      when(desService.estateVariation(any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(VariationResponse("TVN34567890")))

      val OUT = new VariationDeclarationService(desService, variationsTransformationService, transformer, auditService, LocalDateServiceStub)

      val transformedResponse = GetEstateProcessedResponse(transformedEtmpResponseJson, ResponseHeader("Processed", formBundleNo))

      whenReady(OUT.submitDeclaration(utr, internalId, declaration)) { variationResponse => {

        variationResponse mustBe VariationResponse("TVN34567890")

        verify(variationsTransformationService, times( 1))
          .applyDeclarationTransformations(equalTo(utr), equalTo(internalId), equalTo(estateInfoJson))(any[HeaderCarrier])

        verify(transformer, times(1))
          .transform(equalTo(transformedResponse), equalTo(response.getEstate), equalTo(declaration), any())

        val arg: ArgumentCaptor[JsValue] = ArgumentCaptor.forClass(classOf[JsValue])

        verify(desService, times(1)).estateVariation(arg.capture())(any[HeaderCarrier])

        arg.getValue mustBe transformedJson
      }}
    }
  }

  "Fail if the etmp data version doesn't match our submission data" in {
    val desService = mock[DesService]
    val transformationService = mock[VariationsTransformationService]
    val auditService = mock[AuditService]
    val transformer = mock[VariationDeclarationTransform]

    when(desService.getEstateInfoFormBundleNo(utr))
      .thenReturn(Future.successful("31415900000"))

    when(transformationService.applyDeclarationTransformations(any(), any(), any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(JsSuccess(transformedEtmpResponseJson)))

    when(desService.getEstateInfo(equalTo(utr), equalTo(internalId))(any[HeaderCarrier]()))
      .thenReturn(Future.successful(GetEstateProcessedResponse(estateInfoJson, ResponseHeader("Processed", formBundleNo))))

    when(desService.estateVariation(any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(VariationResponse("TVN34567890")))

    when(transformer.transform(any(),any(),any(),any()))
      .thenReturn(JsSuccess(transformedJson))

    val OUT = new VariationDeclarationService(desService, transformationService, transformer, auditService, LocalDateServiceStub)

    whenReady(OUT.submitDeclaration(utr, internalId, declaration).failed) { exception =>

      exception mustBe an[EtmpCacheDataStaleException.type]
      verify(desService, times(0)).estateVariation(any())(any[HeaderCarrier])
    }
  }
}
