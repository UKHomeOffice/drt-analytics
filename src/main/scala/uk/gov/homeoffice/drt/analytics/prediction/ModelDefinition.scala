package uk.gov.homeoffice.drt.analytics.prediction

import uk.gov.homeoffice.drt.actor.PredictionModelActor.WithId
import uk.gov.homeoffice.drt.prediction.arrival.features.Feature

trait ModelDefinition[T, B] {
  def modelName: String

  def featuresVersion: Int

  def features: List[Feature[_]]

  def aggregateValue: T => Option[WithId]

  def targetValueAndFeatures: T => Option[(Double, Seq[String], Seq[Double], String)]

  def baselineValue: B => Double
}
