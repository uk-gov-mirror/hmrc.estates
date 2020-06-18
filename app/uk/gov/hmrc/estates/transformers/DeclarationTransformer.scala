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

package uk.gov.hmrc.estates.transformers

import play.api.libs.json._
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.estates.models.{AddressType, Declaration, NameType}

class DeclarationTransformer {

  def transform(actor: AffinityGroup, body: JsValue, declarationName: NameType): JsResult[JsValue] = {
    addDeclaration(actor, declarationName, body)
  }

  private val correspondenceAddressPath: JsPath = __ \ 'correspondence \ 'address
  private val agentAddressPath: JsPath          = __ \ 'agentDetails \ 'agentAddress

  private def addDeclaration(actor: AffinityGroup, name: NameType, responseJson: JsValue): JsResult[JsObject] = {
    for {
      addressJson <- if (actor == Agent){
        takeAddressFromPath(agentAddressPath ,responseJson)
      } else {
        takeAddressFromPath(correspondenceAddressPath, responseJson)
      }
      address <- addressJson.validate[AddressType]
      declaration = Declaration(name, address)
      updated <- responseJson.transform(putNewValue(__ \ 'declaration, Json.toJson(declaration)))
    } yield {
      updated
    }
  }

  private def takeAddressFromPath(path: JsPath, responseJson: JsValue): JsResult[JsValue] = {
    responseJson.transform(path.json.pick)
  }

  private def putNewValue(path: JsPath, value: JsValue ): Reads[JsObject] =
    __.json.update(path.json.put(value))

}
