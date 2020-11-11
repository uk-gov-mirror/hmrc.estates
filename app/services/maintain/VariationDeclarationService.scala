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

package services.maintain

import javax.inject.Inject
import play.api.Logging
import play.api.libs.json.Reads._
import play.api.libs.json._
import models._
import models.getEstate.ResponseHeader
import services.LocalDateService

class VariationDeclarationService @Inject()(localDateService: LocalDateService) extends Logging {

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

  def transform(amendDocument: JsValue,
                responseHeader: ResponseHeader,
                cachedDocument: JsValue,
                declaration: DeclarationForApi): JsResult[JsValue] = {
    logger.debug(s"[VariationDeclarationService] applying declaration transforms to document $amendDocument from cached $cachedDocument")

    amendDocument.transform(
      (__ \ 'applicationType).json.prune andThen
        (__ \ 'declaration).json.prune andThen
        (__ \ 'yearsReturns).json.prune andThen
        updateCorrespondence(amendDocument) andThen
        removePersonalRepAddressIfHasNinoOrUtr(amendDocument, pathToPersonalRep) andThen
        convertPersonalRepresentativeToArray(amendDocument) andThen
        endPreviousPersonalRepIfChanged(amendDocument, cachedDocument) andThen
        putNewValue(__ \ 'reqHeader \ 'formBundleNo, JsString(responseHeader.formBundleNo)) andThen
        addDeclaration(declaration, amendDocument) andThen
        addAgentIfDefined(declaration.agentDetails)
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

  private def addPreviousPersonalRepAsExpiredStep(previousPersonalRepJson: JsValue, date: JsValue): Reads[JsObject] = {
    val personalRepField = determinePersonalRepField(__, previousPersonalRepJson)

    previousPersonalRepJson.transform(__.json.update {
      logger.info(s"[addPreviousPersonalRepAsExpiredStep] setting end date on original personal representative")
      (__ \ 'entityEnd).json.put(date)
    }).fold(
      errors => {
        logger.error(s"[addPreviousPersonalRepAsExpiredStep] unable to set end date on original personal representative")
        Reads(_ => JsError(errors))
      },
      endedJson => {
        logger.info(s"[addPreviousPersonalRepAsExpiredStep]" +
          s" ended old personal representative, adding them to personal representative array")
        pathToPersonalRep.json.update(of[JsArray]
          .map { a => a :+ Json.obj(personalRepField -> endedJson) })
      })
  }

  private def endPreviousPersonalRepIfChanged(newJson: JsValue, originalJson: JsValue): Reads[JsObject] = {
    val newPersonalRep = newJson.transform(pickPersonalRep)
    val originalPersonalRep = originalJson.transform(pickPersonalRep)

    (newPersonalRep, originalPersonalRep) match {
      case (JsSuccess(newPersonalRepJson, _), JsSuccess(originalPersonalRepJson, _)) if newPersonalRepJson != originalPersonalRepJson =>
        logger.info(s"[endPreviousPersonalRepIfChanged] personal representative has changed")

        val startDateReads = (__ \ "entityStart").json.pick

        (for {
          previousPersonalRepWithAddressRemoved <- originalPersonalRepJson.transform {
            removePersonalRepAddressIfHasNinoOrUtr(originalPersonalRepJson, __)
          }
          originalStartDate <- previousPersonalRepWithAddressRemoved.transform(startDateReads)
          newStartDate <- newPersonalRepJson.transform(startDateReads)
        } yield {
          val startDateHasChanged = originalStartDate != newStartDate
          if (startDateHasChanged) {
            addPreviousPersonalRepAsExpiredStep(previousPersonalRepWithAddressRemoved, newStartDate)
          } else {
            addPreviousPersonalRepAsExpiredStep(previousPersonalRepWithAddressRemoved, Json.toJson(localDateService.now))
          }
        }).getOrElse(Reads(_ => JsError.apply("[endPreviousPersonalRepIfChanged] unable to end previous personal representative")))
      case _ =>
        logger.info(s"[endPreviousPersonalRepIfChanged] personal representative has not changed")
        __.json.pick[JsObject]
    }
  }

  private def putNewValue(path: JsPath, value: JsValue ): Reads[JsObject] = __.json.update(path.json.put(value))

  private def declarationAddress(agentDetails: Option[AgentDetails],
                                 amendJson: JsValue) : JsResult[AddressType] = agentDetails match {
      case Some(x) =>
        logger.info(s"[declarationAddress] using agents address as declaration")
        JsSuccess(x.agentAddress)
      case None =>
        logger.info(s"[declarationAddress] using personal representatives address as declaration")
        amendJson.transform((pathToPersonalRep \ 'identification \ 'address).json.pick).flatMap(_.validate[AddressType])
    }

  private def addDeclaration(declarationForApi: DeclarationForApi, amendJson: JsValue) : Reads[JsObject] = {
    declarationAddress(declarationForApi.agentDetails, amendJson) match {
      case JsSuccess(value, _) =>
        val declarationToSend = Declaration(declarationForApi.declaration.name, value)
        putNewValue(declarationPath, Json.toJson(declarationToSend))
      case e : JsError =>
        logger.error(s"[addDeclaration] unable to set declaration address," +
          s" hadAgent: ${declarationForApi.agentDetails.isDefined}, due to ${e.errors}")
        Reads(_ => e)
    }
  }

  private def addAgentIfDefined(agentDetails: Option[AgentDetails]) : Reads[JsObject] = agentDetails match {
      case Some(x) =>
        __.json.update(agentPath.json.put(Json.toJson(x)))
      case None =>
        __.json.pick[JsObject]
    }

}
