import kafka4m.Kafka4mApp

object ETLWriterExample extends App {
  dockerenv.kafka().start()
  Kafka4mApp.main(Array("write", "example/local/local-etl.conf"))
}
