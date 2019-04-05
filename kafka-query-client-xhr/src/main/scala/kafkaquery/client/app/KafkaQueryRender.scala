package kafkaquery.client.app

import scalatags.JsDom.all._

//import scalatags.Text.all.html

/**
  * http://www.lihaoyi.com/scalatags/
  */
object KafkaQueryRender {

  def render = {
    html(
      head(
        script("some script")
      ),
      body(
        h1("This is my title"),
        div(
          p("This is my first paragraph"),
          p("This is my second paragraph")
        )
      )
    )
  }
}
