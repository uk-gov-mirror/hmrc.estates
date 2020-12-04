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
import org.mockito.Matchers.{any, eq => equalTo}
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import exceptions.{BadRequestException, InternalServerErrorException}
import models.{SubscriptionIdResponse, TaxEnrolmentFailure, TaxEnrolmentSuccess}

import scala.concurrent.Future

class RosmPatternServiceSpec extends BaseSpec with BeforeAndAfterEach {

  private val mockEstateService = mock[EstatesService]
  private val mockTaxEnrolmentsService = mock[TaxEnrolmentsService]
  private val mockAuditService = mock[AuditService]
  private val identifier = "auth identifier"

  override def afterEach(): Unit =  {
    reset(mockAuditService)
  }

  val SUT = new RosmPatternServiceImpl(mockEstateService, mockTaxEnrolmentsService, mockAuditService)

  ".getSubscriptionIdAndEnrol" should {

    "return success taxEnrolmentSuscriberResponse " when {

      "successfully sets subscriptionId id in tax enrolments for provided trn." in {

        when(mockEstateService.getSubscriptionId("trn123456789")).
          thenReturn(Future.successful(SubscriptionIdResponse("123456789")))

        when(mockTaxEnrolmentsService.setSubscriptionId("123456789")).
          thenReturn(Future.successful(TaxEnrolmentSuccess))

        val futureResult = SUT.getSubscriptionIdAndEnrol("trn123456789", identifier)

        whenReady(futureResult) {
          result =>
            result mustBe TaxEnrolmentSuccess
            verify(mockAuditService).auditEnrolSuccess(
              equalTo("123456789"),
              equalTo("trn123456789"),
              equalTo(identifier))(any())
        }
      }
    }

    "return exception" when {

      "DES throws an exception" in {

        when(mockEstateService.getSubscriptionId("trn123456789")).
          thenReturn(Future.failed(InternalServerErrorException("bad juju")))

        val futureResult = SUT.getSubscriptionIdAndEnrol("trn123456789", identifier)

        whenReady(futureResult.failed) {
          result => result mustBe an[InternalServerErrorException]
        }
      }
    }

    "return TaxEnrolmentFailure" when {

      "tax enrolment service returns a failure" in {

        when(mockEstateService.getSubscriptionId("trn123456789")).
          thenReturn(Future.successful(SubscriptionIdResponse("123456789")))

        when(mockTaxEnrolmentsService.setSubscriptionId("123456789")).
          thenReturn(Future.successful(TaxEnrolmentFailure("test tax enrolment failure")))

        val futureResult = SUT.getSubscriptionIdAndEnrol("trn123456789", identifier)

        whenReady(futureResult) {
          result =>
            result mustBe TaxEnrolmentFailure("test tax enrolment failure")
            verify(mockAuditService).auditEnrolFailed(
              equalTo("123456789"),
              equalTo("trn123456789"),
              equalTo(identifier),
              equalTo("test tax enrolment failure"))(any())
        }
      }
    }

    "return BadRequestException" when {

      "tax enrolment service does not found provided subscription id." in {

        when(mockEstateService.getSubscriptionId("trn123456789")).
          thenReturn(Future.successful(SubscriptionIdResponse("123456789")))

        when(mockTaxEnrolmentsService.setSubscriptionId("123456789")).
          thenReturn(Future.failed(BadRequestException))

        val futureResult = SUT.getSubscriptionIdAndEnrol("trn123456789", identifier)

        whenReady(futureResult.failed) {
          result => result mustBe BadRequestException
        }
      }
    }
  }
}


