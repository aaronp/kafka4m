package esa.client
import quickstart.{Counter, Increment}

object CounterApp {

  import scala.scalajs.js

  /**
    * Performs an XMLHttpRequest on the `currentValue` endpoint, and then
    * deserializes the JSON response as a `Counter`.
    */
  val eventuallyCounter: js.Thenable[Counter] = CounterClient.currentValue(())

  val eventuallyDone: js.Thenable[Unit] = CounterClient.increment(Increment(42))

}
