package uk.gov.homeoffice.drt.analytics

import org.slf4j.{Logger, LoggerFactory}
import uk.gov.homeoffice.drt.analytics.DailyPaxCountsOnDay.applyDiffToExisting
import uk.gov.homeoffice.drt.time.{SDate, SDateLike, UtcDate}

case class OriginTerminalDailyPaxCountsOnDay(origin: String, terminal: String, counts: DailyPaxCountsOnDay) {
  def applyAndGetDiff(existingCounts: Map[(Long, Long), Int]): (Map[(Long, Long), Int], Iterable[(Long, Long, Int)]) = {
    val diff = counts.diffFromExisting(existingCounts)
    (applyDiffToExisting(diff, existingCounts), diff)
  }
}

case class DailyPaxCountsOnDay(date: UtcDate, dailyPax: Map[Long, Int]) {
  val log: Logger = LoggerFactory.getLogger(getClass)

  import DailyPaxCountsOnDay._

  val day: SDateLike = SDate(date)

  def diffFromExisting(existingPaxNos: Map[(Long, Long), Int]): Iterable[(Long, Long, Int)] = dailyPax.map {
    case (incomingDayMillis, incomingPax) =>
      val key = (day.millisSinceEpoch, incomingDayMillis)
      val incomingDay = SDate(incomingDayMillis)

      existingPaxNos.get(key) match {
        case None =>
          log.info(s"New day of pax ($incomingPax) for ${incomingDay.toISOString} on ${day.toISOString}")
          Some((day.millisSinceEpoch, incomingDayMillis, incomingPax))
        case Some(existingPax) if existingPax != incomingPax =>
          log.info(s"Change in pax ($existingPax -> $incomingPax) for ${incomingDay.toISOString} on ${day.toISOString}")
          Some((day.millisSinceEpoch, incomingDayMillis, incomingPax))
        case Some(existingPax) =>
          log.debug(s"No change in pax ($existingPax) for ${incomingDay.toISOString} on ${day.toISOString}")
          None
      }
  }.collect { case Some(diff) => diff }

  def applyToExisting(existingCounts: Map[(Long, Long), Int]): Map[(Long, Long), Int] =
    applyDiffToExisting(diffFromExisting(existingCounts), existingCounts)

  def applyAndGetDiff(existingCounts: Map[(Long, Long), Int]): (Map[(Long, Long), Int], Iterable[(Long, Long, Int)]) = {
    val diff = diffFromExisting(existingCounts)
    (applyDiffToExisting(diff, existingCounts), diff)
  }
}

object DailyPaxCountsOnDay {
  def applyDiffToExisting(diff: Iterable[(Long, Long, Int)],
                          existing: Map[(Long, Long), Int]): Map[(Long, Long), Int] = diff.foldLeft(existing) {
    case (stateSoFar, (pit, day, paxCount)) => stateSoFar.updated((pit, day), paxCount)
  }
}
