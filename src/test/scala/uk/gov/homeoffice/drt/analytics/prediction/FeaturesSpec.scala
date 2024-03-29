package uk.gov.homeoffice.drt.analytics.prediction

import org.apache.spark.ml.linalg.Vectors
import org.apache.spark.sql.SparkSession
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.prediction.FeaturesWithOneToManyValues
import uk.gov.homeoffice.drt.prediction.arrival.features.FeatureColumnsV1.{Carrier, DayOfWeek}
import uk.gov.homeoffice.drt.time.{SDate, SDateLike}

class FeaturesSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll {
  implicit val session: SparkSession = SparkSession
    .builder
    .config("spark.master", "local")
    .getOrCreate()

  import session.implicits._

  override def afterAll(): Unit = session.close()

  "Given a simple one column feature and a data frame, Features" should {
    "give an indexed seq containing the unique 2 values strings" in {
      val featureTypes = List(Carrier)
      val df = List("a_1", "a_1", "a_2", "a_2").toDF(List("carrier"): _*)
      val features = DataSet(df, featureTypes).featuresWithOneToManyValues

      features.oneToManyValues.toSet should ===(Set("a_1", "a_2"))
    }
  }

  "Give a dataframe row, a Features" should {
    "return an appropriate feature vector when the values match the first feature value" in {
      val featureTypes = List(Carrier)
      val features = FeaturesWithOneToManyValues(featureTypes, IndexedSeq("ab_1", "ab_1", "ab_2", "ab_2"))

      val row = List("ab_1").toDF(List("carrier"): _*).collect().head

      FeatureVectors.featuresVectorForRow(row, features) should ===(Vectors.dense(1d, 0d, 0d, 0d))
    }

    "return an appropriate feature vector when the values match the third feature value" in {
      val featureTypes = List(Carrier)
      val features = FeaturesWithOneToManyValues(featureTypes, IndexedSeq("ab_1", "ab_1", "ab_2", "ab_2"))

      val row = List("ab_2").toDF(List("carrier"): _*).collect().head

      FeatureVectors.featuresVectorForRow(row, features) should ===(Vectors.dense(0d, 0d, 1d, 0d))
    }

    "return an appropriate feature vector for 2 one to many features" in {
      implicit val sdateProvider: Long => SDateLike = (ts: Long) => SDate(ts)
      val featureTypes = List(Carrier, DayOfWeek())
      val features = FeaturesWithOneToManyValues(featureTypes, IndexedSeq("ab_1", "ab_1", "ab_2", "ab_2", "zb_s", "zb_t"))

      val row = List(("ab_2", "zb_s")).toDF(List("carrier", "dayOfTheWeek"): _*).collect().head

      FeatureVectors.featuresVectorForRow(row, features) should ===(Vectors.dense(0d, 0d, 1d, 0d, 1d, 0d))
    }
  }
}
