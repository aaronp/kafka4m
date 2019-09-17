import kafka4m.Kafka4mApp

object ETLReaderExample extends App {
  dockerenv.kafka().start()
  Kafka4mApp.main(Array("read", "example/local/local-etl.conf"))
}
