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

import java.time.LocalDate

import org.scalatest.{AsyncWordSpec, MustMatchers}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import models.{EstateWillType, IdentificationType, NameType}
import uk.gov.hmrc.repositories.TransformIntegrationTest

class DeceasedSpec extends AsyncWordSpec with MustMatchers with MockitoSugar with TransformIntegrationTest {

  private val originalDeceased = EstateWillType(
    NameType("First", None, "Last"),
    Some(LocalDate.of(1996, 4, 15)),
    LocalDate.of(2016, 7, 2),
    Some(IdentificationType(
      nino = Some("AB000000C"),
      address = None,
      passport = None
    ))
  )

  private val newDeceased = EstateWillType(
    NameType("New First", Some("New Normal"), "New Last"),
    Some(LocalDate.of(1992, 4, 15)),
    LocalDate.of(2012, 7, 2),
    Some(IdentificationType(
      nino = Some("AB654321C"),
      address = None,
      passport = None
    ))
  )

  "an add Deceased call" must {
    "return added data in a subsequent 'GET' call" in assertMongoTest(createApplication) { app =>
          roundTripTest(app, originalDeceased)
          roundTripTest(app, newDeceased)
    }
  }

  private def roundTripTest(app: Application, deceased: EstateWillType) = {
    val amendRequest = FakeRequest(POST, "/estates/deceased")
      .withBody(Json.toJson(deceased))
      .withHeaders(CONTENT_TYPE -> "application/json")

    status(route(app, amendRequest).get) mustBe OK

    val newResult = route(app, FakeRequest(GET, "/estates/deceased")).get
    status(newResult) mustBe OK
    contentAsJson(newResult) mustBe Json.toJson(deceased)
  }
}
