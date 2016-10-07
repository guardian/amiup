package com.gu.ami.amiup

import com.amazonaws.services.cloudformation.AmazonCloudFormationAsyncClient
import com.amazonaws.services.cloudformation.model._

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


object UpdateCloudFormation {
  private val allowedStatuses = List(
    StackStatus.CREATE_COMPLETE, StackStatus.ROLLBACK_COMPLETE, StackStatus.UPDATE_COMPLETE,
    StackStatus.UPDATE_ROLLBACK_COMPLETE
  )

  def findStacks(existingAmiOrStacks: Either[String, Seq[String]], parameterName: String, client: AmazonCloudFormationAsyncClient): Future[Seq[Stack]] = {
    for {
      describeStacksResult <- AWS.describeStacks(client)
    } yield {
      existingAmiOrStacks match {
        case Left(existingAmi) =>
          describeStacksResult.getStacks.asScala
            .filter(filterStack(existingAmi, parameterName))
            .toList
        case Right(stacks) =>
          describeStacksResult.getStacks.asScala
              .filter(stack => stacks.contains(stack.getStackId))
      }
    }
  }

  def validateStacks(parameterName: String, stacks: Seq[Stack]): Either[String, Seq[Stack]] = {
    val stacksWithoutAmiParameter = stacks.filterNot { stack =>
      stack.getParameters.asScala.exists(_.getParameterKey == parameterName)
    }
    if (stacksWithoutAmiParameter.isEmpty) Right(stacks)
    else Left(s"The following stacks do not have a `$parameterName` parameter: ${stacksWithoutAmiParameter.map(_.getStackName).mkString(",")}")
  }

  def updateStacks(stacks: List[Stack], newAmi: String, parameterName: String)(implicit client: AmazonCloudFormationAsyncClient): Future[Map[Stack, UpdateStackResult]] = {
    Future.traverse(stacks)(updateStack(newAmi, parameterName)).map(_.toMap)
  }

  def updateStack(newAmi: String, parameterName: String)(stack: Stack)(implicit client: AmazonCloudFormationAsyncClient): Future[(Stack, UpdateStackResult)] = {
    val updatedParameters = stack.getParameters.asScala.map {
      case parameter if parameter.getParameterKey == parameterName =>
        new Parameter()
          .withParameterKey(parameterName)
          .withParameterValue(newAmi)
      case parameter =>
        parameter
    }
    val updateStackRequest = new UpdateStackRequest()
      .withParameters(updatedParameters.asJava)

    AWS.updateStack(updateStackRequest, client).map(stack -> _)
  }

  private[amiup] def filterStack(sourceAmi: String, parameterName: String)(stack: Stack): Boolean = {
    allowedStatuses.contains(StackStatus.fromValue(stack.getStackStatus)) && {
      stack.getParameters.asScala.toList
        .find(_.getParameterKey == parameterName)
        .exists(_.getParameterValue == sourceAmi)
    }
  }
}
