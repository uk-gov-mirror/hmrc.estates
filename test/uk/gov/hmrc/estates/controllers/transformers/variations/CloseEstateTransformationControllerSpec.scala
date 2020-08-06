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

package uk.gov.hmrc.estates.controllers.transformers.variations

import java.time.LocalDate

import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FreeSpec, MustMatchers}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{CONTENT_TYPE, _}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.estates.controllers.actions.FakeIdentifierAction
import uk.gov.hmrc.estates.models.Success
import uk.gov.hmrc.estates.services.maintain.CloseEstateTransformationService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CloseEstateTransformationControllerSpec extends FreeSpec with MockitoSugar with ScalaFutures with MustMatchers {

  private val cc = stubControllerComponents()
  private val identifierAction = new FakeIdentifierAction(cc.parsers.default, Agent)

  private val fakeUtr: String = "utr"

  private val closeEstateTransformationService: CloseEstateTransformationService =
    mock[CloseEstateTransformationService]
  private val controller: CloseEstateTransformationController =
    new CloseEstateTransformationController(identifierAction, cc, closeEstateTransformationService)

  "close estate" - {

    "must add a new close estate transform" in {

      val closeDate = LocalDate.parse("2000-01-01")

      when(closeEstateTransformationService.addCloseEstateTransformer(any(), any(), any()))
        .thenReturn(Future.successful(Success))

      val request = FakeRequest("POST", "path")
        .withBody(Json.toJson(closeDate))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.close(fakeUtr).apply(request)

      status(result) mustBe OK
      verify(closeEstateTransformationService).addCloseEstateTransformer(fakeUtr, "id", closeDate)
    }

    "must return an error for malformed json" in {

      val request = FakeRequest("POST", "path")
        .withBody(Json.toJson("{}"))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.close(fakeUtr).apply(request)
      status(result) mustBe BAD_REQUEST
    }
  }

  "get close date" - {

    "must return date if the transform exists" in {

      val expectedResult: LocalDate = LocalDate.parse("2000-01-01")

      when(closeEstateTransformationService.getCloseDate(any(), any()))
        .thenReturn(Future.successful(Some(expectedResult)))

      val request = FakeRequest("GET", "path")
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.getCloseDate(fakeUtr).apply(request)

      status(result) mustBe OK
      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe Json.toJson(expectedResult)
    }

    "must return false if the transform doesn't exist" in {

      when(closeEstateTransformationService.getCloseDate(any(), any()))
        .thenReturn(Future.successful(None))

      val request = FakeRequest("GET", "path")
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.getCloseDate(fakeUtr).apply(request)

      status(result) mustBe OK
      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe Json.obj()
    }
  }
}
