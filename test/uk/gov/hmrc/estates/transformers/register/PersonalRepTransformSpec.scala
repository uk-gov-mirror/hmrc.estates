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

import org.scalatest.{FreeSpec, MustMatchers, OptionValues}
import play.api.libs.json.{JsError, Json}
import uk.gov.hmrc.estates.models._
import uk.gov.hmrc.estates.utils.JsonUtils

class PersonalRepTransformSpec extends FreeSpec with MustMatchers with OptionValues {

  val newPersonalRepInd = EstatePerRepIndType(
    name =  NameType("Alister", None, "Mc'Lovern"),
    dateOfBirth = LocalDate.of(1980,6,1),
    identification = IdentificationType(Some("JS123456A"), None, None),
    phoneNumber = "078888888",
    email = Some("test@abc.com")
  )

  val newPersonalRepIndWithUkAddress = EstatePerRepIndType(
    name =  NameType("Alister", None, "Mc'Lovern"),
    dateOfBirth = LocalDate.of(1980,6,1),
    identification = IdentificationType(None, None, Some(AddressType(
      line1 = "Line 1",
      line2 = "Line 2",
      line3 = None,
      line4 = None,
      postCode = Some("NE981ZZ"),
      country = "GB"
    ))),
    phoneNumber = "078888888",
    email = Some("test@abc.com")
  )

  val newPersonalRepIndWithNonUkAddress = EstatePerRepIndType(
    name =  NameType("Alister", None, "Mc'Lovern"),
    dateOfBirth = LocalDate.of(1980,6,1),
    identification = IdentificationType(None, None, Some(AddressType(
      line1 = "Line 1",
      line2 = "Line 2",
      line3 = None,
      line4 = None,
      postCode = None,
      country = "DE"
    ))),
    phoneNumber = "078888888",
    email = Some("test@abc.com")
  )

  val newPersonalRepOrgWithUkAddress = EstatePerRepOrgType(
    orgName =  "Lovely Organisation",
    identification = IdentificationOrgType(
      None,
      Some(AddressType("line1", "line2", Some("line3"), Some("line4"), Some("postCode"), "Country"))
    ),
    phoneNumber = "078888888",
    email = Some("testy@xyz.org")
  )

  val newPersonalRepOrgWithNonUkAddress = EstatePerRepOrgType(
    orgName =  "Lovely Organisation",
    identification = IdentificationOrgType(
      None,
      Some(AddressType("line1", "line2", Some("line3"), Some("line4"), None, "Country"))
    ),
    phoneNumber = "078888888",
    email = Some("testy@xyz.org")
  )

  val newPersonalRepOrgWithUtrAndAddress = EstatePerRepOrgType(
    orgName =  "Lovely Organisation",
    identification = IdentificationOrgType(
      Some("1234567890"),
      Some(AddressType("line1", "line2", Some("line3"), Some("line4"), None, "Country"))
    ),
    phoneNumber = "078888888",
    email = Some("testy@xyz.org")
  )

  "the add personal rep transformer should" - {

    "add a personal rep individual" - {

      "when there is an existing personal rep" in {

        val trustJson = JsonUtils.getJsonValueFromFile("mdtp/valid-estate-registration-01.json")

        val afterJson = JsonUtils.getJsonValueFromFile("transformed/valid-estate-registration-01-personal-rep-ind-transformed.json")

        val transformer = new PersonalRepTransform(Some(newPersonalRepInd), None)

        val result = transformer.applyTransform(trustJson).get

        result mustBe afterJson
      }

      "when there are no existing personal reps" in {
        val trustJson = JsonUtils.getJsonValueFromFile("mdtp/valid-estate-registration-01.json")

        val afterJson = JsonUtils.getJsonValueFromFile("transformed/valid-estate-registration-01-personal-rep-ind-transformed.json")

        val transformer = new PersonalRepTransform(Some(newPersonalRepInd), None)

        val result = transformer.applyTransform(trustJson).get

        result mustBe afterJson
      }

      "when the document is empty" in {
        val transformer = PersonalRepTransform(Some(newPersonalRepInd), None)

        val result = transformer.applyTransform(Json.obj()).get

        result mustBe Json.obj(
          "estate" -> Json.obj(
            "entities" -> Json.obj(
              "personalRepresentative" -> Json.obj(
                "estatePerRepInd"-> Json.toJson(newPersonalRepInd)
              )
            )
          )
        )
      }
    }
    "add a personal rep organisation" - {

      "when there is an existing transform with personal rep with utr, being replaced with a transform without utr" in {
        val trustJson = JsonUtils.getJsonValueFromFile("mdtp/valid-estate-registration-05-with-per-rep-org-utr.json")

        val afterJson = JsonUtils.getJsonValueFromFile("transformed/valid-estate-registration-01-personal-rep-org-transformed-without-utr.json")

        val transformer = new PersonalRepTransform(None, Some(newPersonalRepOrgWithUkAddress))

        val result = transformer.applyTransform(trustJson).get

        result mustBe afterJson
      }

      "when there is an existing personal rep" in {

        val trustJson = JsonUtils.getJsonValueFromFile("mdtp/valid-estate-registration-01.json")

        val afterJson = JsonUtils.getJsonValueFromFile("transformed/valid-estate-registration-01-personal-rep-org-transformed.json")

        val transformer = new PersonalRepTransform(None, Some(newPersonalRepOrgWithUkAddress))

        val result = transformer.applyTransform(trustJson).get

        result mustBe afterJson
      }

      "when there are no existing personal reps" in {
        val trustJson = JsonUtils.getJsonValueFromFile("mdtp/valid-estate-registration-01.json")

        val afterJson = JsonUtils.getJsonValueFromFile("transformed/valid-estate-registration-01-personal-rep-org-transformed.json")

        val transformer = new PersonalRepTransform(None, Some(newPersonalRepOrgWithUkAddress))

        val result = transformer.applyTransform(trustJson).get

        result mustBe afterJson
      }
    }

  }

  "the personal rep declaration transform" - {

    "when personal rep doesn't have an address" in {
      val document = Json.obj()

      val transformer = new PersonalRepTransform(Some(newPersonalRepInd), None)

      val result = transformer.applyDeclarationTransform(document)

      val expectedResult = JsError("No address on personal rep to apply to correspondence")

      result mustBe expectedResult
    }

    "remove isPassport field and apply phone number rules upon applying transform" in {

      val personalRepInd = EstatePerRepIndType(
        name =  NameType("Alister", None, "Mc'Lovern"),
        dateOfBirth = LocalDate.of(1980,6,1),
        identification = IdentificationType(
          None,
          Some(PassportType("123456789", LocalDate.parse("2025-09-28"), "ES", Some(true))),
          Some(AddressType("Address line 1", "Address line 2", Some("Address line 3"), Some("Town or city"), Some("Z99 2YY"), "GB"))
        ),
        phoneNumber = "(0)078888888",
        email = Some("test@abc.com")
      )

      val trustJson = JsonUtils.getJsonValueFromFile("mdtp/valid-estate-registration-03-with-is-passport-field.json")

      val afterJson = JsonUtils.getJsonValueFromFile("transformed/valid-estate-registration-03-personal-rep-ind-transformed.json")

      val transformer = new PersonalRepTransform(Some(personalRepInd), None)

      val result = transformer.applyDeclarationTransform(trustJson).get

      result mustBe afterJson
    }

    "individual personal rep" - {

      "must remove address" - {

        "when personal rep has a nino" in {
            val document = Json.obj(
              "correspondence" -> Json.obj(
                "name" -> "Estate of Personal Rep"
              ),
              "estate" -> Json.obj(
                "entities" -> Json.obj(
                  "personalRepresentative" -> Json.obj(
                    "estatePerRepInd" -> Json.obj(
                      "identification" -> Json.obj(
                        "nino" -> "JP121212A",
                        "address" -> Json.obj()
                      )
                    )
                  )
                )
              )
            )

            val transformer = new PersonalRepTransform(Some(newPersonalRepIndWithUkAddress), None)

            val result = transformer.applyDeclarationTransform(document).get

            val expectedResult = JsonUtils.getJsonValueFromFile("transformed/declared/declaration-transform-personal-rep-ind-address-removed.json")

            result mustBe expectedResult
          }
      }

      "UK based" - {

        "must write to correspondence" - {

          "when starting with a blank document" in {

            val document = Json.obj()

            val transformer = new PersonalRepTransform(Some(newPersonalRepIndWithUkAddress), None)

            val result = transformer.applyDeclarationTransform(document).get

            val expectedResult = JsonUtils.getJsonValueFromFile("transformed/declared/declaration-transform-personal-rep-ind-correspondence-with-uk-address.json")

            result mustBe expectedResult
          }

          "when not starting with a blank document" in {
            val document = Json.obj(
              "correspondence" -> Json.obj(
                "name" -> "Estate of Personal Rep"
              )
            )

            val transformer = new PersonalRepTransform(Some(newPersonalRepIndWithUkAddress), None)

            val result = transformer.applyDeclarationTransform(document).get

            val expectedResult = JsonUtils.getJsonValueFromFile("transformed/declared/declaration-transform-personal-rep-ind-correspondence-with-uk-address-and-name.json")

            result mustBe expectedResult
          }

        }
      }

      "non-UK based" - {

        "must write to correspondence" - {

          "when starting with a blank document" in {

            val document = Json.obj()

            val transformer = new PersonalRepTransform(Some(newPersonalRepIndWithNonUkAddress), None)

            val result = transformer.applyDeclarationTransform(document).get

            val expectedResult = JsonUtils.getJsonValueFromFile("transformed/declared/declaration-transform-personal-rep-ind-correspondence-with-non-uk-address.json")

            result mustBe expectedResult
          }

          "when not starting with a blank document" in {
            val document = Json.obj(
              "correspondence" -> Json.obj(
                "name" -> "Estate of Personal Rep"
              )
            )

            val transformer = new PersonalRepTransform(Some(newPersonalRepIndWithNonUkAddress), None)

            val result = transformer.applyDeclarationTransform(document).get

            val expectedResult = JsonUtils.getJsonValueFromFile("transformed/declared/declaration-transform-personal-rep-ind-correspondence-with-non-uk-address-and-name.json")

            result mustBe expectedResult
          }

        }
      }
    }

    "business personal rep" - {

      "must remove address" - {

        "when personal rep has a utr" in {
            val document = Json.obj(
              "correspondence" -> Json.obj(
                "name" -> "Estate of Personal Rep"
              ),
              "estate" -> Json.obj(
                "entities" -> Json.obj(
                  "personalRepresentative" -> Json.obj(
                    "estatePerRepOrg" -> Json.obj(
                      "identification" -> Json.obj(
                        "utr" -> "1234567890",
                        "address" -> Json.obj()
                      )
                    )
                  )
                )
              )
            )

            val transformer = new PersonalRepTransform(None, Some(newPersonalRepOrgWithNonUkAddress))

            val result = transformer.applyDeclarationTransform(document).get

            val expectedResult = JsonUtils.getJsonValueFromFile("transformed/declared/declaration-transform-personal-rep-org-address-removed.json")

            result mustBe expectedResult
          }
      }

      "UK based" - {

        "must write to correspondence" - {

          "when starting with a blank document" in {

            val document = Json.obj()

            val transformer = new PersonalRepTransform(None, Some(newPersonalRepOrgWithUkAddress))

            val result = transformer.applyDeclarationTransform(document).get

            val expectedResult = JsonUtils.getJsonValueFromFile("transformed/declared/declaration-transform-personal-rep-org-correspondence-with-uk-address.json")

            result mustBe expectedResult
          }

          "when not starting with a blank document" in {
            val document = Json.obj(
              "correspondence" -> Json.obj(
                "name" -> "Estate of Personal Rep"
              )
            )

            val transformer = new PersonalRepTransform(None, Some(newPersonalRepOrgWithUkAddress))

            val result = transformer.applyDeclarationTransform(document).get

            val expectedResult = JsonUtils.getJsonValueFromFile("transformed/declared/declaration-transform-personal-rep-org-correspondence-with-uk-address-and-name.json")

            result mustBe expectedResult
          }

        }
      }

      "non-UK based" - {

        "must write to correspondence" - {

          "when starting with a blank document" in {

            val document = Json.obj()

            val transformer = new PersonalRepTransform(None, Some(newPersonalRepOrgWithNonUkAddress))

            val result = transformer.applyDeclarationTransform(document).get

            val expectedResult = JsonUtils.getJsonValueFromFile("transformed/declared/declaration-transform-personal-rep-org-correspondence-with-non-uk-address.json")

            result mustBe expectedResult
          }

          "when not starting with a blank document" in {
            val document = Json.obj(
              "correspondence" -> Json.obj(
                "name" -> "Estate of Personal Rep"
              )
            )

            val transformer = new PersonalRepTransform(None, Some(newPersonalRepOrgWithNonUkAddress))

            val result = transformer.applyDeclarationTransform(document).get

            val expectedResult = JsonUtils.getJsonValueFromFile("transformed/declared/declaration-transform-personal-rep-org-correspondence-with-non-uk-address-and-name.json")

            result mustBe expectedResult
          }

        }
      }
    }

  }
}