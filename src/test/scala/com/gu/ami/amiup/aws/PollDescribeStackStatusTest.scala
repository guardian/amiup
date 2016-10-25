package com.gu.ami.amiup.aws

import com.amazonaws.services.cloudformation.model.{Stack, StackStatus}
import com.gu.ami.amiup.StackProgress
import org.scalatest.{FreeSpec, Matchers}

class PollDescribeStackStatusTest extends FreeSpec with Matchers {
  import PollDescribeStackStatus._

  "updateProgress" - {
    "starts a stack that is neither started nor finished" in {
      val stack = new Stack()
        .withStackName("test-stack")
        .withStackId("test-stack")
        .withStackStatus(StackStatus.CREATE_COMPLETE)
      val notStarted = StackProgress(stack, started = false, finished = false)
      val currentStatus = stack.clone().withStackStatus(StackStatus.UPDATE_IN_PROGRESS)

      val progress = updateProgress(Seq(notStarted), Seq(currentStatus))
      progress.head.started shouldEqual true
    }

    "finishes a stack that has already started" in {
      val stack = new Stack()
        .withStackName("test-stack")
        .withStackId("test-stack")
        .withStackStatus(StackStatus.UPDATE_IN_PROGRESS)
      val inProgress = StackProgress(stack, started = true, finished = false)
      val currentStatus = stack.clone().withStackStatus(StackStatus.UPDATE_COMPLETE)

      val progress = updateProgress(Seq(inProgress), Seq(currentStatus))
      progress.head.finished shouldEqual true
    }
  }

  "complete" - {
    "is true when all stacks are started and finished" in {
      val finished1 = StackProgress(new Stack(), true, true)
      val finished2 = StackProgress(new Stack(), true, true)

      complete(Seq(finished1, finished2)) shouldEqual true
    }

    "is false if there is a stack that is nto finished" in {
      val finished = StackProgress(new Stack(), true, true)
      val unfinished = StackProgress(new Stack(), true, false)

      complete(Seq(finished, unfinished)) shouldEqual false
    }
  }
}
