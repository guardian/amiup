package com.gu.ami.amiup

import com.amazonaws.services.cloudformation.model.{Parameter, Stack, StackStatus}
import org.scalatest.{FreeSpec, Matchers}


class UpdateCloudFormationTest extends FreeSpec with Matchers {
  import com.gu.ami.amiup.aws.UpdateCloudFormation._

  "updateStacks" - {
    val sourceAmi = "ami-abc123"
    val parameterName = "AMI"

    "filters out a stack with an invalid status" in {
      val stack = new Stack()
          .withStackStatus(StackStatus.CREATE_FAILED)
      filterStack(sourceAmi, parameterName)(stack) shouldEqual false
    }

    "if the status is valid" - {
      val status = StackStatus.CREATE_COMPLETE

      "filters out a stack with no parameters" in {
        val stack = new Stack()
          .withStackStatus(status)
          .withParameters()
        filterStack(sourceAmi, parameterName)(stack) shouldEqual false
      }

      "filters out a stack without a matching parameter name" in {
        val parameter = new Parameter().withParameterKey("foo").withParameterValue("bar")
        val stack = new Stack()
          .withStackStatus(status)
          .withParameters(parameter)
        filterStack(sourceAmi, parameterName)(stack) shouldEqual false
      }

      "filters out a stack that has a matching parameter name with a different value" in {
        val parameter = new Parameter().withParameterKey(parameterName).withParameterValue("different-value")
        val stack = new Stack()
          .withStackStatus(status)
          .withParameters(parameter)
        filterStack(sourceAmi, parameterName)(stack) shouldEqual false
      }

      "includes a stack with a matching parameter name/value" in {
        val matchingParameter = new Parameter().withParameterKey(parameterName).withParameterValue(sourceAmi)
        val stack = new Stack()
          .withStackStatus(status)
          .withParameters(matchingParameter)
        filterStack(sourceAmi, parameterName)(stack) shouldEqual true
      }
    }
  }
}
