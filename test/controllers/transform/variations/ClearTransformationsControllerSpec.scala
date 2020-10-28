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

package controllers.transform.variations

import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FreeSpec, MustMatchers}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{CONTENT_TYPE, _}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import controllers.actions.FakeIdentifierAction
import services.VariationsTransformationService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ClearTransformationsControllerSpec extends FreeSpec with MockitoSugar with ScalaFutures with MustMatchers {

  private val cc = stubControllerComponents()
  val identifierAction = new FakeIdentifierAction(cc.parsers.default, Agent)

  val fakeUtr: String = "utr"

  "clear transformations controller" - {

    val variationsTransformationService = mock[VariationsTransformationService]
    val controller = new ClearTransformationsController(identifierAction, cc, variationsTransformationService)

    "must clear transformations" in {

      when(variationsTransformationService.removeAllTransformations(any(), any()))
        .thenReturn(Future.successful(Some(Json.obj())))

      val request = FakeRequest("POST", "path")
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.clearTransformations(fakeUtr).apply(request)

      status(result) mustBe OK
      verify(variationsTransformationService).removeAllTransformations(fakeUtr, "id")
    }
  }
}
