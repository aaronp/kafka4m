package pipelines.data.shape

/**
  * Represents a repository of functions
  *
  * @param functions
  * @param functors
  * @tparam T
  */
class Computer[T: CanCompute](functions: Seq[T], functors: Seq[Map2]) {

  import Computer.Step
  type CalcSteps    = Seq[Step[T]]
  type CalcSequence = Seq[T]

  private val canCompute: CanCompute[T]    = implicitly[CanCompute[T]]
  private def asCompute(value: T): Compute = canCompute.compute(value)
  private lazy val functorByName = functors.groupBy(_.name).mapValues {
    case Seq(only) => only
    case many      => sys.error(s"${many.size} functors found for ${many.head.name}")
  }

  def containing(param: Shape) = {
    functions.filter { tea =>
      asCompute(tea).outputFor(param).isDefined
    }
  }

  /**
    * figure out the ways we can compute a given type from an input type
    *
    * @param from
    * @param to
    * @return all the ways we can get to the 'to' solution given the functions we have
    */
  def paths(from: Shape, to: Shape): Seq[CalcSequence] = {
    findPaths(from, _ == to, 10).map(_.map(_.value))
  }

  def findPaths(from: Shape, endState: Shape => Boolean, depth: Int): Seq[CalcSteps] = {
    functions match {
      case empty if empty.isEmpty || depth == 0 => Nil
      case many =>
        many.flatMap { startPoint: T =>
          val output = asCompute(startPoint).outputFor(from)
          output match {
            case Some(to) if endState(to) =>
              val singleStep = Step(startPoint, asCompute(startPoint), from, to)
              Seq(Seq(singleStep))
            case Some(nextInput: Shape) =>
              // this calculation doesn't produce the output we want

              val solutions: Seq[CalcSteps] = findPaths(nextInput, endState, depth - 1)

              // if the output is a parameterized type, we could return a parameterized response
              // e.g. if the output is F[A], and we have a function A=>B, then we could return F[A=>B]
              if (solutions.isEmpty) {
                nextInput match {
                  case ParameterizedShape(name, Seq(singleTypeParam)) if functorByName.contains(name) =>
                    // find a function which can take our parameterized type as an input and produce an output
                    val solutionsForParameterizedType: Seq[CalcSteps] = findPaths(singleTypeParam, endState, depth - 1)
                    solutionsForParameterizedType.map { solution =>
                      val singleCompute  = solution.map(_.compute).reduce(_ andThen _)
                      val desc           = solution.map(_.value).mkString(" andThen ")
                      val functor        = functorByName(name)
                      val functorCompute = functor.asCompute(asCompute(startPoint), singleCompute)
                      val functorOutput  = ParameterizedShape(functor.name, Seq(solution.last.output))
                      // functor solution
                      val tea: T = canCompute.lift(s"${name}[${desc}]", functorCompute)
                      Step(tea, functorCompute, from, functorOutput) :: Nil
                    }
                  case _ => Nil
                }
              } else {
                val stepOutput = nextInput
                val singleStep = Step(startPoint, asCompute(startPoint), from, stepOutput)
                solutions.map(singleStep +: _)
              }

            case None => Nil
          }
        }
    }
  }
}

object Computer {

  case class Step[T](value: T, compute: Compute, input: Shape, output: Shape)

  def apply[T: CanCompute](calcs: T*): Computer[T] = {
    new Computer(calcs.toSeq, Map2.values)
  }
  def apply[T: CanCompute](calcs: Seq[T], functors: Seq[Map2]) = new Computer(calcs, functors)

}
