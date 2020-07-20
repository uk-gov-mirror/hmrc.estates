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

package transforms

import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.estates.models.{YearReturnType, YearsReturns}
import uk.gov.hmrc.repositories.TransformIntegrationTest

class YearsReturnsSpec extends WordSpec with MustMatchers with MockitoSugar with TransformIntegrationTest {

  private val cyMinusOneReturn =  YearReturnType(taxReturnYear = "20", taxConsequence = true)
  private val cyMinusTwoReturn =  YearReturnType(taxReturnYear = "19", taxConsequence = false)

  "an add YearsReturns call" must {
    "return added data in a subsequent 'GET' call" in assertMongoTest(createApplication) { app =>
          roundTripTest(app, YearsReturns(List(cyMinusOneReturn, cyMinusTwoReturn)))
          roundTripTest(app, YearsReturns(List(cyMinusOneReturn)))
    }
  }

  private def roundTripTest(app: Application, yearsReturns: YearsReturns) = {
    val amendRequest = FakeRequest(POST, "/estates/tax-liability")
      .withBody(Json.toJson(yearsReturns))
      .withHeaders(CONTENT_TYPE -> "application/json")

    val amendResult = route(app, amendRequest).get
    status(amendResult) mustBe OK

    val newResult = route(app, FakeRequest(GET, "/estates/tax-liability")).get
    status(newResult) mustBe OK
    contentAsJson(newResult) mustBe Json.toJson(yearsReturns)
  }
}
