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

import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{MustMatchers, WordSpec}
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.estates.controllers.actions.{FakeIdentifierAction, IdentifierAction}
import uk.gov.hmrc.estates.models.{AddressType, EstatePerRepIndType, IdentificationType, NameType}
import uk.gov.hmrc.repositories.TransformIntegrationTest

import scala.concurrent.ExecutionContext.Implicits.global

class AmendPersonalRepIndSpec extends WordSpec with MustMatchers with MockitoSugar with TransformIntegrationTest {

  private val cc = stubControllerComponents()

  private val application = applicationBuilder
    .overrides(
      bind[IdentifierAction].toInstance(new FakeIdentifierAction(cc.parsers.default, Organisation))
    )
    .build()

  "an amend personal rep call" must {
    "return amended data in a subsequent 'get' call" in {

      val newPersonalRep = EstatePerRepIndType(
        name = NameType("newFirstName", Some("newMiddleName"), "newLastName"),
        dateOfBirth = LocalDate.of(1965, 2, 10),
        phoneNumber = "newPhone",
        email = Some("newEmail"),
        identification = IdentificationType(
          None,
          None,
          Some(AddressType(
            "1344 Army Road",
            "Suite 111",
            Some("Telford"),
            Some("Shropshire"),
            Some("TF1 5DR"),
            "GB"
          )))
      )

      running(application) {
        getConnection(application).map { connection =>

          dropTheDatabase(connection)

          val amendRequest = FakeRequest(POST, "/estates/personal-rep/amend/5174384721")
            .withBody(Json.toJson(newPersonalRep))
            .withHeaders(CONTENT_TYPE -> "application/json")

          val amendResult = route(application, amendRequest).get
          status(amendResult) mustBe OK

          val newResult = route(application, FakeRequest(GET, "/estates/personal-rep/individual/5174384721")).get
          status(newResult) mustBe OK
          contentAsJson(newResult) mustBe Json.toJson(newPersonalRep)

          dropTheDatabase(connection)
        }.get
      }
    }
  }
}
