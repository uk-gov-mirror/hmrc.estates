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

package uk.gov.hmrc.estates.services

import com.google.inject.ImplementedBy
import javax.inject.Inject
import play.api.Logger
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.estates.models.{TaxEnrolmentFailure, TaxEnrolmentNotProcessed, TaxEnrolmentSuccess, TaxEnrolmentSubscriberResponse}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal


class RosmPatternServiceImpl @Inject()(desService: DesService,
                                       taxEnrolmentService : TaxEnrolmentsService
                                      ) extends RosmPatternService {

  override def getSubscriptionIdAndEnrol(trn : String)(implicit hc : HeaderCarrier): Future[TaxEnrolmentSubscriberResponse] ={

    for {
      subscriptionIdResponse <- desService.getSubscriptionId(trn = trn)
      taxEnrolmentSuscriberResponse <- taxEnrolmentService.setSubscriptionId(subscriptionIdResponse.subscriptionId)
    } yield {
      taxEnrolmentSuscriberResponse
    }
  }

  override def enrol(trn: String, affinityGroup: AffinityGroup)
                    (implicit hc: HeaderCarrier) : Future[TaxEnrolmentSubscriberResponse] = {
    affinityGroup match {
      case AffinityGroup.Organisation =>
        getSubscriptionIdAndEnrol(trn) map {
          case TaxEnrolmentSuccess =>
            Logger.info(s"Rosm completed successfully for provided trn: $trn.")
            TaxEnrolmentSuccess
          case TaxEnrolmentFailure =>
            Logger.error(s"Rosm pattern is not completed for trn: $trn.")
            TaxEnrolmentFailure
        } recover {
          case NonFatal(_) =>
            Logger.error(s"Rosm pattern is not completed for trn: $trn.")
            TaxEnrolmentFailure
          }
      case _ =>
        Logger.info("Tax enrolments is not required for Agent.")
        Future.successful(TaxEnrolmentNotProcessed)
    }
  }

}
@ImplementedBy(classOf[RosmPatternServiceImpl])
trait RosmPatternService {
  def getSubscriptionIdAndEnrol(trn : String)(implicit hc : HeaderCarrier): Future[TaxEnrolmentSubscriberResponse]
  def enrol(trn: String, affinityGroup: AffinityGroup)(implicit hc: HeaderCarrier) : Future[TaxEnrolmentSubscriberResponse]
}
