package andriusdap.orbweaver.controllers

import andriusdap.orbweaver.core.Classifier
import com.google.inject.Singleton
import play.api.mvc._

import scala.language.postfixOps

@Singleton
class ClassifierController {
  def classify(source: String) = Action {
    request =>
      Results.Ok(Classifier.classify(source))
  }
}