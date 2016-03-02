package scraper

import org.scalatest.FunSuite

import scraper.expressions.{Alias, AttributeRef, Expression, ExpressionId}
import scraper.plans.QueryPlan
import scraper.plans.logical.LogicalPlan
import scraper.trees.TreeNode
import scraper.types.DataType
import scraper.utils._

trait TestUtils { this: FunSuite =>
  private[scraper] def assertSideBySide(expected: String, actual: String): Unit = {
    if (expected != actual) {
      fail(sideBySide(
        s"""Expected
           |$expected
           |""".stripMargin,

        s"""Actual
           |$actual
           |""".stripMargin,

        withHeader = true
      ))
    }
  }

  private[scraper] def assertSideBySide[T <: TreeNode[T]](
    expected: TreeNode[T],
    actual: TreeNode[T]
  ): Unit = {
    if (expected != actual) {
      fail(sideBySide(
        s"""Expected
           |${expected.prettyTree}
           |""".stripMargin,

        s"""Actual
           |${actual.prettyTree}
           |""".stripMargin,

        withHeader = true
      ))
    }
  }

  private[scraper] def checkTree[T <: TreeNode[T]](
    expected: TreeNode[T],
    actual: TreeNode[T]
  ): Unit = {
    assertSideBySide(expected, actual)
  }

  private def normalizeExpressionId[Plan <: QueryPlan[Plan]](plan: Plan): Plan = {
    var normalizedId = -1L

    plan.transformAllExpressions {
      case e: AttributeRef =>
        normalizedId += 1
        e.copy(expressionId = ExpressionId(normalizedId))

      case e: Alias =>
        normalizedId += 1
        e.copy(expressionId = ExpressionId(normalizedId))
    }
  }

  def checkPlan[Plan <: QueryPlan[Plan]](actual: Plan, expected: Plan): Unit = {
    checkTree(normalizeExpressionId(expected), normalizeExpressionId(actual))
  }

  def checkDataFrame(actual: DataFrame, expected: DataFrame): Unit =
    checkDataFrame(actual, expected.toSeq)

  def checkDataFrame(ds: DataFrame, expected: Row): Unit =
    checkDataFrame(ds, expected :: Nil)

  def checkDataFrame(ds: DataFrame, expected: => Seq[Row]): Unit = {
    val actual = ds.queryExecution.physicalPlan.iterator.toSeq
    if (actual != expected) {
      val explanation = ds.explanation(extended = true)

      val answerDiff = sideBySide(
        s"""Expected answer:
           |${expected mkString "\n"}
           |""".stripMargin,

        s"""Actual answer:
           |${actual mkString "\n"}
           |""".stripMargin,

        withHeader = true
      )

      fail(
        s"""Unexpected row(s) detected:
           |
           |$answerDiff
           |
           |Query plan details:
           |
           |$explanation
           |""".stripMargin
      )
    }
  }

  def checkWellTyped(e: Expression, dataType: DataType): Unit = {
    if (!e.wellTyped) {
      fail(
        s"""Expression ${e.debugString} is not well-typed:
           |${e.prettyTree}
           |""".stripMargin
      )
    }

    val actualType = e.strictlyTypedForm.get.dataType
    if (actualType != dataType) {
      fail(
        s"""Strictly typed form of ${e.debugString} has wrong data type $actualType:
           |${e.prettyTree}
           |""".stripMargin
      )
    }
  }

  def checkStrictlyTyped(e: Expression, dataType: DataType): Unit = {
    if (!e.strictlyTyped) {
      fail(
        s"""Expression ${e.debugString} is not strictly-typed:
           |${e.prettyTree}
           |""".stripMargin
      )
    }

    val actualType = e.strictlyTypedForm.get.dataType
    if (actualType != dataType) {
      fail(
        s"""Strictly typed form of ${e.debugString} has wrong data type $actualType:
           |${e.prettyTree}
           |""".stripMargin
      )
    }
  }

  def checkWellTyped(plan: LogicalPlan): Unit = {
    if (!plan.wellTyped) {
      fail(
        s"""Logical plan not well-typed:
           |${plan.prettyTree}
           |""".stripMargin
      )
    }
  }

  def checkStrictlyTyped(plan: LogicalPlan): Unit = {
    if (!plan.wellTyped) {
      fail(
        s"""Logical plan not well-typed:
           |${plan.prettyTree}
           |""".stripMargin
      )
    }

    if (!plan.strictlyTyped) {
      fail(
        s"""Logical plan is well typed but not strictly-typed:
           |
           |# Original logical plan:
           |${plan.prettyTree}
           |
           |# Well-typed logical plan:
           |${plan.strictlyTypedForm.get.prettyTree}
           |""".stripMargin
      )
    }
  }
}