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

package controllers

import java.time.LocalDate

import base.BaseSpec
import org.mockito.Matchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatest.MustMatchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import models._
import services.{PersonalRepTransformationService, TransformationService}
import transformers.ComposedDeltaTransform
import transformers.register.PersonalRepTransform

import scala.concurrent.Future
import scala.util.Success

class PersonalRepTransformationControllerSpec extends BaseSpec with MockitoSugar with ScalaFutures with MustMatchers {

  private val transformationService = mock[TransformationService]

  private val personalRepInd = EstatePerRepIndType(
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

  private val personalRepOrg = EstatePerRepOrgType(
    orgName =  "Personal Rep Org",
    identification = IdentificationOrgType(None, None),
    phoneNumber = "07987654",
    email = None
  )

  "amend personal rep ind" must {

    "add a new amend personal rep transform" in {

        val personalRepTransformationService = mock[PersonalRepTransformationService]

        val application = applicationBuilder()
          .overrides(
            bind[PersonalRepTransformationService].toInstance(personalRepTransformationService)
          ).build()

        when(personalRepTransformationService.addAmendEstatePerRepIndTransformer(any(), any()))
          .thenReturn(Future.successful(Success))

        val controller = application.injector.instanceOf[PersonalRepTransformationController]

        val request = FakeRequest("POST", "path")
          .withBody(Json.toJson(personalRepInd))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.amendPersonalRepInd().apply(request)

        status(result) mustBe OK
        verify(personalRepTransformationService)
          .addAmendEstatePerRepIndTransformer("id", personalRepInd)
      }

    "must return an error for malformed json" in {

        val controller = injector.instanceOf[PersonalRepTransformationController]

        val request = FakeRequest("POST", "path")
          .withBody(Json.parse("{}"))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.amendPersonalRepInd().apply(request)
        status(result) mustBe BAD_REQUEST
      }
  }

  "amend personal rep org" must {

    "add a new amend personal rep transform" in {

        val personalRepTransformationService = mock[PersonalRepTransformationService]

        val application = applicationBuilder()
          .overrides(
            bind[PersonalRepTransformationService].toInstance(personalRepTransformationService)
          ).build()

        when(personalRepTransformationService.addAmendEstatePerRepOrgTransformer(any(), any()))
          .thenReturn(Future.successful(Success))

        val controller = application.injector.instanceOf[PersonalRepTransformationController]

        val request = FakeRequest("POST", "path")
          .withBody(Json.toJson(personalRepOrg))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.amendPersonalRepOrg().apply(request)

        status(result) mustBe OK
        verify(personalRepTransformationService)
          .addAmendEstatePerRepOrgTransformer("id", personalRepOrg)
    }

    "must return an error for malformed json" in {

        val controller = injector.instanceOf[PersonalRepTransformationController]

        val request = FakeRequest("POST", "path")
          .withBody(Json.parse("{}"))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.amendPersonalRepOrg().apply(request)
        status(result) mustBe BAD_REQUEST
    }
  }

  "getPersonalRepInd" should {

    "return 200 - Ok with processed content" when {
      "a transform is retrieved" in {

        val application = applicationBuilder()
          .overrides(
            bind[TransformationService].toInstance(transformationService)
          ).build()

        when(transformationService.getTransformations(any[String]))
          .thenReturn(Future.successful(Some(ComposedDeltaTransform(Seq(PersonalRepTransform(Some(personalRepInd), None))))))

        val controller = application.injector.instanceOf[PersonalRepTransformationController]

        val result = controller.getPersonalRepInd()(FakeRequest(GET, "/estates/personal-rep/individual"))

        status(result) mustBe OK
        contentType(result) mustBe Some(JSON)
        contentAsJson(result) mustBe Json.toJson(personalRepInd)

      }

      "a transform is not retrieved" in {

        val application = applicationBuilder()
          .overrides(
            bind[TransformationService].toInstance(transformationService)
          ).build()

        when(transformationService.getTransformations(any[String]))
          .thenReturn(Future.successful(None))

        val controller = application.injector.instanceOf[PersonalRepTransformationController]

        val result = controller.getPersonalRepInd()(FakeRequest(GET, "/estates/personal-rep/individual"))

        status(result) mustBe OK
        contentType(result) mustBe Some(JSON)
        contentAsJson(result) mustBe Json.toJson(Json.obj())
      }

    }
  }

  "getPersonalRepOrg" should {

    "return 200 - Ok with processed content" when {
      "a transform is retrieved" in {

        val application = applicationBuilder()
          .overrides(
            bind[TransformationService].toInstance(transformationService)
          ).build()

        when(transformationService.getTransformations(any[String]))
          .thenReturn(Future.successful(Some(ComposedDeltaTransform(Seq(PersonalRepTransform(None, Some(personalRepOrg)))))))

        val controller = application.injector.instanceOf[PersonalRepTransformationController]

        val result = controller.getPersonalRepOrg()(FakeRequest(GET, "/estates/personal-rep/organisation"))

        status(result) mustBe OK
        contentType(result) mustBe Some(JSON)
        contentAsJson(result) mustBe Json.toJson(personalRepOrg)

      }

      "a transform is not retrieved" in {

        val application = applicationBuilder()
          .overrides(
            bind[TransformationService].toInstance(transformationService)
          ).build()

        when(transformationService.getTransformations(any[String]))
          .thenReturn(Future.successful(None))

        val controller = application.injector.instanceOf[PersonalRepTransformationController]

        val result = controller.getPersonalRepOrg()(FakeRequest(GET, "/estates/personal-rep/organisation"))

        status(result) mustBe OK
        contentType(result) mustBe Some(JSON)
        contentAsJson(result) mustBe Json.toJson(Json.obj())
      }

    }
  }
}
