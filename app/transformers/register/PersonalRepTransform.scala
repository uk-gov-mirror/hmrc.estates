/*
 * Copyright 2021 HM Revenue & Customs
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

package transformers.register

import play.api.libs.json.{JsPath, _}
import models.JsonWithoutNulls._
import models.{AddressType, EstatePerRepIndType, EstatePerRepOrgType}
import transformers.JsonOperations
import utils.JsonOps._

case class PersonalRepTransform(
                                     newPersonalIndRep: Option[EstatePerRepIndType],
                                     newPersonalOrgRep: Option[EstatePerRepOrgType]
                                   )
  extends SetValueAtPathDeltaTransform with JsonOperations {

  override val path: JsPath = __ \ 'estate \ 'entities \ 'personalRepresentative

  private lazy val correspondencePath = __ \ 'correspondence

  override val value: JsValue = Json.obj(
    "estatePerRepInd" -> newPersonalIndRep,
    "estatePerRepOrg" -> newPersonalOrgRep
  ).withoutNulls

  def transformCorrespondence(input: JsValue, address: Option[AddressType], telephoneNumber: String): JsResult[JsObject] =
    address match {
      case Some(address) =>
        val isAbroad = address.postCode.isEmpty
        input.transform(
          __.json.update(
            correspondencePath.json.put {
              Json.obj(
                "abroadIndicator" -> isAbroad,
                "address" -> address,
                "phoneNumber" -> telephoneNumber
              )
            }
          )
        )
      case None =>
        JsError("No address on personal rep to apply to correspondence")
    }

  override def applyDeclarationTransform(input: JsValue): JsResult[JsValue] = this match {
      case PersonalRepTransform(Some(newPersonalIndRep), None) =>
        for {
          inputWithCorrespondence <- transformCorrespondence(input, newPersonalIndRep.identification.address, newPersonalIndRep.phoneNumber)
          inputWithAddressCorrected <- {
            if (input.transform((path \ 'estatePerRepInd \ 'identification \ 'nino).json.pick).isSuccess) {
              inputWithCorrespondence.transform((path \ 'estatePerRepInd \ 'identification \ 'address).json.prune)
            } else {
              JsSuccess(inputWithCorrespondence)
            }
          }
        } yield removeIsPassportField(inputWithAddressCorrected).applyRules

      case PersonalRepTransform(None, Some(newPersonalOrgRep)) =>
        for {
          inputWithCorrespondence <- transformCorrespondence(input, newPersonalOrgRep.identification.address, newPersonalOrgRep.phoneNumber)
          inputWithAddressCorrected <- {

            if (input.transform((path \ 'estatePerRepOrg \ 'identification \ 'utr).json.pick).isSuccess) {
              inputWithCorrespondence.transform((path \ 'estatePerRepOrg \ 'identification \ 'address).json.prune)
            } else {
              JsSuccess(inputWithCorrespondence)
            }
          }
        } yield inputWithAddressCorrected.applyRules

      case _ =>
        super.applyDeclarationTransform(input)
    }

  private def removeIsPassportField(original: JsValue): JsValue = {
    val isPassportPath = path \ 'estatePerRepInd \ 'identification \ 'passport \ 'isPassport

    original.transform(isPassportPath.json.prune) match {
      case JsSuccess(updated, _) => updated
      case JsError(_) => original
    }
  }
}

object PersonalRepTransform {

  val key = "AddEstatePerRepTransform"

  implicit val format: Format[PersonalRepTransform] = Json.format[PersonalRepTransform]
}
