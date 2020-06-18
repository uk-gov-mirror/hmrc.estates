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

package uk.gov.hmrc.estates.services.register

import java.time.LocalDate

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{MustMatchers, OptionValues}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.JsString
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.estates.BaseSpec
import uk.gov.hmrc.estates.models._
import uk.gov.hmrc.estates.models.register.TaxAmount
import uk.gov.hmrc.estates.models.requests.IdentifierRequest
import uk.gov.hmrc.estates.repositories.TransformationRepository
import uk.gov.hmrc.estates.services.{AuditService, DesService}
import uk.gov.hmrc.estates.transformers.register.{AgentDetailsTransform, AmountOfTaxOwedTransform, DeceasedTransform, YearsReturnsTransform}
import uk.gov.hmrc.estates.transformers.{AddCorrespondenceNameTransform, AddEstatePerRepTransform, ComposedDeltaTransform, DeclarationTransformer}
import uk.gov.hmrc.estates.utils.JsonUtils

import scala.concurrent.ExecutionContext.Implicits._

class RegistrationServiceSpec extends BaseSpec with MockitoSugar with ScalaFutures with MustMatchers with OptionValues {

  "Organisation registering with no tax liability" in {

    val mockTransformationRepository = mock[TransformationRepository]
    val mockDesService = mock[DesService]
    val auditService = injector.instanceOf[AuditService]
    val declarationTransformer = new DeclarationTransformer

    val service = new RegistrationService(mockTransformationRepository, mockDesService, auditService, declarationTransformer)

    val transforms = ComposedDeltaTransform(Seq(
      DeceasedTransform(
        EstateWillType(
          name = NameType("Mr TRS Reference 31", None, "TaxPayer 31"),
          dateOfBirth = None,
          dateOfDeath = LocalDate.parse("2013-04-07"),
          identification = Some(IdentificationType(Some("MT939555B"), None, None))
        )
      ),
      AmountOfTaxOwedTransform(TaxAmount.AmountMoreThanTwoHalfMillion),
      AddCorrespondenceNameTransform(JsString("Estate of Mr A Deceased")),
      AddEstatePerRepTransform(
        Some(EstatePerRepIndType(
          name =  NameType("Alister", None, "Mc'Lovern"),
          dateOfBirth = LocalDate.parse("1955-09-08"),
          identification = IdentificationType(
            Some("JS123456A"),
            None,
            Some(AddressType("AEstateAddress1", "AEstateAddress2", Some("AEstateAddress3"), Some("AEstateAddress4"), Some("TF3 4ER"), "GB"))
          ),
          phoneNumber = "078888888",
          email = Some("test@abc.com")
        )),
        None
      )
    ))

    val expectedJson = JsonUtils.getJsonValueFromFile("transformed/declared/registration-submission-with-personal-rep-ind-no-tax-no-agent.json")

    val transformedJson = service.buildSubmissionFromTransforms(
      NameType("John", None, "Doe"),
      transforms
    )(IdentifierRequest(FakeRequest(), "id", AffinityGroup.Organisation))

    transformedJson.get mustEqual expectedJson
    transformedJson.get.validate[EstateRegistration].isSuccess mustBe true
  }

  "Organisation registering with tax liability" in {

    val mockTransformationRepository = mock[TransformationRepository]
    val mockDesService = mock[DesService]
    val auditService = injector.instanceOf[AuditService]
    val declarationTransformer = new DeclarationTransformer

    val service = new RegistrationService(mockTransformationRepository, mockDesService, auditService, declarationTransformer)

    val transforms = ComposedDeltaTransform(Seq(
      DeceasedTransform(
        EstateWillType(
          name = NameType("Mr TRS Reference 31", None, "TaxPayer 31"),
          dateOfBirth = None,
          dateOfDeath = LocalDate.parse("2013-04-07"),
          identification = Some(IdentificationType(Some("MT939555B"), None, None))
        )
      ),
      AmountOfTaxOwedTransform(TaxAmount.AmountMoreThanTwoHalfMillion),
      AddCorrespondenceNameTransform(JsString("Estate of Mr A Deceased")),
      AddEstatePerRepTransform(
        Some(EstatePerRepIndType(
          name =  NameType("Alister", None, "Mc'Lovern"),
          dateOfBirth = LocalDate.parse("1955-09-08"),
          identification = IdentificationType(
            Some("JS123456A"),
            None,
            Some(AddressType("AEstateAddress1", "AEstateAddress2", Some("AEstateAddress3"), Some("AEstateAddress4"), Some("TF3 4ER"), "GB"))
          ),
          phoneNumber = "078888888",
          email = Some("test@abc.com")
        )),
        None
      ),
      YearsReturnsTransform(YearsReturns(List(YearReturnType("20", taxConsequence = true))))
    ))

    val expectedJson = JsonUtils.getJsonValueFromFile("transformed/declared/registration-submission-with-personal-rep-ind-with-tax-no-agent.json")

    val transformedJson = service.buildSubmissionFromTransforms(
      NameType("John", None, "Doe"),
      transforms
    )(IdentifierRequest(FakeRequest(), "id", AffinityGroup.Organisation))

    transformedJson.get mustEqual expectedJson
    transformedJson.get.validate[EstateRegistration].isSuccess mustBe true
  }

  "Agent registering with no tax liability" in {

    val mockTransformationRepository = mock[TransformationRepository]
    val mockDesService = mock[DesService]
    val auditService = injector.instanceOf[AuditService]
    val declarationTransformer = new DeclarationTransformer

    val service = new RegistrationService(mockTransformationRepository, mockDesService, auditService, declarationTransformer)

    val transforms = ComposedDeltaTransform(Seq(
      DeceasedTransform(EstateWillType(
          name = NameType("Mr TRS Reference 31", None, "TaxPayer 31"),
          dateOfBirth = None,
          dateOfDeath = LocalDate.parse("2013-04-07"),
          identification = Some(IdentificationType(Some("MT939555B"), None, None))
      )),
      AmountOfTaxOwedTransform(TaxAmount.AmountMoreThanTwoHalfMillion),
      AddCorrespondenceNameTransform(JsString("Estate of Mr A Deceased")),
      AddEstatePerRepTransform(
        Some(EstatePerRepIndType(
          name =  NameType("Alister", None, "Mc'Lovern"),
          dateOfBirth = LocalDate.parse("1955-09-08"),
          identification = IdentificationType(
            Some("JS123456A"),
            None,
            Some(AddressType("AEstateAddress1", "AEstateAddress2", Some("AEstateAddress3"), Some("AEstateAddress4"), Some("TF3 4ER"), "GB"))
          ),
          phoneNumber = "078888888",
          email = Some("test@abc.com")
        )),
        None
      ),
      AgentDetailsTransform(AgentDetails("arn", "Agency Ltd", AddressType("line1", "line2", None, None, None, "FR"), "tel", "crn"))
    ))

    val expectedJson = JsonUtils.getJsonValueFromFile("transformed/declared/registration-submission-with-personal-rep-ind-no-tax-with-agent.json")

    val transformedJson = service.buildSubmissionFromTransforms(
      NameType("John", None, "Doe"),
      transforms
    )(IdentifierRequest(FakeRequest(), "id", AffinityGroup.Agent))

    transformedJson.get mustEqual expectedJson
    transformedJson.get.validate[EstateRegistration].isSuccess mustBe true
  }
}
