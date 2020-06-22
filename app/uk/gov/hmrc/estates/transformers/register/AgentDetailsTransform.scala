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

package uk.gov.hmrc.estates.transformers.register

import play.api.libs.json.{JsPath, _}
import uk.gov.hmrc.estates.models.AgentDetails
import uk.gov.hmrc.estates.transformers.JsonOperations
import uk.gov.hmrc.estates.utils.JsonOps._

case class AgentDetailsTransform(agentDetails: AgentDetails)
    extends SetValueAtPathDeltaTransform with JsonOperations {

  override val path: JsPath = __ \ 'agentDetails

  override val value: JsValue = Json.toJson(agentDetails)

  override def applyDeclarationTransform(input: JsValue): JsResult[JsValue] =
    super.applyDeclarationTransform(input.applyRules)

}

object AgentDetailsTransform {

  val key = "AgentDetailsTransform"

  implicit val format: Format[AgentDetailsTransform] = Json.format[AgentDetailsTransform]
}



