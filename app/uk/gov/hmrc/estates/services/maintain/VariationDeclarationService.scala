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

package uk.gov.hmrc.estates.services.maintain

import java.time.LocalDate

import play.api.Logger
import play.api.libs.json.Reads._
import play.api.libs.json._
import uk.gov.hmrc.estates.models._
import uk.gov.hmrc.estates.models.getEstate.GetEstateProcessedResponse

class VariationDeclarationService {

  private lazy val pathToEntities: JsPath = __ \ 'details \ 'estate \ 'entities
  private lazy val pathToPersonalRep: JsPath = pathToEntities \ 'personalRepresentative
  private lazy val pathToPersonalRepAddress = pathToPersonalRep \ 'identification \ 'address
  private lazy val pathToPersonalRepPhoneNumber = pathToPersonalRep \ 'phoneNumber
  private lazy val pathToPersonalRepCountry = pathToPersonalRepAddress \ 'country
  private lazy val pathToCorrespondenceAddress = __ \ 'correspondence \ 'address
  private lazy val pathToCorrespondencePhoneNumber = __ \ 'correspondence \ 'phoneNumber
  private lazy val pickPersonalRep = pathToPersonalRep.json.pick
  private lazy val declarationPath = __ \ 'declaration
  private lazy val agentPath = __ \ 'agentDetails

  def transform(workingDocument: GetEstateProcessedResponse,
                cachedDocument: JsValue,
                declaration: DeclarationForApi,
                date: LocalDate): JsResult[JsValue] = {

    val amendJson = workingDocument.getEstate
    val responseHeader = workingDocument.responseHeader

    Logger.debug(s"[VariationDeclarationService] applying declaration transforms to document $workingDocument from cached $cachedDocument")

    amendJson.transform(
      (__ \ 'applicationType).json.prune andThen
        (__ \ 'declaration).json.prune andThen
        (__ \ 'yearsReturns).json.prune andThen
        updateCorrespondence(amendJson) andThen
        removePersonalRepAddressIfHasNinoOrUtr(amendJson, pathToPersonalRep) andThen
        convertPersonalRepresentativeToArray(amendJson) andThen
        endPreviousPersonalRepIfChanged(amendJson, cachedDocument, date) andThen
        putNewValue(__ \ 'reqHeader \ 'formBundleNo, JsString(responseHeader.formBundleNo)) andThen
        addDeclaration(declaration, amendJson) andThen
        addAgentIfDefined(declaration.agentDetails) andThen
        addEndDateIfDefined(declaration.endDate)
    )
  }

  private def updateCorrespondence(responseJson: JsValue): Reads[JsObject] = {

    val personalRepCountry = responseJson.transform(pathToPersonalRepCountry.json.pick)
    val inUk = personalRepCountry.isError || personalRepCountry.asOpt.contains(JsString("GB"))

    pathToCorrespondenceAddress.json.prune andThen
      pathToCorrespondencePhoneNumber.json.prune andThen
      putNewValue(__ \ 'correspondence \ 'abroadIndicator, JsBoolean(!inUk)) andThen
      __.json.update(pathToCorrespondenceAddress.json.copyFrom(pathToPersonalRepAddress.json.pick)) andThen
      __.json.update(pathToCorrespondencePhoneNumber.json.copyFrom(pathToPersonalRepPhoneNumber.json.pick))
  }

  private def convertPersonalRepresentativeToArray(json: JsValue): Reads[JsObject] = {
    pathToPersonalRep.json.update(of[JsObject]
      .map{ a => Json.arr(Json.obj(determinePersonalRepField(pathToPersonalRep, json) -> a )) })
  }

  private def removePersonalRepAddressIfHasNinoOrUtr(personalRepJson: JsValue, personalRepPath: JsPath) : Reads[JsObject] = {

    val hasField = (field: String) =>
      personalRepJson.transform((personalRepPath \ "identification" \ field).json.pick).isSuccess

    val hasUtr = hasField("utr")
    val hasNino = hasField("nino")

    if (hasUtr || hasNino) {
      (personalRepPath \ 'identification \ 'address).json.prune
    } else {
      __.json.pick[JsObject]
    }
  }

  private def determinePersonalRepField(rootPath: JsPath, json: JsValue): String = {
    val namePath = (rootPath \ 'name).json.pick

    if(json.transform(namePath).flatMap(_.validate[NameType]).isSuccess) {
      "estatePerRepInd"
    } else {
      "estatePerRepOrg"
    }
  }

  private def addPreviousPersonalRepAsExpiredStep(previousPersonalRepJson: JsValue, date: LocalDate): Reads[JsObject] = {
    val personalRepField = determinePersonalRepField(__, previousPersonalRepJson)

    previousPersonalRepJson.transform(__.json.update {
      Logger.info(s"[VariationDeclarationService] setting end date on original personal representative")
      (__ \ 'entityEnd).json.put(Json.toJson(date))
    }).fold(
      errors => {
        Logger.error(s"[VariationDeclarationService] unable to set end date on original personal representative")
        Reads(_ => JsError(errors))
      },
      endedJson => {
        Logger.info(s"[VariationDeclarationService] ended old personal representative, adding them to personal representative array")
        pathToPersonalRep.json.update(of[JsArray]
          .map { a => a :+ Json.obj(personalRepField -> endedJson) })
      })
  }

  private def endPreviousPersonalRepIfChanged(newJson: JsValue, originalJson: JsValue, date: LocalDate): Reads[JsObject] = {
    val newPersonalRep = newJson.transform(pickPersonalRep)
    val originalPersonalRep = originalJson.transform(pickPersonalRep)

    (newPersonalRep, originalPersonalRep) match {
      case (JsSuccess(newPersonalRepJson, _), JsSuccess(originalPersonalRepJson, _)) if (newPersonalRepJson != originalPersonalRepJson) =>
        Logger.info(s"[VariationDeclarationService] Personal representative has changed")
        val fixPersonalRepReads = removePersonalRepAddressIfHasNinoOrUtr(originalPersonalRepJson, __)
        originalPersonalRepJson.transform(fixPersonalRepReads) match {
          case JsSuccess(value, _) =>
            Logger.info(s"[VariationDeclarationService] Restored personal representative to original state, removed address")
            addPreviousPersonalRepAsExpiredStep(value, date)
          case e: JsError =>
            Logger.error(s"[VariationDeclarationService] Unable to restore personal representative to original state")
            Reads(_ => e)
        }
      case _ =>
        Logger.info(s"[VariationDeclarationService] Personal representative has not changed")
        __.json.pick[JsObject]
    }
  }

  private def putNewValue(path: JsPath, value: JsValue ): Reads[JsObject] = __.json.update(path.json.put(value))

  private def declarationAddress(agentDetails: Option[AgentDetails],
                                 responseJson: JsValue) : JsResult[AddressType] = agentDetails match {
      case Some(x) =>
        JsSuccess(x.agentAddress)
      case None =>
        responseJson.transform((pathToPersonalRep \ 'identification \ 'address).json.pick).flatMap(_.validate[AddressType])
    }

  private def addDeclaration(declarationForApi: DeclarationForApi, responseJson: JsValue) : Reads[JsObject] = {
    declarationAddress(declarationForApi.agentDetails, responseJson) match {
      case JsSuccess(value, _) =>
        val declarationToSend = Declaration(declarationForApi.declaration.name, value)
        putNewValue(declarationPath, Json.toJson(declarationToSend))
      case e : JsError =>
        Logger.error(s"[VariationDeclarationService] unable to set declaration address, hadAgent: ${declarationForApi.agentDetails.isDefined}")
        Reads(_ => e)
    }
  }

  private def addAgentIfDefined(agentDetails: Option[AgentDetails]) : Reads[JsObject] = agentDetails match {
      case Some(x) =>
        __.json.update(agentPath.json.put(Json.toJson(x)))
      case None =>
        __.json.pick[JsObject]
    }

  private def addEndDateIfDefined(endDate: Option[LocalDate]): Reads[JsObject] = endDate match {
      case Some(date) =>
        __.json.update((__ \ 'trustEndDate).json.put(Json.toJson(date)))
      case _ =>
        __.json.pick[JsObject]
    }

}
