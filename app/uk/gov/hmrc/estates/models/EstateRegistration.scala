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

package uk.gov.hmrc.estates.models

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class EstateRegistration(matchData: Option[MatchData],
                              correspondence: Correspondence,
                              yearsReturns: Option[YearsReturns],
                              declaration: Declaration,
                              estate: Estate,
                              agentDetails: Option[AgentDetails] = None
                             )

object EstateRegistration {
  implicit val estateRegistrationFormat: Format[EstateRegistration] = Json.format[EstateRegistration]

  val estateRegistrationWriteToDes :Writes[EstateRegistration] = (
    (JsPath \ "matchData").writeNullable[MatchData] and
      (JsPath \ "correspondence").write[Correspondence] and
      (JsPath \ "declaration").write[Declaration] and
      (JsPath \ "yearsReturns").writeNullable[YearsReturns] and
      (JsPath \ "details" \ "estate").write[Estate](Estate.estateWriteToDes) and
      (JsPath \ "agentDetails" ).writeNullable[AgentDetails]
    )(r => (r.matchData, r.correspondence,r.declaration, r.yearsReturns, r.estate,r.agentDetails))
}