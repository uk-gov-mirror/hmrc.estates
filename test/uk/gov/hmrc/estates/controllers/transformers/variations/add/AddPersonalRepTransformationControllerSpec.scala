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

package uk.gov.hmrc.estates.controllers.transformers.variations.add

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
import uk.gov.hmrc.estates.models.variation.EstatePerRepOrgType
import uk.gov.hmrc.estates.models.{AddressType, IdentificationOrgType, Success}
import uk.gov.hmrc.estates.services.variations.add.PersonalRepTransformationService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AddPersonalRepTransformationControllerSpec extends FreeSpec with MockitoSugar with ScalaFutures with MustMatchers {
  private val cc = stubControllerComponents()

  val identifierAction = new FakeIdentifierAction(cc.parsers.default, Agent)

  "add personal rep" - {

    "must add a new add personal rep transform" in {

      val personalRepTransformationService = mock[PersonalRepTransformationService]
      val controller = new AddPersonalRepTransformationController(identifierAction, cc, personalRepTransformationService)

      val newPersonalRepInfo = EstatePerRepOrgType(
        lineNo = Some("newLineNo"),
        bpMatchStatus = Some("newMatchStatus"),
        orgName = "Name",
        phoneNumber = "newPhone",
        email = None,
        identification = IdentificationOrgType(Some("utr"), Some(AddressType("Line 1", "Line 2", None, None, Some("POSTCODE"), "GB"))),
        entityStart = LocalDate.parse("2012-03-14"),
        entityEnd = None
      )

      when(personalRepTransformationService.addAddBusinessPersonalRepTransformer(any(), any(), any()))
        .thenReturn(Future.successful(Success))

      val request = FakeRequest("POST", "path")
        .withBody(Json.toJson(newPersonalRepInfo))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.addBusinessPersonalRep("aUTR").apply(request)

      status(result) mustBe OK
      verify(personalRepTransformationService).addAddBusinessPersonalRepTransformer("aUTR", "id", newPersonalRepInfo)
    }

    "must return an error for malformed json" in {

      val personalRepTransformationService = mock[PersonalRepTransformationService]
      val controller = new AddPersonalRepTransformationController(identifierAction, cc, personalRepTransformationService)

      val request = FakeRequest("POST", "path")
        .withBody(Json.toJson("{}"))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.addBusinessPersonalRep("aUTR").apply(request)
      status(result) mustBe BAD_REQUEST
    }
  }
}
