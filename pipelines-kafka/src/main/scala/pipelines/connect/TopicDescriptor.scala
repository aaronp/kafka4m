package pipelines.connect

/**
  * Represents data we have about a topic
  */
sealed trait TopicDescriptor

case class AvroDescriptor(avroSchema: String)          extends TopicDescriptor
case class ProtobufDescriptor(protobuffSchema: String) extends TopicDescriptor
case object JsonDescriptor                             extends TopicDescriptor
