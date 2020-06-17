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
import uk.gov.hmrc.estates.models.{EstatePerRepIndType, EstatePerRepOrgType}
import uk.gov.hmrc.estates.models.JsonWithoutNulls._
import uk.gov.hmrc.estates.utils.JsonOps._

case class AddEstatePerRepTransform(
                                     newPersonalIndRep: Option[EstatePerRepIndType],
                                     newPersonalOrgRep: Option[EstatePerRepOrgType]
                                   )
  extends DeltaTransform with JsonOperations {

  private lazy val path = __ \ 'estate \ 'entities \ 'personalRepresentative

  private lazy val correspondencePath = __ \ 'correspondence

  override def applyTransform(input: JsValue): JsResult[JsValue] = {

    val newPersonalRep = Json.obj(
      "estatePerRepInd" -> newPersonalIndRep,
      "estatePerRepOrg" -> newPersonalOrgRep
    ).withoutNulls.applyRules

    input.transform(
      path.json.prune andThen
        __.json.update(path.json.put(Json.toJson(removeIsPassportField(newPersonalRep))))
    )
  }

  override def applyDeclarationTransform(input: JsValue): JsResult[JsValue] = {
    this match {
      case AddEstatePerRepTransform(Some(newPersonalIndRep), None) =>
        val telephone = newPersonalIndRep.phoneNumber
        newPersonalIndRep.identification.address match {
          case Some(address) =>
            val isAbroad = address.postCode.isEmpty

            input.transform(
              __.json.update(
                correspondencePath.json.put {
                  Json.obj(
                    "abroadIndicator" -> isAbroad,
                    "address" -> address,
                    "phoneNumber" -> telephone
                  )
                }
              )
            )

          case None =>
            JsError("No address on personal rep individual to apply to correspondence")
        }
      case AddEstatePerRepTransform(None, Some(newPersonalOrgRep)) =>
        super.applyDeclarationTransform(input)
      case _ =>
        super.applyDeclarationTransform(input)
    }
  }

  private def removeIsPassportField(original: JsValue): JsValue = {
    val isPassportPath = __ \ 'estatePerRepInd \ 'identification \ 'passport \ 'isPassport

    original.transform(isPassportPath.json.prune) match {
      case JsSuccess(updated, _) => updated
      case JsError(_) => original
    }
  }
}

object AddEstatePerRepTransform {

  val key = "AddEstatePerRepTransform"

  implicit val format: Format[AddEstatePerRepTransform] = Json.format[AddEstatePerRepTransform]
}
