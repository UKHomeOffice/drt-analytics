package uk.gov.homeoffice.drt.analytics.actors

import server.protobuf.messages.ModelAndFeatures.{FeaturesMessage, ModelAndFeaturesMessage, OneToManyFeatureMessage, RegressionModelMessage}
import uk.gov.homeoffice.drt.analytics.time.SDate
import uk.gov.homeoffice.drt.prediction.FeatureType.{OneToMany, Single}
import uk.gov.homeoffice.drt.prediction.{Features, ModelAndFeatures, RegressionModel, TouchdownModelAndFeatures}

object ModelAndFeaturesConversion {
  def modelAndFeaturesFromMessage(msg: ModelAndFeaturesMessage): ModelAndFeatures = {
    val model = msg.model.map(modelFromMessage).getOrElse(throw new Exception("No value for model"))
    val features = msg.features.map(featuresFromMessage).getOrElse(throw new Exception("No value for features"))
    val targetName = msg.targetName.getOrElse(throw new Exception("Mandatory parameter 'targetName' not specified"))
    val examplesTrainedOn = msg.examplesTrainedOn.getOrElse(throw new Exception("Mandatory parameter 'examplesTrainedOn' not specified"))
    val improvementPct = msg.improvementPct.getOrElse(throw new Exception("Mandatory parameter 'improvement' not specified"))

    ModelAndFeatures(model, features, targetName, examplesTrainedOn, improvementPct, millis => SDate(millis))
  }

  def modelFromMessage(msg: RegressionModelMessage): RegressionModel =
    RegressionModel(msg.coefficients, msg.intercept.getOrElse(throw new Exception("No value for intercept")))

  def featuresFromMessage(msg: FeaturesMessage): Features = {
    val singles = msg.singleFeatures.map(Single)
    val oneToManys = msg.oneToManyFeatures.map(oneToManyFromMessage)
    val allFeatures = oneToManys ++ singles

    Features(allFeatures.toList, msg.oneToManyValues.toIndexedSeq)
  }

  def oneToManyFromMessage(msg: OneToManyFeatureMessage): OneToMany =
    OneToMany(msg.columns.toList, msg.prefix.getOrElse(throw new Exception("No value for prefix")))

  def modelToMessage(model: RegressionModel): RegressionModelMessage =
    RegressionModelMessage(
      coefficients = model.coefficients.toArray,
      intercept = Option(model.intercept),
    )

  def featuresToMessage(features: Features): FeaturesMessage = {
    FeaturesMessage(
      oneToManyFeatures = features.featureTypes.collect {
        case OneToMany(columnNames, featurePrefix) =>
          OneToManyFeatureMessage(columnNames, Option(featurePrefix))
      },
      singleFeatures = features.featureTypes.collect {
        case Single(columnName) => columnName
      },
      oneToManyValues = features.oneToManyValues
    )
  }

  def modelAndFeaturesToMessage(modelAndFeatures: ModelAndFeatures, now: Long): ModelAndFeaturesMessage = {
    ModelAndFeaturesMessage(
      model = Option(modelToMessage(modelAndFeatures.model)),
      features = Option(featuresToMessage(modelAndFeatures.features)),
      targetName = Option(TouchdownModelAndFeatures.targetName),
      examplesTrainedOn = Option(modelAndFeatures.examplesTrainedOn),
      improvementPct = Option(modelAndFeatures.improvementPct),
      timestamp = Option(now),
    )
  }
}