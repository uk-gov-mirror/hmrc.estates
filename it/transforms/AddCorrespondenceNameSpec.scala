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
import play.api.inject.bind
import play.api.libs.json.{JsString, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.estates.controllers.actions.{FakeIdentifierAction, IdentifierAction}
import uk.gov.hmrc.estates.models.register.AmountOfTaxOwed
import uk.gov.hmrc.estates.models.register.TaxAmount.{AmountMoreThanFiveHundredThousand, AmountMoreThanTenThousand}
import uk.gov.hmrc.repositories.TransformIntegrationTest

import scala.concurrent.ExecutionContext.Implicits.global

class AddCorrespondenceNameSpec extends WordSpec with MustMatchers with MockitoSugar with TransformIntegrationTest {

  private val cc = stubControllerComponents()

  private val application = applicationBuilder
    .overrides(
      bind[IdentifierAction].toInstance(new FakeIdentifierAction(cc.parsers.default, Organisation))
    )
    .build()

  val newEstateName = JsString("New Estate Name")
  val newEstateName2 = JsString("New Estate Name 2")

  "an add correspondence name call" must {
    "return added data in a subsequent 'GET' call" in {

      running(application) {
        getConnection(application).map { connection =>

          dropTheDatabase(connection)


          roundTripTest(newEstateName)
          roundTripTest(newEstateName2)

          dropTheDatabase(connection)
        }.get
      }
    }
  }

  private def roundTripTest(name: JsString) = {
    val amendRequest = FakeRequest(POST, "/estates/correspondence/name")
      .withBody(Json.toJson(name))
      .withHeaders(CONTENT_TYPE -> "application/json")

    val amendResult = route(application, amendRequest).get
    status(amendResult) mustBe OK

    val newResult = route(application, FakeRequest(GET, "/estates/correspondence/name")).get
    status(newResult) mustBe OK
    contentAsJson(newResult) mustBe Json.obj("name" -> name)
  }
}
