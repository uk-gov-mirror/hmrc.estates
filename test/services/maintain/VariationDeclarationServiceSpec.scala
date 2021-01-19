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

package services.maintain

import java.time.LocalDate

import org.scalatest.{FreeSpec, MustMatchers, OptionValues}
import models._
import models.getEstate.{GetEstateProcessedResponse, GetEstateResponse}
import services.LocalDateService
import utils.JsonUtils

class VariationDeclarationServiceSpec extends FreeSpec with MustMatchers with OptionValues {

  object LocalDateServiceStub extends LocalDateService {
    override def now: LocalDate = LocalDate.of(2020, 5, 10)
  }

  "the declaration transformer should" - {

    val declaration = DeclarationName(NameType("First", None, "Last"))
    val declarationForApi = DeclarationForApi(declaration, None)

    "build submission json successfully where individual personal representative has not changed" in {
      val cached = JsonUtils
        .getJsonValueFromFile("etmp/valid-get-estate-4mld-response.json")
        .as[GetEstateResponse].asInstanceOf[GetEstateProcessedResponse]

      val afterJson = JsonUtils.getJsonValueFromFile("transformed/variations/estates-etmp-sent-no-change.json")
      val transformer = new VariationDeclarationService(LocalDateServiceStub)

      val result = transformer.transform(
        cached.getEstate, cached.responseHeader, cached.getEstate, declarationForApi
      )
      result.asOpt.value mustBe afterJson
    }

    "build submission json successfully where org personal rep has not changed" in {
      val beforeJson = JsonUtils.getJsonValueFromFile("etmp/valid-get-estate-response-org-personal-rep.json")

      val cached = beforeJson.as[GetEstateResponse].asInstanceOf[GetEstateProcessedResponse]

      val afterJson = JsonUtils.getJsonValueFromFile("transformed/variations/estates-etmp-sent-org-personal-rep.json")
      val transformer = new VariationDeclarationService(LocalDateServiceStub)

      val result = transformer.transform(cached.getEstate, cached.responseHeader, cached.getEstate, declarationForApi)
      result.asOpt.value mustBe afterJson
    }

    "build submission json successfully where personal representative has changed (individual to new individual)" in {
      val beforeJson = JsonUtils.getJsonValueFromFile("etmp/valid-get-estate-4mld-response.json")

      val amendedDocWithAmendedPerRepTransform = JsonUtils
        .getJsonValueFromFile("etmp/valid-get-estate-response-with-amended-personal-rep.json")
        .as[GetEstateResponse].asInstanceOf[GetEstateProcessedResponse]
        .getEstate

      val cached = beforeJson.as[GetEstateResponse].asInstanceOf[GetEstateProcessedResponse]

      val afterJson = JsonUtils.getJsonValueFromFile("transformed/variations/estates-etmp-sent-new-ind-personal-rep.json")
      val transformer = new VariationDeclarationService(LocalDateServiceStub)

      val result = transformer.transform(
        amendedDocWithAmendedPerRepTransform, cached.responseHeader, cached.getEstate, declarationForApi
      )
      result.asOpt.value mustBe afterJson
    }

    "build submission json successfully where personal representative has been updated, telephone and address (continuous start date)" in {
      val beforeJson = JsonUtils.getJsonValueFromFile("etmp/valid-get-estate-4mld-response.json")

      val amendedDocWithAmendedPerRepTransform = JsonUtils
        .getJsonValueFromFile("etmp/valid-get-estate-response-with-updated-individual-personal-rep.json")
        .as[GetEstateResponse].asInstanceOf[GetEstateProcessedResponse]
        .getEstate

      val cached = beforeJson.as[GetEstateResponse].asInstanceOf[GetEstateProcessedResponse]

      val afterJson = JsonUtils.getJsonValueFromFile("transformed/variations/estates-etmp-sent-updated-ind-personal-rep.json")
      val transformer = new VariationDeclarationService(LocalDateServiceStub)

      val result = transformer.transform(
        amendedDocWithAmendedPerRepTransform, cached.responseHeader, cached.getEstate, declarationForApi
      )
      result.asOpt.value mustBe afterJson
    }

    "build submission json successfully where personal representative has changed (individual to business)" in {
      val beforeJson = JsonUtils.getJsonValueFromFile("etmp/valid-get-estate-4mld-response.json")

      val amendedDocWithAmendedPerRepTransform = JsonUtils
        .getJsonValueFromFile("etmp/valid-get-estate-response-with-new-business-rep.json")
        .as[GetEstateResponse].asInstanceOf[GetEstateProcessedResponse]
        .getEstate

      val cached = beforeJson.as[GetEstateResponse].asInstanceOf[GetEstateProcessedResponse]

      val afterJson =
        JsonUtils.getJsonValueFromFile("transformed/variations/estates-etmp-sent-change-ind-to-business-personal-rep.json")

      val transformer = new VariationDeclarationService(LocalDateServiceStub)

      val result = transformer.transform(
        amendedDocWithAmendedPerRepTransform, cached.responseHeader, cached.getEstate, declarationForApi
      )
      result.asOpt.value mustBe afterJson
    }

    "build submission json for an individual personal representative with an agent" in {
      val agentDetails = AgentDetails(
        "arn",
        "agent name",
        AddressType("Line1", "Line2", Some("Line3"), None, Some("POSTCODE"), "GB"),
        "01234567890",
        "client-ref"
      )

      val declarationForApi = DeclarationForApi(declaration, Some(agentDetails))
      val beforeJson = JsonUtils.getJsonValueFromFile("etmp/valid-get-estate-4mld-response.json")
      val cached = beforeJson.as[GetEstateResponse].asInstanceOf[GetEstateProcessedResponse]
      val afterJson = JsonUtils.getJsonValueFromFile("transformed/variations/estates-etmp-sent-with-agent-details.json")
      val transformer = new VariationDeclarationService(LocalDateServiceStub)

      val result = transformer.transform(cached.getEstate, cached.responseHeader, cached.getEstate, declarationForApi)
      result.asOpt.value mustBe afterJson
    }
  }
}
