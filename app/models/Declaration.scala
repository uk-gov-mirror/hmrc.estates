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

package models

import java.time.LocalDate

import play.api.libs.json.{Format, Json}

case class Declaration(name: NameType,
                       address: AddressType)

object Declaration {
  implicit val declarationFormat: Format[Declaration] = Json.format[Declaration]
}

case class DeclarationName(name: NameType)

object DeclarationName {
  implicit val declarationFormat: Format[DeclarationName] = Json.format[DeclarationName]
}

case class DeclarationForApi(declaration: DeclarationName,
                             agentDetails: Option[AgentDetails])

object DeclarationForApi {
  implicit val declarationForApiFormat: Format[DeclarationForApi] = Json.format[DeclarationForApi]
}