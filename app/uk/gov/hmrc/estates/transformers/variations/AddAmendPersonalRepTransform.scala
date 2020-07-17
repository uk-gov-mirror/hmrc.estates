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

package uk.gov.hmrc.estates.transformers.variations

import play.api.libs.json._

trait AddAmendPersonalRepTransform {
  def setPersonalRep(input: JsValue, newPersonalRepDetails: JsValue): JsResult[JsValue] = {
    val personalRepPath = (__ \ 'details \ 'estate \ 'entities \ 'personalRepresentative)
    val entityStartPath = personalRepPath \ 'entityStart

    val entityStartPick = entityStartPath.json.pick
    input.transform(entityStartPick) match {
      case JsSuccess(entityStart, _) =>
        input.transform(
          personalRepPath.json.prune andThen
            (__).json.update(personalRepPath.json.put(newPersonalRepDetails)) andThen
            (__).json.update(entityStartPath.json.put(entityStart)) andThen
            (personalRepPath \ 'lineNo).json.prune andThen
            (personalRepPath \ 'bpMatchStatus).json.prune
        )
      case e: JsError => e
    }
  }
}
