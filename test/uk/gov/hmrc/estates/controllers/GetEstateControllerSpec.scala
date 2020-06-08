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

import org.mockito.Matchers.{any, eq => mockEq}
import org.mockito.Mockito.{verify, when}
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsSuccess, JsValue, Json}
import play.api.mvc.{BodyParsers, ControllerComponents}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.estates.BaseSpec
import uk.gov.hmrc.estates.controllers.actions.{FakeIdentifierAction, ValidateUTRActionFactory}
import uk.gov.hmrc.estates.models.{EstatePerRepIndType, IdentificationType, NameType}
import uk.gov.hmrc.estates.services.{AuditService, DesService, TransformationService}
import uk.gov.hmrc.estates.utils.JsonRequests
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GetEstateControllerSpec  extends BaseSpec
  with MockitoSugar
  with MustMatchers
  with BeforeAndAfter
  with BeforeAndAfterEach
  with JsonRequests
  with Inside
  with ScalaFutures {

  val utr = "1234567890"

  private val desService: DesService = mock[DesService]
  private val transformationService = mock[TransformationService]
  private val mockedAuditService: AuditService = mock[AuditService]

  private val validateUTRActionFactory = injector.instanceOf[ValidateUTRActionFactory]

  private implicit val cc: ControllerComponents = injector.instanceOf[ControllerComponents]
  private val bodyParsers = injector.instanceOf[BodyParsers.Default]

  val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)

  private def getTrustController =
    new GetEstateController(identifierAction, mockedAuditService, desService, validateUTRActionFactory, transformationService)

  ".getPersonalRep" should {

    "return 200 - Ok with processed content" in {

      val personalRep = EstatePerRepIndType(
        name = NameType("First", None, "Last"),
        dateOfBirth = LocalDate.of(2019, 6, 1),
        identification = IdentificationType(
          nino = Some("JH123456C"),
          passport = None,
          address = None
        ),
        phoneNumber = "07987654345",
        email = None
      )

      when(transformationService.getTransformedData(any[String], any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(personalRep)))

      val result = getTrustController.getPersonalRep(utr)(FakeRequest(GET, s"/trusts/$utr/transformed/personal-rep"))

      status(result) mustBe OK
      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe Json.toJson(personalRep)

    }

  }

}
