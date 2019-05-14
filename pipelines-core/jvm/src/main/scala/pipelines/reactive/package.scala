package pipelines

import monix.execution.Scheduler

package object reactive {

  type NewSource = Scheduler => Data
  def NewSource(data: Data): NewSource = (_: Scheduler) => data
  def NewSource(create : Scheduler => Data): NewSource = create

}
