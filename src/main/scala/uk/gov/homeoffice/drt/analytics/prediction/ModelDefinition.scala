package uk.gov.homeoffice.drt.analytics.prediction

import uk.gov.homeoffice.drt.actor.PredictionModelActor.WithId
import uk.gov.homeoffice.drt.prediction.Feature

trait ModelDefinition[T, B] {
  def modelName: String

  def features: List[Feature]

  def aggregateValue: T => Option[WithId]

  def targetValueAndFeatures: T => Option[(Double, Seq[String], Seq[Double])]

  def baselineValue: B => Double
}
