package com.gu.ami.amiup.aws

import com.gu.ami.amiup.StackProgress
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import software.amazon.awssdk.services.cloudformation.model.{Stack, StackStatus}

class PollDescribeStackStatusTest extends AnyFreeSpec with Matchers {
  import PollDescribeStackStatus._

  private def createStack(status: StackStatus): Stack = {
    Stack.builder()
      .stackName("test-stack")
      .stackId("test-stack")
      .stackStatus(status)
      .build()
  }

  "updateProgress" - {
    "starts a stack that is neither started nor finished, if its status says it has started" in {
      val stack = createStack(StackStatus.CREATE_COMPLETE)
      val prevProgress = StackProgress(stack, started = false, finished = false, failed = false)
      val currentStatus = createStack(StackStatus.UPDATE_IN_PROGRESS)

      val progress = updateProgress(Seq(prevProgress), Seq(currentStatus))
      progress.head.started shouldEqual true
    }

    "does not start a stack if its status hasn't begun" in {
      val stack = createStack(StackStatus.CREATE_COMPLETE)
      val prevProgress = StackProgress(stack, started = false, finished = false, failed = false)
      val currentStatus = createStack(StackStatus.CREATE_COMPLETE)

      val progress = updateProgress(Seq(prevProgress), Seq(currentStatus))
      progress.head.started shouldEqual false
    }

    "finishes a stack that has already started" in {
      val stack = createStack(StackStatus.UPDATE_IN_PROGRESS)
      val prevProgress = StackProgress(stack, started = true, finished = false, failed = false)
      val currentStatus = createStack(StackStatus.UPDATE_COMPLETE)

      val progress = updateProgress(Seq(prevProgress), Seq(currentStatus))
      progress.head.finished shouldEqual true
    }

    "fails a stack that has started and is in a failure state" in {
      val stack = createStack(StackStatus.UPDATE_IN_PROGRESS)
      val prevProgress = StackProgress(stack, started = true, finished = false, failed = false)
      val currentStatus = createStack(StackStatus.UPDATE_ROLLBACK_IN_PROGRESS)

      val progress = updateProgress(Seq(prevProgress), Seq(currentStatus))
      progress.head.failed shouldEqual true
    }

    "does not fail a stack that in a failure state if it has not started (must be left over from the last update)" in {
      val stack = createStack(StackStatus.UPDATE_ROLLBACK_COMPLETE)
      val prevProgress = StackProgress(stack, started = false, finished = false, failed = false)
      val currentStatus = createStack(StackStatus.UPDATE_ROLLBACK_COMPLETE)

      val progress = updateProgress(Seq(prevProgress), Seq(currentStatus))
      progress.head.failed shouldEqual false
    }
  }

  "complete" - {
    "is true when all stacks are started and finished" in {
      val finished1 = StackProgress(Stack.builder().build(), started = true, finished = true, failed = false)
      val finished2 = StackProgress(Stack.builder().build(), started = true, finished = true, failed = false)

      complete(Seq(finished1, finished2)) shouldEqual true
    }

    "is false if there is a stack that is not finished" in {
      val finished = StackProgress(Stack.builder().build(), started = true, finished = true, failed = false)
      val unfinished = StackProgress(Stack.builder().build(), started = true, finished = false, failed = false)

      complete(Seq(finished, unfinished)) shouldEqual false
    }
  }
}
