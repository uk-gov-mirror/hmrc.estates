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

import java.time.LocalDate

import play.api.Logger
import play.api.libs.json.Reads._
import play.api.libs.json._
import uk.gov.hmrc.estates.models._
import uk.gov.hmrc.estates.models.getEstate.GetEstateProcessedResponse

class VariationDeclarationTransform {

  private val pathToEntities: JsPath = __ \ 'details \ 'estate \ 'entities
  private val pathToPersonalRep: JsPath =  pathToEntities \ 'personalRepresentative
  private val pathToPersonalRepAddress = pathToPersonalRep \ 'identification \ 'address
  private val pathToPersonalRepPhoneNumber = pathToPersonalRep \ 'phoneNumber
  private val pathToPersonalRepCountry = pathToPersonalRepAddress \ 'country
  private val pathToCorrespondenceAddress = __ \ 'correspondence \ 'address
  private val pathToCorrespondencePhoneNumber = __ \ 'correspondence \ 'phoneNumber
  private val pickPersonalRep = pathToPersonalRep.json.pick

  def transform(workingDocument: GetEstateProcessedResponse,
                cachedDocument: JsValue,
                declaration: DeclarationForApi,
                date: LocalDate): JsResult[JsValue] = {

    val amendJson = workingDocument.getEstate
    val responseHeader = workingDocument.responseHeader

    Logger.debug(s"[VariationDeclarationTransform] applying declaration transforms to document $workingDocument from cached $cachedDocument")

    amendJson.transform(
      (__ \ 'applicationType).json.prune andThen
        (__ \ 'declaration).json.prune andThen
        (__ \ 'yearsReturns).json.prune andThen
        updateCorrespondence(amendJson) andThen
        fixPersonalRepAddress(amendJson, pathToPersonalRep) andThen
        addPreviousPersonalRep(amendJson, cachedDocument, date) andThen
        putNewValue(__ \ 'reqHeader \ 'formBundleNo, JsString(responseHeader.formBundleNo)) andThen
        addDeclaration(declaration, amendJson) andThen
        addAgentIfDefined(declaration.agentDetails) andThen
        addEndDateIfDefined(declaration.endDate)
    )
  }

  private def updateCorrespondence(responseJson: JsValue): Reads[JsObject] = {
    val personalRepCountry = responseJson.transform(pathToPersonalRepCountry.json.pick)
    val inUk = personalRepCountry.isError || personalRepCountry.get == JsString("GB")
    pathToCorrespondenceAddress.json.prune andThen
      pathToCorrespondencePhoneNumber.json.prune andThen
      putNewValue(__ \ 'correspondence \ 'abroadIndicator, JsBoolean(!inUk)) andThen
      __.json.update(pathToCorrespondenceAddress.json.copyFrom(pathToPersonalRepAddress.json.pick)) andThen
      __.json.update(pathToCorrespondencePhoneNumber.json.copyFrom(pathToPersonalRepPhoneNumber.json.pick))
  }

  private def fixPersonalRepAddress(personalRepJson: JsValue, personalRepPath: JsPath) = {

    val hasField = (field: String) =>
      personalRepJson.transform((personalRepPath \ "identification" \ field).json.pick).isSuccess

    val hasUtr = hasField("utr")
    val hasNino = hasField("nino")

    if (hasUtr || hasNino) {
      (personalRepPath \ 'identification \ 'address).json.prune
    } else {
      __.json.pick
    }
  }

  private def determinePersonalRepField(rootPath: JsPath, json: JsValue): String = {
    val namePath = (rootPath \ 'name).json.pick[JsObject]

    json.transform(namePath).flatMap(_.validate[NameType]) match {
      case JsSuccess(_, _) => "estatePerRepInd"
      case _ => "estatePerRepOrg"
    }
  }

  private def addPreviousPersonalRepAsExpiredStep(previousPersonalRepJson: JsValue, date: LocalDate): Reads[JsObject] = {
    val personalRepField = determinePersonalRepField(__, previousPersonalRepJson)

    previousPersonalRepJson.transform(__.json.update(
      (__ \ 'entityEnd).json.put(Json.toJson(date))
    )).fold(
      errors => Reads(_ => JsError(errors)),
      endedJson => {
        pathToPersonalRep.json.update(of[JsArray]
          .map { a => a :+ Json.obj(personalRepField -> endedJson) })
      })
  }

  private def addPreviousPersonalRep(newJson: JsValue, originalJson: JsValue, date: LocalDate): Reads[JsObject] = {
    val newPersonalRep = newJson.transform(pickPersonalRep)
    val originalPersonalRep = originalJson.transform(pickPersonalRep)

    (newPersonalRep, originalPersonalRep) match {
      case (JsSuccess(newPersonalRepJson, _), JsSuccess(originalPersonalRepJson, _)) if (newPersonalRepJson != originalPersonalRepJson) =>
        Logger.info(s"[VariationDeclarationTransform] ")
        val reads = fixPersonalRepAddress(originalPersonalRepJson, __)
        originalPersonalRepJson.transform(reads) match {
          case JsSuccess(value, _) => addPreviousPersonalRepAsExpiredStep(value, date)
          case e: JsError => Reads(_ => e)
        }
      case _ => __.json.pick[JsObject]
    }
  }

  private def putNewValue(path: JsPath, value: JsValue ): Reads[JsObject] =
    __.json.update(path.json.put(value))

  private def declarationAddress(agentDetails: Option[AgentDetails], responseJson: JsValue) =
    if (agentDetails.isDefined) {
      agentDetails.get.agentAddress
    } else {
      responseJson.transform((pathToPersonalRep \ 'identification \ 'address).json.pick) match {
        case JsSuccess(value, _) => value.as[AddressType]
        case JsError(_) => ???
      }
    }

  private def addDeclaration(declarationForApi: DeclarationForApi, responseJson: JsValue) = {
    val declarationToSend = Declaration(
      declarationForApi.declaration.name,
      declarationAddress(declarationForApi.agentDetails, responseJson)
    )
    putNewValue(__ \ 'declaration, Json.toJson(declarationToSend))
  }

  private def addAgentIfDefined(agentDetails: Option[AgentDetails]) = if (agentDetails.isDefined) {
    __.json.update(
      (__ \ 'agentDetails).json.put(Json.toJson(agentDetails.get))
    )
  } else {
    __.json.pick[JsObject]
  }

  private def addEndDateIfDefined(endDate: Option[LocalDate]) = {
    endDate match {
      case Some(date) =>
        __.json.update(
          (__ \ 'trustEndDate).json.put(Json.toJson(date))
        )
      case _ =>
        __.json.pick[JsObject]
    }
  }

}
