package com.gu.ami.amiup

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import software.amazon.awssdk.services.cloudformation.model.{Parameter, Stack, StackStatus}


class UpdateCloudFormationTest extends AnyFreeSpec with Matchers {
  import com.gu.ami.amiup.aws.UpdateCloudFormation._

  "updateStacks" - {
    val sourceAmi = "ami-abc123"
    val parameterName = "AMI"

    "filters out a stack with an invalid status" in {
      val stack = Stack.builder().stackStatus(StackStatus.CREATE_FAILED).build()
      filterStack(sourceAmi, parameterName)(stack) shouldEqual false
    }

    "if the status is valid" - {
      val status = StackStatus.CREATE_COMPLETE

      "filters out a stack with no parameters" in {
        val stack = Stack.builder().stackStatus(status).build()
        filterStack(sourceAmi, parameterName)(stack) shouldEqual false
      }

      "filters out a stack without a matching parameter name" in {
        val parameter = Parameter.builder()
          .parameterKey("foo")
          .parameterValue("bar")
          .build()
        val stack = Stack.builder().stackStatus(status).parameters(parameter).build()
        filterStack(sourceAmi, parameterName)(stack) shouldEqual false
      }

      "filters out a stack that has a matching parameter name with a different value" in {
        val parameter = Parameter.builder()
          .parameterKey(parameterName)
          .parameterValue("different-value")
          .build()
        val stack = Stack.builder().stackStatus(status).parameters(parameter).build()
        filterStack(sourceAmi, parameterName)(stack) shouldEqual false
      }

      "includes a stack with a matching parameter name/value" in {
        val matchingParameter = Parameter.builder()
          .parameterKey(parameterName)
          .parameterValue(sourceAmi)
          .build()
        val stack = Stack.builder().stackStatus(status).parameters(matchingParameter).build()
        filterStack(sourceAmi, parameterName)(stack) shouldEqual true
      }
    }
  }
}
