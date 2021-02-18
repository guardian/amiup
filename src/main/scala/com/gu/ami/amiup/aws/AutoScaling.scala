package com.gu.ami.amiup.aws


import com.typesafe.scalalogging.LazyLogging
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.autoscaling.AutoScalingAsyncClient
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.services.autoscaling.model._

import scala.concurrent.Future
import scala.jdk.FutureConverters._
import scala.jdk.CollectionConverters._

object AutoScaling extends LazyLogging {

  def client(profile: String, region: Region): AutoScalingAsyncClient = {
    AutoScalingAsyncClient.builder()
      .credentialsProvider(ProfileCredentialsProvider.builder().profileName(profile).build())
      .region(region)
      .build()
  }

  def describeAutoScalingGroup(client: AutoScalingAsyncClient, asgName: String): Future[DescribeAutoScalingGroupsResponse] = {
    val request = DescribeAutoScalingGroupsRequest
      .builder().autoScalingGroupNames(asgName).build()
    client.describeAutoScalingGroups(request).asScala
  }

  def parseAutoScalingGroups(asgName: String, response: DescribeAutoScalingGroupsResponse): Either[String, AutoScalingGroup] = {
    val allAsgs = response.autoScalingGroups.asScala.toSeq
    val matchingAsgs = allAsgs.filter(asg => asg.autoScalingGroupName.contains(asgName))

    matchingAsgs match {
      case Nil => Left(s"AutoScaling group $asgName could not be found")
      case targetAsg :: Nil => Right(targetAsg)
      case _ => Left(s"Multiple groups found for $asgName, the name should be unique")
    }
  }

  def startInstanceRefresh(client: AutoScalingAsyncClient, asg: AutoScalingGroup): Future[StartInstanceRefreshResponse] = {
    val request = StartInstanceRefreshRequest.builder()
      .autoScalingGroupName(asg.autoScalingGroupName).build()
    client.startInstanceRefresh(request).asScala
  }

  // If the update process fails, any instances already replaced are not rolled back to their previous configuration.
  // Rolling updates can fail due to failed health checks or if instances are on standby or are protected from scale in.
  def describeInstanceRefresh(client: AutoScalingAsyncClient, asg: AutoScalingGroup, instanceRefreshId: String): Future[DescribeInstanceRefreshesResponse] = {
    val request = DescribeInstanceRefreshesRequest.builder()
      .autoScalingGroupName(asg.autoScalingGroupName)
      .instanceRefreshIds(instanceRefreshId)
      .build()
    client.describeInstanceRefreshes(request).asScala
  }

  private[aws] def isFinished(refresh: InstanceRefresh): Boolean = {
    refresh.status match {
      case InstanceRefreshStatus.PENDING => false
      case InstanceRefreshStatus.IN_PROGRESS => false
      case InstanceRefreshStatus.CANCELLING => false
      case InstanceRefreshStatus.SUCCESSFUL => true
      case InstanceRefreshStatus.FAILED => true
      case InstanceRefreshStatus.CANCELLED => true
      case _ => false
    }
  }

}
