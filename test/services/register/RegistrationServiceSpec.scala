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

package services.register

import java.time.LocalDate
import base.BaseSpec
import org.mockito.Matchers.{any, eq => mockEq}
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{MustMatchers, OptionValues}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsString, Json}
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.AffinityGroup
import models._
import models.register.{RegistrationDeclaration, TaxAmount}
import models.requests.IdentifierRequest
import org.mockito.ArgumentCaptor
import repositories.TransformationRepository
import services.{AuditService, EstatesService, Estates5MLDService, EstatesStoreService}

import transformers.ComposedDeltaTransform
import transformers.register._
import utils.JsonUtils

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future

class RegistrationServiceSpec extends BaseSpec with MockitoSugar with ScalaFutures with MustMatchers with OptionValues {

  val mockTransformationRepository: TransformationRepository = mock[TransformationRepository]
  val mockEstateService: EstatesService = mock[EstatesService]
  val mockEstatesStoreService: EstatesStoreService = mock[EstatesStoreService]
  val mockAuditService: AuditService = mock[AuditService]

  val estates5MLDService = new Estates5MLDService(mockEstatesStoreService)

  val regCapture: ArgumentCaptor[EstateRegistration] = ArgumentCaptor.forClass(classOf[EstateRegistration])

  before {
    reset(mockAuditService, mockEstatesStoreService)
  }

  val declarationTransformer = new DeclarationTransform

  val address = AddressType("AEstateAddress1", "AEstateAddress2", Some("AEstateAddress3"), Some("AEstateAddress4"), Some("TF3 4ER"), "GB")

  val service = new RegistrationService(
    mockTransformationRepository,
    mockEstateService,
    estates5MLDService,
    declarationTransformer,
    mockAuditService
  )

  val deceased: EstateWillType = EstateWillType(
    name = NameType("Mr TRS Reference 31", None, "TaxPayer 31"),
    dateOfBirth = None,
    dateOfDeath = LocalDate.parse("2013-04-07"),
    identification = Some(IdentificationType(Some("MT939555B"), None, None))
  )

  val personalRepInd: EstatePerRepIndType = EstatePerRepIndType(
    name =  NameType("Alister", None, "Mc'Lovern"),
    dateOfBirth = LocalDate.parse("1955-09-08"),
    identification = IdentificationType(
      Some("JS123456A"),
      None,
      Some(address)
    ),
    phoneNumber = "078888888",
    email = Some("test@abc.com")
  )

  val estateName: String = "Estate of Mr A Deceased"

  val taxAmount: TaxAmount = TaxAmount.AmountMoreThanTwoHalfMillion

  val deceasedTransform = DeceasedTransform(deceased)

  val amountOfTaxOwedTransform = AmountOfTaxOwedTransform(taxAmount)

  val addCorrespondenceNameTransform = CorrespondenceNameTransform(JsString(estateName))

  val addEstatePerRepTransform = PersonalRepTransform(
    Some(personalRepInd),
    None
  )

  val allTransforms = ComposedDeltaTransform(Seq(
      deceasedTransform,
      amountOfTaxOwedTransform,
      addCorrespondenceNameTransform,
      addEstatePerRepTransform))

  implicit val request : IdentifierRequest[_] = IdentifierRequest(FakeRequest(), "id", AffinityGroup.Organisation)

  ".getRegistration" must {

    val registration = EstateRegistrationNoDeclaration(
      None,
      CorrespondenceName(estateName),
      None,
      Estate(
        EntitiesType(
          PersonalRepresentativeType(
            Some(personalRepInd),
            None
          ),
          deceased
        ),
        None,
        taxAmount.toString
      ),
      None
    )

    "successfully return a registration" in {

      reset(mockAuditService)
      when(mockTransformationRepository.get(any())).thenReturn(Future.successful(Some(allTransforms)))

      val result = service.getRegistration()

      result.futureValue mustBe registration

      verify(mockAuditService).auditGetRegistrationSuccess(any())(any(), any())
    }

    "return run time exception" when {

      "no transforms" in {

        reset(mockAuditService)
        when(mockTransformationRepository.get(any())).thenReturn(Future.successful(None))

        val result = service.getRegistration()

        assertThrows[RuntimeException] {
          result.futureValue
        }
        verify(mockAuditService).auditGetRegistrationFailed(
          mockEq(ComposedDeltaTransform(Seq.empty)),
          mockEq("Unable to get registration due to there being no transforms"),
          any()
        )(any(), any())
      }

      "transformed json cannot be parsed as EstateRegistration" in {

        reset(mockAuditService)
        val transforms: ComposedDeltaTransform = ComposedDeltaTransform(Seq(deceasedTransform))

        when(mockTransformationRepository.get(any())).thenReturn(Future.successful(Some(transforms)))

        val result = service.getRegistration()

        assertThrows[RuntimeException] {
          result.futureValue
        }
        verify(mockAuditService).auditGetRegistrationFailed(
          mockEq(transforms),
          mockEq("Unable to parse transformed json as EstateRegistrationNoDeclaration"),
          any()
        )(any(), any())
      }
    }
  }

  ".submit" must {

    "successfully submit the payload" when {

      "4mld" in {

        when(mockEstatesStoreService.isFeatureEnabled(mockEq("5mld"))(any(), any()))
          .thenReturn(Future.successful(false))

        when(mockTransformationRepository.get(any()))
          .thenReturn(Future.successful(Some(allTransforms)))

        when(mockEstateService.registerEstate(regCapture.capture()))
          .thenReturn(Future.successful(RegistrationTrnResponse("trn")))

        val result = service.submit(RegistrationDeclaration(NameType("John", None, "Doe")))

        result.futureValue mustBe RegistrationTrnResponse("trn")

        regCapture.getValue.submissionDate mustBe None

        verify(mockAuditService).auditRegistrationSubmitted(
          any(),
          mockEq("trn")
        )(any(), any())
      }

      "5mld" in {

        when(mockEstatesStoreService.isFeatureEnabled(mockEq("5mld"))(any(), any()))
          .thenReturn(Future.successful(true))

        when(mockTransformationRepository.get(any()))
          .thenReturn(Future.successful(Some(allTransforms)))

        when(mockEstateService.registerEstate(regCapture.capture()))
          .thenReturn(Future.successful(RegistrationTrnResponse("trn")))

        val result = service.submit(RegistrationDeclaration(NameType("John", None, "Doe")))

        result.futureValue mustBe RegistrationTrnResponse("trn")

        regCapture.getValue.submissionDate.value mustBe a[LocalDate]

        verify(mockAuditService).auditRegistrationSubmitted(
          any(),
          mockEq("trn")
        )(any(), any())
      }
    }

    "failed to submit the payload for some reason" in {

      when(mockTransformationRepository.get(any()))
        .thenReturn(Future.successful(Some(allTransforms)))

      when(mockEstateService.registerEstate(any()))
        .thenReturn(Future.successful(AlreadyRegisteredResponse))

      when(mockEstatesStoreService.isFeatureEnabled(mockEq("5mld"))(any(), any()))
        .thenReturn(Future.successful(false))

      val result = service.submit(RegistrationDeclaration(NameType("John", None, "Doe")))

      result.futureValue mustBe AlreadyRegisteredResponse

      verify(mockAuditService).auditRegistrationFailed(
        mockEq("id"),
        any(),
        mockEq(AlreadyRegisteredResponse)
      )(any())
    }

    "return run time exception" when {

      "no transforms" in {

        when(mockTransformationRepository.get(any()))
          .thenReturn(Future.successful(None))

        when(mockEstatesStoreService.isFeatureEnabled(mockEq("5mld"))(any(), any()))
          .thenReturn(Future.successful(false))

        val result = service.submit(RegistrationDeclaration(NameType("John",None, "Doe")))

        assertThrows[RuntimeException] {
          result.futureValue
        }

        verify(mockAuditService).auditRegistrationTransformationError(
          any(),
          any(),
          mockEq("Unable to submit registration due to there being no transforms"),
          any()
        )(any(), any())
      }

      "transformed json cannot be built from transforms" in {

        val transforms: ComposedDeltaTransform = ComposedDeltaTransform(Seq(deceasedTransform))

        when(mockTransformationRepository.get(any()))
          .thenReturn(Future.successful(Some(transforms)))

        when(mockEstatesStoreService.isFeatureEnabled(mockEq("5mld"))(any(), any()))
          .thenReturn(Future.successful(false))

        val result = service.submit(RegistrationDeclaration(NameType("John",None, "Doe")))

        assertThrows[RuntimeException] {
          result.futureValue
        }

        verify(mockAuditService).auditRegistrationTransformationError(
          any(),
          mockEq(Json.toJson(transforms)),
          mockEq("Unable to build json from transforms"),
          any()
        )(any(), any())
      }
    }
  }

  ".buildSubmissionFromTransforms" must {

    "build a Organisation registering with no tax liability" in {

      val service = injector.instanceOf[RegistrationService]

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
        CorrespondenceNameTransform(JsString("Estate of Mr A Deceased")),
        PersonalRepTransform(
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
        transforms,
        false
      )(IdentifierRequest(FakeRequest(), "id", AffinityGroup.Organisation))

      transformedJson.get mustEqual expectedJson
      transformedJson.get.validate[EstateRegistration].isSuccess mustBe true
    }

    "build a Organisation registering with tax liability" in {

      val service = injector.instanceOf[RegistrationService]

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
        CorrespondenceNameTransform(JsString("Estate of Mr A Deceased")),
        PersonalRepTransform(
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
        transforms,
        false
      )(IdentifierRequest(FakeRequest(), "id", AffinityGroup.Organisation))

      transformedJson.get mustEqual expectedJson
      transformedJson.get.validate[EstateRegistration].isSuccess mustBe true
    }

    "build a Agent registering with no tax liability" in {

      val service = injector.instanceOf[RegistrationService]

      val transforms = ComposedDeltaTransform(Seq(
        DeceasedTransform(EstateWillType(
          name = NameType("Mr TRS Reference 31", None, "TaxPayer 31"),
          dateOfBirth = None,
          dateOfDeath = LocalDate.parse("2013-04-07"),
          identification = Some(IdentificationType(Some("MT939555B"), None, None))
        )),
        AmountOfTaxOwedTransform(TaxAmount.AmountMoreThanTwoHalfMillion),
        CorrespondenceNameTransform(JsString("Estate of Mr A Deceased")),
        PersonalRepTransform(
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
        transforms,
        false
      )(IdentifierRequest(FakeRequest(), "id", AffinityGroup.Agent))

      transformedJson.get mustEqual expectedJson
      transformedJson.get.validate[EstateRegistration].isSuccess mustBe true
    }
  }

}
