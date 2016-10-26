package com.gu.ami.amiup.aws

import com.amazonaws.services.cloudformation.AmazonCloudFormationAsyncClient
import com.amazonaws.services.cloudformation.model._
import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}


object UpdateCloudFormation extends LazyLogging {
  private val allowedStatuses = List(
    StackStatus.CREATE_COMPLETE, StackStatus.ROLLBACK_COMPLETE, StackStatus.UPDATE_COMPLETE,
    StackStatus.UPDATE_ROLLBACK_COMPLETE
  )

  def findStacks(existingAmiOrStacks: Either[String, Seq[String]], parameterName: String, client: AmazonCloudFormationAsyncClient): Future[Seq[Stack]] = {
    for {
      describeStacksResult <- AWS.describeStacks(client)
    } yield {
      val allStacks = describeStacksResult.getStacks.asScala
      logger.info(s"Found ${allStacks.size} stacks")
      logger.debug(s"stacks: ${allStacks.map(_.getStackName).mkString(", ")}")
      val matchingStacks = existingAmiOrStacks match {
        case Left(existingAmi) =>
          allStacks.filter(filterStack(existingAmi, parameterName))
        case Right(specifiedStacks) =>
          allStacks.filter(stack => specifiedStacks.contains(stack.getStackId))
      }
      logger.info(s"Using ${matchingStacks.size} stacks")
      logger.debug(s"Matching stacks: ${matchingStacks.map(_.getStackName).mkString(", ")}")
      matchingStacks
    }
  }

  def validateStacks(parameterName: String, stacks: Seq[Stack]): Either[String, Seq[Stack]] = {
    val stacksWithoutAmiParameter = stacks.filterNot { stack =>
      stack.getParameters.asScala.exists(_.getParameterKey == parameterName)
    }

    if (stacks.isEmpty) Left("No stacks found")
    else if (stacksWithoutAmiParameter.isEmpty) Right(stacks)
    else Left(s"The following stacks do not have a `$parameterName` parameter: ${stacksWithoutAmiParameter.map(_.getStackName).mkString(",")}")
  }

  def updateStacks(stacks: Seq[Stack], newAmi: String, parameterName: String, client: AmazonCloudFormationAsyncClient)(implicit ec: ExecutionContext): Future[Map[Stack, UpdateStackResult]] = {
    Future.traverse(stacks)(updateStack(newAmi, parameterName, client)).map(_.toMap)
  }

  def updateStack(newAmi: String, parameterName: String, client: AmazonCloudFormationAsyncClient)(stack: Stack): Future[(Stack, UpdateStackResult)] = {
    val updateStackRequest = new UpdateStackRequest()
      .withStackName(stack.getStackName)
      .withUsePreviousTemplate(true)
      .withCapabilities(Capability.CAPABILITY_IAM)
      .withParameters(stack.getParameters.asScala.map {
        case parameter if parameter.getParameterKey == parameterName =>
          new Parameter()
            .withParameterKey(parameterName)
            .withParameterValue(newAmi)
        case parameter =>
          parameter
      }.asJava)

    AWS.updateStack(updateStackRequest, client).map(stack -> _)
  }

  private[amiup] def filterStack(sourceAmi: String, parameterName: String)(stack: Stack): Boolean = {
    val isIncluded = allowedStatuses.contains(StackStatus.fromValue(stack.getStackStatus)) && {
      stack.getParameters.asScala
        .find(_.getParameterKey == parameterName)
        .exists(_.getParameterValue == sourceAmi)
    }
    logger.debug(s"Stack ${stack.getStackName} ${if(isIncluded) "included" else "excluded"}")
    isIncluded
  }
}
