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

package uk.gov.hmrc.estates.services

import java.net.URL

import com.github.fge.jackson.JsonLoader
import com.github.fge.jsonschema.core.report.LogLevel.ERROR
import com.github.fge.jsonschema.core.report.ProcessingReport
import com.github.fge.jsonschema.main.{JsonSchema, JsonSchemaFactory}
import javax.inject.Inject
import play.api.Logger
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsPath, Json, JsonValidationError, Reads}
import uk.gov.hmrc.estates.models.EstateRegistration
import uk.gov.hmrc.estates.utils.EstateBusinessValidation

import scala.collection.JavaConverters._
import scala.io.Source
import scala.util.{Failure, Success, Try}

class ValidationService @Inject()() {

  private val factory = JsonSchemaFactory.byDefault()

  def get(schemaFile: String): Validator = {
    println(s"schemaFile is $schemaFile")
    val resource: URL = getClass.getResource(schemaFile)
    println(s"resource is $resource")
    println(s"resource path is ${resource.getPath}")
    val source = Source.fromFile(resource.getPath)
    val schemaJsonFileString = source.mkString
    source.close()
    val schemaJson = JsonLoader.fromString(schemaJsonFileString)
    val schema = factory.getJsonSchema(schemaJson)
    new Validator(schema)
  }

}

class Validator(schema: JsonSchema) {

  private val JsonErrorMessageTag = "message"
  private val JsonErrorInstanceTag = "instance"
  private val JsonErrorPointerTag = "pointer"

  def validate[T](inputJson: String)(implicit reads: Reads[T]): Either[List[EstatesValidationError], T] = {
    Try(JsonLoader.fromString(inputJson)) match {
      case Success(json) =>
        val result = schema.validate(json)

        if (result.isSuccess) {
          Json.parse(inputJson).validate[T].fold(
            errors => Left(getValidationErrors(errors)),
            request =>
              validateBusinessRules(request)
          )
        } else {
          Logger.error(s"[Validator][validate] unable to validate to schema")
          Left(getValidationErrors(result))
        }
      case Failure(e) =>
        Logger.error(s"[Validator][validate] IOException $e")
        Left(List(EstatesValidationError(s"[Validator][validate] IOException $e", "")))
    }

  }

  private def validateBusinessRules[T](request: T): Either[List[EstatesValidationError], T] = {
    request match {
       case estateRegistration: EstateRegistration =>
        EstateBusinessValidation.check(estateRegistration) match {
          case Nil => Right(request)
          case errors @ _ :: _ =>
            Logger.error(s"[validateBusinessRules] Validation fails : $errors")
            Left(errors)
        }
      case _ => Right(request)
    }
  }

  protected def getValidationErrors(errors: Seq[(JsPath, Seq[JsonValidationError])]): List[EstatesValidationError] = {
    val validationErrors = errors.flatMap(errors => errors._2.map(error => EstatesValidationError(error.message, errors._1.toString()))).toList
    Logger.debug(s"[Validator][getValidationErrors]  validationErrors in validate :  $validationErrors")
    validationErrors
  }

  private def getValidationErrors(validationOutput: ProcessingReport): List[EstatesValidationError] = {
    val validationErrors: List[EstatesValidationError] = validationOutput.iterator.asScala.toList.filter(m => m.getLogLevel == ERROR).map(m => {
      val error = m.asJson()
      val message = error.findValue(JsonErrorMessageTag).asText("")
      val location = error.findValue(JsonErrorInstanceTag).at(s"/$JsonErrorPointerTag").asText()
      val locations = error.findValues(JsonErrorPointerTag)
      Logger.error(s"validation failed at locations :  $locations")
      EstatesValidationError(message, location)
    })
    validationErrors
  }

}

case class EstatesValidationError(message: String, location: String)

object EstatesValidationError {
  implicit val formats = Json.format[EstatesValidationError]
}