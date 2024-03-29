package uk.gov.homeoffice.drt.analytics.actors

import akka.actor.Props
import akka.persistence._
import org.joda.time.DateTimeZone
import org.slf4j.{Logger, LoggerFactory}
import uk.gov.homeoffice.drt.analytics.messages.MessageConversion
import uk.gov.homeoffice.drt.analytics.{Arrivals, SimpleArrival}
import uk.gov.homeoffice.drt.arrivals.UniqueArrival
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.protobuf.messages.FlightsMessage.{FeedStatusMessage, FlightMessage, FlightStateSnapshotMessage, FlightsDiffMessage}
import uk.gov.homeoffice.drt.time.{SDate, SDateLike}

import scala.collection.mutable

case class GetArrivals(firstDay: SDateLike, lastDay: SDateLike)

object ArrivalsActor {
  def props: (String, SDateLike) => Props = (persistenceId: String, date: SDateLike) =>
    Props(new ArrivalsActor(persistenceId, date))
}

class ArrivalsActor(val persistenceId: String, date: SDateLike) extends PersistentActor {
  val log: Logger = LoggerFactory.getLogger(getClass)

  var arrivals: mutable.Map[UniqueArrival, SimpleArrival] = mutable.Map()
  val pointInTime: SDateLike = date

  override def receiveRecover: Receive = {
    case SnapshotOffer(_, FlightStateSnapshotMessage(flightMessages, _)) =>
      val incoming = simpleArrivalsFromMessages(flightMessages).map(a => (a.uniqueArrival, a))
      arrivals ++= incoming

    case FlightsDiffMessage(Some(createdAt), removals, updates, _) =>
      if (createdAt <= pointInTime.millisSinceEpoch) {
        arrivals --= removals.map(m => UniqueArrival(m.number.getOrElse(0), m.terminalName.getOrElse(""), m.scheduled.getOrElse(0L), m.origin.getOrElse("")))
        val incomingUpdates = simpleArrivalsFromMessages(updates).map(a => (a.uniqueArrival, a))
        arrivals ++= incomingUpdates
      }

    case _: FeedStatusMessage =>

    case RecoveryCompleted =>
      log.debug(s"Recovery completed for $persistenceId at $date: ${arrivals.size} arrivals")

    case u =>
      log.info(s"Got unexpected recovery msg: $u")
  }

  private def simpleArrivalsFromMessages(updates: Seq[FlightMessage]): Seq[SimpleArrival] = updates
    .filter(msg => SDate(msg.getScheduled).toUtcDate == date.toUtcDate)
    .filterNot(a => PortCode(a.getOrigin).isDomesticOrCta)
    .map(MessageConversion.fromFlightMessage)

  override def receiveCommand: Receive = {
    case GetArrivals(start, end) =>
      sender() ! Arrivals(Map() ++ arrivals.filter { case (_, a) =>
        val arrivalDate = SDate(a.scheduled, DateTimeZone.forID("Europe/London")).toISODateOnly
        start.toISODateOnly <= arrivalDate && arrivalDate <= end.toISODateOnly
      })
    case u =>
      log.info(s"Got unexpected command: $u")
  }

  override def recovery: Recovery = {
    val criteria = SnapshotSelectionCriteria(maxTimestamp = pointInTime.millisSinceEpoch)
    Recovery(fromSnapshot = criteria, replayMax = 500)
  }
}

object FeedPersistenceIds {
  val forecastBase = "actors.ForecastBaseArrivalsActor-forecast-base"
  val forecast = "actors.ForecastPortArrivalsActor-forecast-port"
  val liveBase = "actors.LiveBaseArrivalsActor-live-base"
  val live = "actors.LiveArrivalsActor-live"
}
