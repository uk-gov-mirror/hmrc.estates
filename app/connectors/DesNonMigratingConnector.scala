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

package connectors

import java.util.UUID

import javax.inject.Inject
import play.api.Logging
import play.api.http.HeaderNames
import config.AppConfig
import models._
import utils.Constants._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future

class DesNonMigratingConnector @Inject()(http: HttpClient, config: AppConfig) extends Logging {

  private lazy val subscriptionsUrl : String = s"${config.estatesNonMigratingBaseUrl}/trusts"

  private val ENVIRONMENT_HEADER = "Environment"
  private val CORRELATION_HEADER = "CorrelationId"

  private def desHeadersNonMigrating(correlationId : String) : Seq[(String, String)] =
    Seq(
      HeaderNames.AUTHORIZATION -> s"Bearer ${config.desTokenNonMigrating}",
      CONTENT_TYPE -> CONTENT_TYPE_JSON,
      ENVIRONMENT_HEADER -> config.desEnvironmentNonMigrating,
      CORRELATION_HEADER -> correlationId
    )

  def getSubscriptionId(trn: String): Future[SubscriptionIdResponse] = {

    val correlationId = UUID.randomUUID().toString

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = desHeadersNonMigrating(correlationId))

    val subscriptionIdEndpointUrl = s"$subscriptionsUrl/trn/$trn/subscription"
    http.GET[SubscriptionIdResponse](subscriptionIdEndpointUrl)
  }
}
