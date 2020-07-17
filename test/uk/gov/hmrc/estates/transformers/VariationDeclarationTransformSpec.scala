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

import java.time.LocalDate

import org.scalatest.{FreeSpec, MustMatchers, OptionValues}
import uk.gov.hmrc.estates.models._
import uk.gov.hmrc.estates.models.getEstate.{GetEstateProcessedResponse, GetEstateResponse}
import uk.gov.hmrc.estates.transformers.register.VariationDeclarationTransform
import uk.gov.hmrc.estates.utils.JsonUtils

class VariationDeclarationTransformSpec extends FreeSpec with MustMatchers with OptionValues {

  val entityEnd = LocalDate.of(2020, 1, 30)

  "the declaration transformer should" - {

    val declaration = DeclarationName(NameType("First", None, "Last"))
    val declarationForApi = DeclarationForApi(declaration, None, None)

    "transform json successfully for an individual personal rep" in {
      val beforeJson = JsonUtils.getJsonValueFromFile("etmp/valid-get-estate-response.json")
      val estateResponse = beforeJson.as[GetEstateResponse].asInstanceOf[GetEstateProcessedResponse]
      val afterJson = JsonUtils.getJsonValueFromFile("transformed/variations/estates-etmp-sent.json")
      val transformer = new VariationDeclarationTransform

      val result = transformer.transform(estateResponse, estateResponse.getEstate, declarationForApi, entityEnd)
      result.asOpt.value mustBe afterJson
    }

    "transform json successfully for an org personal rep" in {
      val beforeJson = JsonUtils.getJsonValueFromFile("etmp/valid-get-estate-response-org-personal-rep.json")
      val estateResponse = beforeJson.as[GetEstateResponse].asInstanceOf[GetEstateProcessedResponse]
      val afterJson = JsonUtils.getJsonValueFromFile("transformed/variations/estates-etmp-sent-org-personal-rep.json")
      val transformer = new VariationDeclarationTransform

      val result = transformer.transform(estateResponse, estateResponse.getEstate, declarationForApi, entityEnd)
      result.asOpt.value mustBe afterJson
    }

    "transform json successfully for an individual personal rep with agent details" in {
      val agentDetails = AgentDetails(
        "arn",
        "agent name",
        AddressType("Line1", "Line2", Some("Line3"), None, Some("POSTCODE"), "GB"),
        "01234567890",
        "client-ref"
      )

      val declarationForApi = DeclarationForApi(declaration, Some(agentDetails), None)
      val beforeJson = JsonUtils.getJsonValueFromFile("etmp/valid-get-estate-response.json")
      val estateResponse = beforeJson.as[GetEstateResponse].asInstanceOf[GetEstateProcessedResponse]
      val afterJson = JsonUtils.getJsonValueFromFile("transformed/variations/estates-etmp-sent-with-agent-details.json")
      val transformer = new VariationDeclarationTransform

      val result = transformer.transform(estateResponse, estateResponse.getEstate, declarationForApi, entityEnd)
      result.asOpt.value mustBe afterJson
    }

    "transform json successfully when closing estate" in {
      val declarationForApi = DeclarationForApi(declaration, None, Some(LocalDate.parse("2019-02-03")))

      val beforeJson = JsonUtils.getJsonValueFromFile("etmp/valid-get-estate-response.json")
      val estateResponse = beforeJson.as[GetEstateResponse].asInstanceOf[GetEstateProcessedResponse]
      val afterJson = JsonUtils.getJsonValueFromFile("transformed/variations/estates-etmp-sent-with-end-date.json")
      val transformer = new VariationDeclarationTransform

      val result = transformer.transform(estateResponse, estateResponse.getEstate, declarationForApi, entityEnd)
      result.asOpt.value mustBe afterJson
    }
  }
}
