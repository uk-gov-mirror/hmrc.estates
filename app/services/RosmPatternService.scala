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

package services

import com.google.inject.ImplementedBy
import javax.inject.Inject
import play.api.Logging
import uk.gov.hmrc.auth.core.AffinityGroup
import models.{TaxEnrolmentFailure, TaxEnrolmentNotProcessed, TaxEnrolmentSubscriberResponse, TaxEnrolmentSuccess}
import utils.Session
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal


class RosmPatternServiceImpl @Inject()(estateService: EstatesService,
                                       taxEnrolmentService : TaxEnrolmentsService,
                                       auditService: AuditService
                                      ) extends RosmPatternService with Logging {

  def getSubscriptionIdAndEnrol(trn : String, identifier: String)(implicit hc : HeaderCarrier): Future[TaxEnrolmentSubscriberResponse] ={

    for {
      subscriptionIdResponse <- estateService.getSubscriptionId(trn = trn)
      taxEnrolmentResponse <- taxEnrolmentService.setSubscriptionId(subscriptionIdResponse.subscriptionId)
    } yield {
      taxEnrolmentResponse match {
        case TaxEnrolmentSuccess =>
          auditService.auditEnrolSuccess(
            subscriptionIdResponse.subscriptionId,
            trn,
            identifier
          )
          TaxEnrolmentSuccess

        case response: TaxEnrolmentFailure =>
          auditService.auditEnrolFailed(
            subscriptionIdResponse.subscriptionId,
            trn,
            identifier,
            response.reason
          )
          response
        case r => r
      }
    }
  }

  override def enrol(trn: String, affinityGroup: AffinityGroup, identifier: String)
                    (implicit hc: HeaderCarrier) : Future[TaxEnrolmentSubscriberResponse] = {
    affinityGroup match {
      case AffinityGroup.Organisation =>
        getSubscriptionIdAndEnrol(trn, identifier) map {
          case TaxEnrolmentSuccess =>
            logger.info(s"[Session ID: ${Session.id(hc)}] Rosm completed successfully for provided trn: $trn.")
            TaxEnrolmentSuccess
          case response: TaxEnrolmentFailure =>
            logger.error(s"[Session ID: ${Session.id(hc)}]" +
              s" Rosm pattern is not completed for trn: $trn. with reason: ${response.reason}")
            response
          case r => r
        } recover {
          case NonFatal(e) =>
            logger.error(s"[Session ID: ${Session.id(hc)}] Rosm pattern is not completed for trn: $trn.")
            TaxEnrolmentFailure(s"Non-fatal error: ${e.getMessage}")
          }
      case _ =>
        logger.info(s"[Session ID: ${Session.id(hc)}] Tax enrolments is not required for Agent.")
        Future.successful(TaxEnrolmentNotProcessed)
    }
  }

}
@ImplementedBy(classOf[RosmPatternServiceImpl])
trait RosmPatternService {
  def enrol(trn: String, affinityGroup: AffinityGroup, identifier: String)(implicit hc: HeaderCarrier) : Future[TaxEnrolmentSubscriberResponse]
}
