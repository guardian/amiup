package com.gu.ami.amiup.aws

import com.typesafe.scalalogging.LazyLogging
import software.amazon.awssdk.services.cloudformation.CloudFormationAsyncClient
import software.amazon.awssdk.services.cloudformation.model._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.jdk.CollectionConverters._


object UpdateCloudFormation extends LazyLogging {
  private val allowedStatuses = List(
    StackStatus.CREATE_COMPLETE, StackStatus.ROLLBACK_COMPLETE, StackStatus.UPDATE_COMPLETE,
    StackStatus.UPDATE_ROLLBACK_COMPLETE
  )

  def findStacks(existingAmiOrStacks: Either[String, Seq[String]], parameterName: String, client: CloudFormationAsyncClient): Future[Seq[Stack]] = {
    for {
      describeStacksResponse <- AWS.describeStacks(client)
    } yield {
      val allStacks = describeStacksResponse.stacks.asScala.toSeq
      logger.info(s"Found ${allStacks.size} stacks")
      logger.debug(s"stacks: ${allStacks.map(_.stackName).mkString(", ")}")
      val matchingStacks = existingAmiOrStacks match {
        case Left(existingAmi) =>
          allStacks.filter(filterStack(existingAmi, parameterName))
        case Right(specifiedStacks) =>
          allStacks.filter(stack => specifiedStacks.contains(stack.stackId))
      }
      logger.info(s"Using ${matchingStacks.size} stacks")
      logger.debug(s"Matching stacks: ${matchingStacks.map(_.stackName).mkString(", ")}")
      matchingStacks
    }
  }

  def validateStacks(parameterName: String, stacks: Seq[Stack]): Either[String, Seq[Stack]] = {
    val stacksWithoutAmiParameter = stacks.filterNot { stack =>
      stack.parameters.asScala.exists(_.parameterKey == parameterName)
    }

    if (stacks.isEmpty) Left("No stacks found")
    else if (stacksWithoutAmiParameter.isEmpty) Right(stacks)
    else Left(s"The following stacks do not have a `$parameterName` parameter: ${stacksWithoutAmiParameter.map(_.stackName).mkString(",")}")
  }

  def updateStacks(stacks: Seq[Stack], newAmi: String, parameterName: String, client: CloudFormationAsyncClient): Future[Map[Stack, UpdateStackResponse]] = {
    Future.traverse(stacks)(updateStack(newAmi, parameterName, client)).map(_.toMap)
  }

  def updateStack(newAmi: String, parameterName: String, client: CloudFormationAsyncClient)(stack: Stack): Future[(Stack, UpdateStackResponse)] = {
    val updateStackRequest = UpdateStackRequest.builder()
      .stackName(stack.stackName)
      .usePreviousTemplate(true)
      .capabilities(Capability.CAPABILITY_IAM)
      .parameters(stack.parameters.asScala.map {
        case parameter if parameter.parameterKey == parameterName =>
          Parameter.builder()
            .parameterKey(parameterName)
            .parameterValue(newAmi)
            .build()
        case parameter =>
          parameter
      }.asJava)
      .build()

    AWS.updateStack(updateStackRequest, client).map(stack -> _)
  }

  private[amiup] def filterStack(sourceAmi: String, parameterName: String)(stack: Stack): Boolean = {
    val isIncluded = allowedStatuses.contains(stack.stackStatus) && {
      stack.parameters.asScala
        .find(_.parameterKey == parameterName)
        .exists(_.parameterValue == sourceAmi)
    }
    logger.debug(s"Stack ${stack.stackName} ${if(isIncluded) "included" else "excluded"}")
    isIncluded
  }
}
