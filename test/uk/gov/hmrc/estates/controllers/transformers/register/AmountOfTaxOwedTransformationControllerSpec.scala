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

package uk.gov.hmrc.estates.controllers.transformers.register

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
import uk.gov.hmrc.estates.BaseSpec
import uk.gov.hmrc.estates.controllers.actions.FakeIdentifierAction
import uk.gov.hmrc.estates.models.Success
import uk.gov.hmrc.estates.models.register.AmountOfTaxOwed
import uk.gov.hmrc.estates.models.register.TaxAmount.AmountMoreThanTenThousand
import uk.gov.hmrc.estates.services.register.AmountOfTaxTransformationService

import scala.concurrent.Future

class AmountOfTaxOwedTransformationControllerSpec extends BaseSpec with MockitoSugar with ScalaFutures with MustMatchers {

  import scala.concurrent.ExecutionContext.Implicits._

  private implicit val cc: ControllerComponents = injector.instanceOf[ControllerComponents]
  private val bodyParsers = injector.instanceOf[BodyParsers.Default]

  val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)
  
  val mockTransformationService: AmountOfTaxTransformationService = mock[AmountOfTaxTransformationService]

  "amount of tax owed controller" when {

    ".get" must {

      "return the amount of tax owed" in {
        val controller = new AmountOfTaxOwedTransformationController(identifierAction, cc, LocalDateServiceStub, mockTransformationService)

        when(mockTransformationService.get(any())).thenReturn(Future.successful(Some(AmountOfTaxOwed(AmountMoreThanTenThousand))))

        val request = FakeRequest("GET", "path")
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.get.apply(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(JSON)
        contentAsJson(result) mustBe Json.parse(
          """
            |{
            | "amount": "01"
            |}
            |""".stripMargin)
      }

      "return an empty json object when there is no amount" in {
        val controller = new AmountOfTaxOwedTransformationController(identifierAction, cc, LocalDateServiceStub, mockTransformationService)

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
        val controller = new AmountOfTaxOwedTransformationController(identifierAction, cc, LocalDateServiceStub, mockTransformationService)

        val amount = AmountOfTaxOwed(AmountMoreThanTenThousand)

        val request = FakeRequest("POST", "path")
          .withBody(Json.toJson(amount))
          .withHeaders(CONTENT_TYPE -> "application/json")

        when(mockTransformationService.addTransform(any(), any())).thenReturn(Future.successful(Success))

        val result = controller.save().apply(request)

        status(result) mustBe OK
      }

      "must return an error for malformed json" in {
        val controller = new AmountOfTaxOwedTransformationController(identifierAction, cc, LocalDateServiceStub, mockTransformationService)

        val request = FakeRequest("POST", "path")
          .withBody(Json.toJson("{}"))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.save().apply(request)

        status(result) mustBe BAD_REQUEST
      }

    }

  }

}
