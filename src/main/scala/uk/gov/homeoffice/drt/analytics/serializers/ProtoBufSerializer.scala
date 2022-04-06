package uk.gov.homeoffice.drt.analytics.serializers

import akka.serialization.SerializerWithStringManifest
import uk.gov.homeoffice.drt.protobuf.messages.CrunchState.{FlightsWithSplitsDiffMessage, FlightsWithSplitsMessage}
import uk.gov.homeoffice.drt.protobuf.messages.FlightsMessage._
import uk.gov.homeoffice.drt.protobuf.messages.ModelAndFeatures.ModelAndFeaturesMessage
import uk.gov.homeoffice.drt.protobuf.messages.PaxMessage.{OriginTerminalPaxCountsMessage, OriginTerminalPaxCountsMessages, PaxCountMessage}

class ProtoBufSerializer extends SerializerWithStringManifest {
  override def identifier: Int = 9001

  override def manifest(targetObject: AnyRef): String = targetObject.getClass.getName

  final val FlightsDiff: String                   = classOf[FlightsDiffMessage].getName
  final val FlightStateSnapshot: String           = classOf[FlightStateSnapshotMessage].getName
  final val Flight: String                        = classOf[FlightMessage].getName
  final val UniqueArrival: String                 = classOf[UniqueArrivalMessage].getName
  final val FeedStatus: String                    = classOf[FeedStatusMessage].getName
  final val FeedStatuses: String                  = classOf[FeedStatusesMessage].getName
  final val PaxCount: String                      = classOf[PaxCountMessage].getName
  final val OriginTerminalPaxCounts: String       = classOf[OriginTerminalPaxCountsMessage].getName
  final val OriginTerminalPaxCountss: String      = classOf[OriginTerminalPaxCountsMessages].getName
  final val FlightsWithSplits: String             = classOf[FlightsWithSplitsMessage].getName
  final val FlightsWithSplitsDiff: String         = classOf[FlightsWithSplitsDiffMessage].getName
  final val ModelAndFeatures: String              = classOf[ModelAndFeaturesMessage].getName

  override def toBinary(objectToSerialize: AnyRef): Array[Byte] = {
    objectToSerialize match {
      case m: FlightsDiffMessage => m.toByteArray
      case m: FlightStateSnapshotMessage => m.toByteArray
      case m: FlightMessage => m.toByteArray
      case m: UniqueArrivalMessage => m.toByteArray
      case m: FeedStatusMessage => m.toByteArray
      case m: FeedStatusesMessage => m.toByteArray
      case m: PaxCountMessage => m.toByteArray
      case m: OriginTerminalPaxCountsMessage => m.toByteArray
      case m: OriginTerminalPaxCountsMessages => m.toByteArray
      case m: FlightsWithSplitsMessage => m.toByteArray
      case m: FlightsWithSplitsDiffMessage => m.toByteArray
      case m: ModelAndFeaturesMessage => m.toByteArray
    }
  }

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = {
    manifest match {
      case FlightsDiff                    => FlightsDiffMessage.parseFrom(bytes)
      case FlightStateSnapshot            => FlightStateSnapshotMessage.parseFrom(bytes)
      case Flight                         => FlightMessage.parseFrom(bytes)
      case UniqueArrival                  => UniqueArrivalMessage.parseFrom(bytes)
      case FeedStatus                     => FeedStatusMessage.parseFrom(bytes)
      case FeedStatuses                   => FeedStatusesMessage.parseFrom(bytes)
      case PaxCount                       => PaxCountMessage.parseFrom(bytes)
      case OriginTerminalPaxCounts        => OriginTerminalPaxCountsMessage.parseFrom(bytes)
      case OriginTerminalPaxCountss       => OriginTerminalPaxCountsMessages.parseFrom(bytes)
      case FlightsWithSplits              => FlightsWithSplitsMessage.parseFrom(bytes)
      case FlightsWithSplitsDiff          => FlightsWithSplitsDiffMessage.parseFrom(bytes)
      case ModelAndFeatures               => ModelAndFeaturesMessage.parseFrom(bytes)
    }
  }
}
