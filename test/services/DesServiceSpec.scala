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

package services

import base.BaseSpec
import org.mockito.Mockito.{times, verify, verifyZeroInteractions, when}
import org.mockito.Matchers._
import play.api.libs.json.JsValue
import connectors.{DesConnector, DesNonMigratingConnector}
import exceptions._
import models.ExistingCheckResponse._
import models._
import models.getEstate._
import models.variation.{VariationFailureResponse, VariationSuccessResponse}
import repositories.CacheRepositoryImpl
import utils.ErrorResponses.DuplicateSubmissionErrorResponse
import utils.{JsonRequests, JsonUtils}

import scala.concurrent.Future

class DesServiceSpec extends BaseSpec with JsonRequests {

  private trait DesServiceFixture {
    lazy val request = ExistingCheckRequest("estate name", postcode = Some("NE65TA"), "1234567890")
    val mockConnector: DesConnector = mock[DesConnector]
    val mockNonMigratingConnector: DesNonMigratingConnector = mock[DesNonMigratingConnector]
    val mockRepository: CacheRepositoryImpl = mock[CacheRepositoryImpl]
    when(mockRepository.get(any[String], any[String])).thenReturn(Future.successful(None))
    when(mockRepository.resetCache(any[String], any[String])).thenReturn(Future.successful(None))
    val myId = "myId"

    val SUT = new DesService(mockConnector, mockNonMigratingConnector, mockRepository)
  }

  ".getEstateInfoFormBundleNo should return formBundle No from ETMP Data" in {
    val etmpData = JsonUtils.getJsonValueFromFile("etmp/valid-get-estate-5mld-response.json").as[GetEstateResponse].asInstanceOf[GetEstateProcessedResponse]
    val mockDesconnector = mock[DesConnector]
    val mockDesNonMigratingConnector: DesNonMigratingConnector = mock[DesNonMigratingConnector]
    val mockRepository = mock[CacheRepositoryImpl]
    when(mockDesconnector.getEstateInfo(any())).thenReturn(Future.successful(etmpData))

    val OUT = new DesService(mockDesconnector, mockDesNonMigratingConnector, mockRepository)

    whenReady(OUT.getEstateInfoFormBundleNo("75464876")) {formBundleNo =>
      formBundleNo mustBe etmpData.responseHeader.formBundleNo
    }
  }
  
  ".checkExistingEstate" should {
    "return Matched " when {
      "connector returns Matched." in new DesServiceFixture {
        when(mockConnector.checkExistingEstate(request)).
          thenReturn(Future.successful(Matched))
        val futureResult = SUT.checkExistingEstate(request)
        whenReady(futureResult) {
          result => result mustBe Matched
        }
      }
    }


    "return NotMatched " when {
      "connector returns NotMatched." in new DesServiceFixture {
        when(mockConnector.checkExistingEstate(request)).
          thenReturn(Future.successful(NotMatched))
        val futureResult = SUT.checkExistingEstate(request)
        whenReady(futureResult) {
          result => result mustBe NotMatched
        }
      }
    }

    "return BadRequest " when {
      "connector returns BadRequest." in new DesServiceFixture {
        when(mockConnector.checkExistingEstate(request)).
          thenReturn(Future.successful(BadRequest))
        val futureResult = SUT.checkExistingEstate(request)
        whenReady(futureResult) {
          result => result mustBe BadRequest
        }
      }
    }

    "return AlreadyRegistered " when {
      "connector returns AlreadyRegistered." in new DesServiceFixture {
        when(mockConnector.checkExistingEstate(request)).
          thenReturn(Future.successful(AlreadyRegistered))
        val futureResult = SUT.checkExistingEstate(request)
        whenReady(futureResult) {
          result => result mustBe AlreadyRegistered
        }
      }
    }

    "return ServiceUnavailable " when {
      "connector returns ServiceUnavailable." in new DesServiceFixture {
        when(mockConnector.checkExistingEstate(request)).
          thenReturn(Future.successful(ServiceUnavailable))
        val futureResult = SUT.checkExistingEstate(request)
        whenReady(futureResult) {
          result => result mustBe ServiceUnavailable
        }

      }
    }

    "return ServerError " when {
      "connector returns ServerError." in new DesServiceFixture {
        when(mockConnector.checkExistingEstate(request)).
          thenReturn(Future.successful(ServerError))
        val futureResult = SUT.checkExistingEstate(request)
        whenReady(futureResult) {
          result => result mustBe ServerError
        }
      }
    }
  }

  ".registerEstate" should {

    "return RegistrationTrnResponse " when {
      "connector returns RegistrationTrnResponse." in new DesServiceFixture {
        when(mockConnector.registerEstate(estateRegRequest)).
          thenReturn(Future.successful(RegistrationTrnResponse("trn123")))
        val futureResult = SUT.registerEstate(estateRegRequest)
        whenReady(futureResult) {
          result => result mustBe RegistrationTrnResponse("trn123")
        }
      }
    }

    "return same Exception " when {
      "connector returns exception." in new DesServiceFixture {
        when(mockConnector.registerEstate(estateRegRequest)).
          thenReturn(Future.failed(InternalServerErrorException("")))
        val futureResult = SUT.registerEstate(estateRegRequest)

        whenReady(futureResult.failed) {
          result => result mustBe an[InternalServerErrorException]
        }
      }
    }

  }

  ".getSubscriptionId" should {

    "return SubscriptionIdResponse " when {
      "connector returns SubscriptionIdResponse." in new DesServiceFixture {
        when(mockNonMigratingConnector.getSubscriptionId("trn123456789")).
          thenReturn(Future.successful(SubscriptionIdResponse("123456789")))
        val futureResult = SUT.getSubscriptionId("trn123456789")
        whenReady(futureResult) {
          result => result mustBe SubscriptionIdResponse("123456789")
        }
      }
    }

    "return same Exception " when {
      "connector returns  exception." in new DesServiceFixture {
        when(mockNonMigratingConnector.getSubscriptionId("trn123456789")).
          thenReturn(Future.failed(InternalServerErrorException("")))
        val futureResult = SUT.getSubscriptionId("trn123456789")

        whenReady(futureResult.failed) {
          result => result mustBe an[InternalServerErrorException]
        }
      }
    }
  }

  ".getEstateInfo" should {
    "return EstateFoundResponse" when {
      "EstateFoundResponse is returned from DES Connector with a Processed flag and an estate body when not cached" in new DesServiceFixture {
        val utr = "1234567890"
        val fullEtmpResponseJson = get4MLDEstateResponse
        val estateInfoJson = (fullEtmpResponseJson \ "trustOrEstateDisplay").as[JsValue]

        when(mockRepository.get(any[String], any[String])).thenReturn(Future.successful(None))
        when(mockRepository.set(any[String], any[String], any[JsValue])).thenReturn(Future.successful(true))
        when(mockRepository.resetCache(any[String], any[String])).thenReturn(Future.successful(None))
        when(mockConnector.getEstateInfo(any()))
          .thenReturn(Future.successful(GetEstateProcessedResponse(estateInfoJson, ResponseHeader("Processed", "1"))))

        val futureResult = SUT.getEstateInfo(utr, myId)
        whenReady(futureResult) { result =>
          result mustBe GetEstateProcessedResponse(estateInfoJson, ResponseHeader("Processed", "1"))
          verify(mockRepository, times(1)).set(utr, myId, fullEtmpResponseJson)
        }
      }

      "EstateFoundResponse is returned from repository with a Processed flag and an estate body when cached" in new DesServiceFixture {
        val utr = "1234567890"

        val fullEtmpResponseJson = get4MLDEstateResponse
        val estateInfoJson = (fullEtmpResponseJson \ "trustOrEstateDisplay").as[JsValue]

        when(mockRepository.get(any[String], any[String])).thenReturn(Future.successful(Some(fullEtmpResponseJson)))
        when(mockConnector.getEstateInfo(any())).thenReturn(Future.failed(new Exception("Connector should not have been called")))

        val futureResult = SUT.getEstateInfo(utr, myId)
        whenReady(futureResult) { result =>
          result mustBe GetEstateProcessedResponse(estateInfoJson, ResponseHeader("Processed", "1"))
          verifyZeroInteractions(mockConnector)
        }
      }
    }

    "return BadRequestResponse" when {
      "BadRequestResponse is returned from DES Connector" in new DesServiceFixture {

        when(mockConnector.getEstateInfo(any())).thenReturn(Future.successful(BadRequestResponse))

        val utr = "123456789"
        val futureResult = SUT.getEstateInfo(utr, myId)

        whenReady(futureResult) { result =>
          result mustBe BadRequestResponse
        }
      }
    }

    "return ResourceNotFoundResponse" when {
      "ResourceNotFoundResponse is returned from DES Connector" in new DesServiceFixture {

        when(mockConnector.getEstateInfo(any())).thenReturn(Future.successful(ResourceNotFoundResponse))

        val utr = "123456789"
        val futureResult = SUT.getEstateInfo(utr, myId)

        whenReady(futureResult) { result =>
          result mustBe ResourceNotFoundResponse
        }
      }
    }

    "return InternalServerErrorResponse" when {
      "InternalServerErrorResponse is returned from DES Connector" in new DesServiceFixture {

        when(mockConnector.getEstateInfo(any())).thenReturn(Future.successful(InternalServerErrorResponse))

        val utr = "123456789"
        val futureResult = SUT.getEstateInfo(utr, myId)

        whenReady(futureResult) { result =>
          result mustBe InternalServerErrorResponse
        }
      }
    }

    "return ServiceUnavailableResponse" when {
      "ServiceUnavailableResponse is returned from DES Connector" in new DesServiceFixture {

        when(mockConnector.getEstateInfo(any())).thenReturn(Future.successful(ServiceUnavailableResponse))

        val utr = "123456789"
        val futureResult = SUT.getEstateInfo(utr, myId)

        whenReady(futureResult) { result =>
          result mustBe ServiceUnavailableResponse
        }
      }
    }
  } // getEstateInfo

  ".estateVariation" should {
    "return a VariationTvnResponse" when {

      "connector returns VariationResponse." in new DesServiceFixture {

        when(mockConnector.estateVariation(estateVariationsRequest)).
          thenReturn(Future.successful(VariationSuccessResponse("tvn123")))

        val futureResult = SUT.estateVariation(estateVariationsRequest)

        whenReady(futureResult) {
          result => result mustBe VariationSuccessResponse("tvn123")
        }

      }


      "return DuplicateSubmissionException" when {

        "connector returns  DuplicateSubmissionException." in new DesServiceFixture {

          when(mockConnector.estateVariation(estateVariationsRequest)).
            thenReturn(Future.successful(VariationFailureResponse(DuplicateSubmissionErrorResponse)))

          val futureResult = SUT.estateVariation(estateVariationsRequest)

          whenReady(futureResult) {
            result => result mustBe VariationFailureResponse(DuplicateSubmissionErrorResponse)
          }

        }

      }

      "return same Exception " when {
        "connector returns  exception." in new DesServiceFixture {

          when(mockConnector.estateVariation(estateVariationsRequest)).
            thenReturn(Future.failed(InternalServerErrorException("")))

          val futureResult = SUT.estateVariation(estateVariationsRequest)

          whenReady(futureResult.failed) {
            result => result mustBe an[InternalServerErrorException]
          }

        }
      }

    }
  }

}
