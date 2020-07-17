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

import play.api.libs.json.{JsValue, _}
import uk.gov.hmrc.estates.transformers.variations.amend.{AmendBusinessPersonalRepTransform, AmendIndividualPersonalRepTransform}
import uk.gov.hmrc.estates.transformers.register._
import uk.gov.hmrc.estates.transformers.variations.add.AddBusinessPersonalRepTransform

trait DeltaTransform {

  def applyTransform(input: JsValue): JsResult[JsValue]

  def applyDeclarationTransform(input: JsValue): JsResult[JsValue] = JsSuccess(input)
}

object DeltaTransform {

  private def readsForTransform[T](key: String)(implicit reads: Reads[T]): PartialFunction[JsObject, JsResult[T]] = {
    case json if json.keys.contains(key) =>
      (json \ key).validate[T]
  }

  def personalRepReads: PartialFunction[JsObject, JsResult[DeltaTransform]] = {
    readsForTransform[PersonalRepTransform](PersonalRepTransform.key)
  }

  def correspondenceNameReads: PartialFunction[JsObject, JsResult[DeltaTransform]] = {
    readsForTransform[CorrespondenceNameTransform](CorrespondenceNameTransform.key)
  }

  def deceasedReads: PartialFunction[JsObject, JsResult[DeltaTransform]] = {
    readsForTransform[DeceasedTransform](DeceasedTransform.key)
  }

  def agentDetailsReads: PartialFunction[JsObject, JsResult[DeltaTransform]] = {
    readsForTransform[AgentDetailsTransform](AgentDetailsTransform.key)
  }

  def amountTaxOwedReads: PartialFunction[JsObject, JsResult[DeltaTransform]] = {
    readsForTransform[AmountOfTaxOwedTransform](AmountOfTaxOwedTransform.key)
  }

  def yearsReturnsReads: PartialFunction[JsObject, JsResult[DeltaTransform]] = {
    readsForTransform[YearsReturnsTransform](YearsReturnsTransform.key)
  }

  def addBusinessPersonalRepReads: PartialFunction[JsObject, JsResult[DeltaTransform]] = {
    readsForTransform[AddBusinessPersonalRepTransform](AddBusinessPersonalRepTransform.key)
  }

  def amendIndividualPersonalRepReads: PartialFunction[JsObject, JsResult[DeltaTransform]] = {
    readsForTransform[AmendIndividualPersonalRepTransform](AmendIndividualPersonalRepTransform.key)
  }

  def amendBusinessPersonalRepReads: PartialFunction[JsObject, JsResult[DeltaTransform]] = {
    readsForTransform[AmendBusinessPersonalRepTransform](AmendBusinessPersonalRepTransform.key)
  }

  implicit val reads: Reads[DeltaTransform] = Reads[DeltaTransform](
    value =>
      (
        personalRepReads orElse
          agentDetailsReads orElse
          deceasedReads orElse
          amountTaxOwedReads orElse
          correspondenceNameReads orElse
          yearsReturnsReads orElse
          addBusinessPersonalRepReads orElse
          amendIndividualPersonalRepReads orElse
          amendBusinessPersonalRepReads orElse
          readsForTransform[DeceasedTransform](DeceasedTransform.key)
      )(value.as[JsObject]) orElse (throw new Exception(s"Don't know how to deserialise transform"))
  )

  def personalRepWrites[T <: DeltaTransform] : PartialFunction[T, JsValue] = {
    case transform: PersonalRepTransform =>
      Json.obj(PersonalRepTransform.key -> Json.toJson(transform)(PersonalRepTransform.format))
  }

  def correspondenceNameWrites[T <: DeltaTransform] : PartialFunction[T, JsValue] = {
    case transform: CorrespondenceNameTransform =>
      Json.obj(CorrespondenceNameTransform.key -> Json.toJson(transform)(CorrespondenceNameTransform.format))
  }

  def yearsReturnsWrites[T <: DeltaTransform] : PartialFunction[T, JsValue] = {
    case transform: YearsReturnsTransform =>
      Json.obj(YearsReturnsTransform.key -> Json.toJson(transform)(YearsReturnsTransform.format))
  }

  def agentDetailsWrites[T <: DeltaTransform] : PartialFunction[T, JsValue] = {
    case transform: AgentDetailsTransform =>
      Json.obj(AgentDetailsTransform.key -> Json.toJson(transform)(AgentDetailsTransform.format))
  }

  def amountOfTaxOwedWrites[T <: DeltaTransform] : PartialFunction[T, JsValue] = {
    case transform: AmountOfTaxOwedTransform =>
      Json.obj(AmountOfTaxOwedTransform.key -> Json.toJson(transform)(AmountOfTaxOwedTransform.format))
  }

  def deceasedWrites[T <: DeltaTransform] : PartialFunction[T, JsValue] = {
    case transform: DeceasedTransform =>
      Json.obj(DeceasedTransform.key -> Json.toJson(transform)(DeceasedTransform.format))
  }

  def addBusinessPersonalRepWrites[T <: DeltaTransform] : PartialFunction[T, JsValue] = {
    case transform: AddBusinessPersonalRepTransform =>
      Json.obj(AddBusinessPersonalRepTransform.key -> Json.toJson(transform)(AddBusinessPersonalRepTransform.format))
  }

  def amendIndividualPersonalRepWrites[T <: DeltaTransform] : PartialFunction[T, JsValue] = {
    case transform: AmendIndividualPersonalRepTransform =>
      Json.obj(AmendIndividualPersonalRepTransform.key -> Json.toJson(transform)(AmendIndividualPersonalRepTransform.format))
  }

  def amendBusinessPersonalRepWrites[T <: DeltaTransform] : PartialFunction[T, JsValue] = {
    case transform: AmendBusinessPersonalRepTransform =>
      Json.obj(AmendBusinessPersonalRepTransform.key -> Json.toJson(transform)(AmendBusinessPersonalRepTransform.format))
  }

  def defaultWrites[T <: DeltaTransform]: PartialFunction[T, JsValue] = {
    case transform => throw new Exception(s"Don't know how to serialise transform - $transform")
  }

  implicit val writes: Writes[DeltaTransform] = Writes[DeltaTransform] { deltaTransform =>
    (
      personalRepWrites orElse
        agentDetailsWrites orElse
        amountOfTaxOwedWrites orElse
        deceasedWrites orElse
        correspondenceNameWrites orElse
        yearsReturnsWrites orElse
        addBusinessPersonalRepWrites orElse
        amendIndividualPersonalRepWrites orElse
        amendBusinessPersonalRepWrites orElse
        defaultWrites
      ).apply(deltaTransform)
  }

}

case class ComposedDeltaTransform(deltaTransforms: Seq[DeltaTransform]) extends DeltaTransform {
  override def applyTransform(input: JsValue): JsResult[JsValue] = {
    deltaTransforms.foldLeft[JsResult[JsValue]](JsSuccess(input))((cur, xform) => cur.flatMap(xform.applyTransform))
  }

  override def applyDeclarationTransform(input: JsValue): JsResult[JsValue] = {
    deltaTransforms.foldLeft[JsResult[JsValue]](JsSuccess(input))((cur, xform) => cur.flatMap(xform.applyDeclarationTransform))
  }

  def :+(transform: DeltaTransform): ComposedDeltaTransform = ComposedDeltaTransform(deltaTransforms :+ transform)
}

object ComposedDeltaTransform {
  implicit val format: Format[ComposedDeltaTransform] = Json.format[ComposedDeltaTransform]
}
