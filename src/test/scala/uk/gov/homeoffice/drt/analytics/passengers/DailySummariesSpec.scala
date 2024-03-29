package uk.gov.homeoffice.drt.analytics.passengers

import akka.actor.{Actor, ActorSystem, Props}
import akka.stream.Materializer
import akka.testkit.TestKit
import org.specs2.mutable.SpecificationLike
import uk.gov.homeoffice.drt.analytics.actors.{FeedPersistenceIds, GetArrivals}
import uk.gov.homeoffice.drt.analytics.{Arrivals, DailyPaxCountsOnDay, SimpleArrival}
import uk.gov.homeoffice.drt.arrivals.Passengers
import uk.gov.homeoffice.drt.ports.{ForecastFeedSource, LiveFeedSource}
import uk.gov.homeoffice.drt.time.{SDate, SDateLike, UtcDate}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}

object MockArrivalsActor {
  def props: Arrivals => (String, SDateLike) => Props = (arrivals: Arrivals) => (_: String, _: SDateLike) => Props(new MockArrivalsActor(arrivals))
}

class MockArrivalsActor(arrivals: Arrivals) extends Actor {
  override def receive: Receive = {
    case GetArrivals(_, _) => sender() ! arrivals
  }
}

class DailySummariesSpec extends TestKit(ActorSystem("passengers-actor")) with SpecificationLike {
  implicit val ec: ExecutionContextExecutor = ExecutionContext.global
  implicit val mat: Materializer = Materializer.createMaterializer(system)

  val livePid: String = FeedPersistenceIds.live
  val forecastPid: String = FeedPersistenceIds.forecastBase
  val date: SDateLike = SDate("2020-01-01")
  val forecastArrival: SimpleArrival = SimpleArrival("BA", 1, date.millisSinceEpoch, "T1", "JFK", "sched",
    Map(ForecastFeedSource -> Passengers(Option(100), Option(0))), None)
  val liveArrival: SimpleArrival = SimpleArrival("BA", 1, date.millisSinceEpoch, "T1", "JFK", "sched",
    Map(LiveFeedSource -> Passengers(Option(50), Option(0))), None)

  "Given a sourcePersistenceId, and props for a mock actor with an arrival" >> {
    val props = MockArrivalsActor.props

    "When I ask for arrivals from sources" >> {

      val eventualsArrivals = DailySummaries.arrivalsForSources(List(forecastPid), date.toUtcDate, date.toUtcDate, props(genArrivals(Seq(forecastArrival))))
      "I should get the arrival from the mock actor" >> {
        val result: Seq[(String, Arrivals)] = eventualsArrivals.map { eventualArrivals =>
          Await.result(eventualArrivals, 1.second)
        }
        result === Seq((forecastPid, genArrivals(Seq(forecastArrival))))
      }
    }
  }

  "Given a live and a forecast version of the same arrival" >> {
    "When I ask to merge live and forecast arrivals" >> {
      val futureForecastArrivals = Future((forecastPid, genArrivals(Seq(forecastArrival))))
      val futureLiveArrivals = Future((livePid, genArrivals(Seq(liveArrival))))

      val mergedArrivals = Await.result(DailySummaries.mergeArrivals(Seq(futureForecastArrivals, futureLiveArrivals)), 1.second)

      mergedArrivals === Map(liveArrival.uniqueArrival -> liveArrival)
    }
  }

  "Given one arrival" >> {
    "When I ask for the summary csv line for the two days starting from scheduled date of the arrival for" >> {
      val numberOfDays = 2
      "I should get a csv line showing the date, terminal, origin and pax count for that date and a '-' for the day after" >> {
        val eventualCountsByOrigin = DailySummaries.dailyPaxCountsForDayByOrigin(date.toUtcDate, date.toUtcDate, numberOfDays, "T1", Future(genArrivals(Seq(liveArrival)).arrivals))
        val eventualCsv = DailySummaries.dailyOriginCountsToCsv(date, date, numberOfDays, "T1", eventualCountsByOrigin)
        val summaries = Await.result(eventualCsv, 1.second)

        summaries === "2020-01-01,T1,JFK,50,-"
      }
    }
  }

  "Given one arrival" >> {
    "When I ask for the daily summary for that 1 day when the arrival is scheduled for" >> {
      "I should get a map of the arrival's origin to a DailyPaxCountOnDay containing 1 day with the pax from the 1 arrival" >> {
        val summaries = Await.result(DailySummaries.dailyPaxCountsForDayByOrigin(date.toUtcDate, date.toUtcDate, 1, "T1", Future(genArrivals(Seq(liveArrival)).arrivals)), 1.second)

        summaries === Map(liveArrival.origin ->
          DailyPaxCountsOnDay(date.toUtcDate, Map(date.millisSinceEpoch -> liveArrival.bestPaxEstimate.getPcpPax.getOrElse(0))))
      }
    }

    "When I ask for the daily summary for 2 days starting on the 1 day when the arrival is scheduled for" >> {
      "I should get a map of the arrival's origin to a DailyPaxCountOnDay containing just one day as the next doesn't have any pax" >> {
        val summaries = Await.result(DailySummaries.dailyPaxCountsForDayByOrigin(date.toUtcDate, date.toUtcDate, 2, "T1", Future(genArrivals(Seq(liveArrival)).arrivals)), 1.second)

        summaries === Map(liveArrival.origin ->
          DailyPaxCountsOnDay(date.toUtcDate, Map(date.millisSinceEpoch -> liveArrival.bestPaxEstimate.getPcpPax.getOrElse(0))))
      }
    }
  }

  "Given 2 arrivals scheduled for the same day" >> {
    "When I ask for the daily summary for the day when the arrival is scheduled for" >> {
      "I should get a map of the arrival's origin to a DailyPaxCountOnDay containing 1 day with the total pax from the 2 arrivals" >> {
        val arrival2 = liveArrival.copy(number = 2)
        val summaries = Await
          .result(DailySummaries.dailyPaxCountsForDayByOrigin(date.toUtcDate, date.toUtcDate, 1, "T1", Future(genArrivals(Seq(liveArrival, arrival2)).arrivals)), 1.second)

        summaries === Map(liveArrival.origin -> DailyPaxCountsOnDay(date.toUtcDate,
          Map(date.millisSinceEpoch -> (liveArrival.bestPaxEstimate.getPcpPax.getOrElse(0) +
            arrival2.bestPaxEstimate.getPcpPax.getOrElse(0)))))
      }
    }
  }

  "Given 2 arrivals scheduled for the same day, each with 100 pax and 10 transit pax" >> {
    "When I ask for the daily summary for the day when the arrival is scheduled for" >> {
      "I should get a map of the arrival's origin to a DailyPaxCountOnDay containing 1 day with the total pax from the 2 arrivals minus the transit pax" >> {
        val arrival = liveArrival
          .copy(passengerSources = Map(LiveFeedSource -> Passengers(Option(100), Option(10))))
        val arrival2 = liveArrival
          .copy(number = 2, passengerSources = Map(LiveFeedSource -> Passengers(Option(100), Option(10))))
        val summaries = Await.result(DailySummaries
          .dailyPaxCountsForDayByOrigin(date.toUtcDate, date.toUtcDate, 1, "T1", Future(genArrivals(Seq(arrival, arrival2)).arrivals)), 1.second)

        summaries === Map(liveArrival.origin -> DailyPaxCountsOnDay(date.toUtcDate, Map(date.millisSinceEpoch -> (100 * 2 - 10 * 2))))
      }
    }
  }

  private def genArrivals(arrivals: Seq[SimpleArrival]): Arrivals = Arrivals(arrivals.map(a => (a.uniqueArrival, a)).toMap)
}
