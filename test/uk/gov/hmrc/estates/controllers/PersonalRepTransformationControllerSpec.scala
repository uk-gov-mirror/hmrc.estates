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

package uk.gov.hmrc.estates.controllers

import java.time.LocalDate

import org.mockito.Matchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.MustMatchers
import play.api.libs.json.Json
import play.api.mvc.{BodyParsers, ControllerComponents}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.estates.BaseSpec
import uk.gov.hmrc.estates.controllers.actions.FakeIdentifierAction
import uk.gov.hmrc.estates.models.{EstatePerRepIndType, IdentificationType, NameType}
import uk.gov.hmrc.estates.services.{LocalDateService, PersonalRepTransformationService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Success

class PersonalRepTransformationControllerSpec extends BaseSpec with MockitoSugar with ScalaFutures with MustMatchers {

  private implicit val cc: ControllerComponents = injector.instanceOf[ControllerComponents]
  private val bodyParsers = injector.instanceOf[BodyParsers.Default]

  val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)

  object LocalDateServiceStub extends LocalDateService {
    override def now: LocalDate = LocalDate.of(1999, 3, 14)
  }

  "amend lead trustee" must {

    "add a new amend lead trustee transform" in {

      val personalRepTransformationService = mock[PersonalRepTransformationService]
      val controller = new PersonalRepTransformationController(identifierAction, personalRepTransformationService, cc, LocalDateServiceStub)

      val personalRep = EstatePerRepIndType(
        name =  NameType("First", None, "Last"),
        dateOfBirth = LocalDate.of(2000,1,1),
        identification = IdentificationType(None, None, None),
        phoneNumber = "07987654",
        email = None
      )

      when(personalRepTransformationService.addAmendEstatePerRepInTransformer(any(), any(), any()))
        .thenReturn(Future.successful(Success))

      val request = FakeRequest("POST", "path")
        .withBody(Json.toJson(personalRep))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.amendLeadTrustee("aUTR").apply(request)

      status(result) mustBe OK
      verify(personalRepTransformationService)
        .addAmendEstatePerRepInTransformer("aUTR", "id", personalRep)
    }

    "must return an error for malformed json" in {
      val personalRepTransformationService = mock[PersonalRepTransformationService]
      val controller = new PersonalRepTransformationController(identifierAction, personalRepTransformationService, cc, LocalDateServiceStub)

      val request = FakeRequest("POST", "path")
        .withBody(Json.parse("{}"))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.amendLeadTrustee("aUTR").apply(request)
      status(result) mustBe BAD_REQUEST
    }
  }
}
