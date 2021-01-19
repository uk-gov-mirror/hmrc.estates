/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.transform.register

import base.BaseSpec
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
import controllers.actions.FakeIdentifierAction
import models.{Success, YearReturnType, YearsReturns}
import services.register.YearsReturnsTransformationService

import scala.concurrent.Future

class YearsReturnsTransformationControllerSpec extends BaseSpec with MockitoSugar with ScalaFutures with MustMatchers {

  import scala.concurrent.ExecutionContext.Implicits._

  private implicit val cc: ControllerComponents = injector.instanceOf[ControllerComponents]
  private val bodyParsers = injector.instanceOf[BodyParsers.Default]

  val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)

  val mockTransformationService: YearsReturnsTransformationService = mock[YearsReturnsTransformationService]

  "years returns controller" when {

    val cyMinusOneReturn =  YearReturnType(taxReturnYear = "20", taxConsequence = true)
    val cyMinusTwoReturn =  YearReturnType(taxReturnYear = "19", taxConsequence = false)

    ".get" must {

      "return the years returns" in {
        val controller = new YearsReturnsTransformationController(identifierAction, cc, mockTransformationService)

        when(mockTransformationService.get(any())).thenReturn(Future.successful(Some(YearsReturns(List(cyMinusOneReturn, cyMinusTwoReturn)))))

        val request = FakeRequest("GET", "path")
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.get.apply(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(JSON)
        contentAsJson(result) mustBe Json.parse(
          """
            |{
            | "returns":[
            |   {
            |     "taxReturnYear":"20",
            |     "taxConsequence":true
            |   },
            |   {
            |     "taxReturnYear":"19",
            |     "taxConsequence":false
            |   }
            | ]
            |}
            |""".stripMargin)
      }

      "return an empty json object when there is no years returns" in {
        val controller = new YearsReturnsTransformationController(identifierAction, cc, mockTransformationService)

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
        val controller = new YearsReturnsTransformationController(identifierAction, cc, mockTransformationService)

        val returns = YearsReturns(List(cyMinusOneReturn, cyMinusTwoReturn))

        val request = FakeRequest("POST", "path")
          .withBody(Json.toJson(returns))
          .withHeaders(CONTENT_TYPE -> "application/json")

        when(mockTransformationService.addTransform(any(), any())).thenReturn(Future.successful(Success))

        val result = controller.save().apply(request)

        status(result) mustBe OK
      }

      "must return an error for malformed json" in {
        val controller = new YearsReturnsTransformationController(identifierAction, cc, mockTransformationService)

        val request = FakeRequest("POST", "path")
          .withBody(Json.toJson("{}"))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.save().apply(request)

        status(result) mustBe BAD_REQUEST
      }

    }

    ".reset" must {

      "remove all YearsReturns transforms" in {
        val controller = new YearsReturnsTransformationController(identifierAction, cc, mockTransformationService)

        val request = FakeRequest("POST", "path")

        when(mockTransformationService.removeTransforms(any())).thenReturn(Future.successful(Success))

        val result = controller.reset().apply(request)

        status(result) mustBe OK
      }
    }

  }

}
