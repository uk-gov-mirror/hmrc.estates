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

package uk.gov.hmrc.estates.config

import akka.actor.ActorSystem
import javax.inject.{Inject, Singleton}
import play.api.libs.ws.WSClient
import play.api.{Configuration, Environment}
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.hooks.{HttpHook, HttpHooks}
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.http.ws._


@Singleton
class WSHttp @Inject()(
                        val conf: Configuration,
                        val environment: Environment,
                        override val auditConnector: AuditConnector,
                        override val wsClient: WSClient,
                        override val actorSystem: ActorSystem
                      )
  extends WSGet with HttpGet
    with WSPut with HttpPut
    with WSPost with HttpPost
    with WSDelete with HttpDelete
    with AppName
    with HttpHooks with HttpAuditing {

  override val hooks: Seq[HttpHook] = Seq(AuditingHook)

  override protected def appNameConfiguration: Configuration = conf

  override protected def configuration: Option[com.typesafe.config.Config] = Some(conf.underlying)

}






