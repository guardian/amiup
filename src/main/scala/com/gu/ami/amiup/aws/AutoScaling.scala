package com.gu.ami.amiup.aws


import cats.data.EitherT
import com.gu.ami.amiup.InstanceRefreshProgress
import com.gu.ami.amiup.util.RichFuture
import com.typesafe.scalalogging.LazyLogging
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.autoscaling.AutoScalingAsyncClient
import software.amazon.awssdk.services.autoscaling.model._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._
import scala.jdk.FutureConverters._

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

  def pollRefreshProgress(client: AutoScalingAsyncClient, asg: AutoScalingGroup, instanceRefreshId: String)(onNext: Seq[InstanceRefreshProgress] => Unit)(implicit ec: ExecutionContext): EitherT[Future, String, Unit] = {

    def loop(instanceRefreshes: Seq[InstanceRefreshProgress]): EitherT[Future, String, Unit] = {
      onNext(instanceRefreshes)
      if (instanceRefreshes.forall(_.finished)) {
        // stop looping when we're finished
        logger.debug("Polling complete, instance refresh has finished")
        EitherT.pure(())
      } else {
        // poll again, after a delay
        logger.debug("Polling not yet complete, will repeat lookup after delay")
        for {
          // delay next execution
          _ <- EitherT.right(RichFuture.delay(10.seconds))
          // call describe instance refresh to get current status
          refreshResponse <- EitherT.right(describeInstanceRefresh(client, asg, instanceRefreshId))
          refreshes = refreshResponse.instanceRefreshes.asScala.toSeq
          progress = refreshes.map(getProgress)
          next <- loop(progress)
        } yield next
      }
    }
    val initial = InstanceRefreshProgress(asg.autoScalingGroupName, false, false, false, None, None)
    loop(Seq(initial))
  }

  private[aws] def getProgress(refresh: InstanceRefresh): InstanceRefreshProgress = {
    InstanceRefreshProgress(
      refresh.autoScalingGroupName,
      hasStarted(refresh),
      hasFailed(refresh),
      hasFinished(refresh),
      Option(refresh.status),
      Option(refresh.statusReason)
    )
  }

  private[aws] def hasStarted(refresh: InstanceRefresh): Boolean = {
    refresh.status match {
      case InstanceRefreshStatus.PENDING => false
      case InstanceRefreshStatus.IN_PROGRESS => true
      case InstanceRefreshStatus.CANCELLING => true
      case InstanceRefreshStatus.SUCCESSFUL => true
      case InstanceRefreshStatus.FAILED => true
      case InstanceRefreshStatus.CANCELLED => true
      case _ => false
    }
  }

  private[aws] def hasFinished(refresh: InstanceRefresh): Boolean = {
    refresh.status match {
      case InstanceRefreshStatus.SUCCESSFUL => true
      case InstanceRefreshStatus.FAILED => true
      case InstanceRefreshStatus.CANCELLED => true
      case _ => false
    }
  }

  private[aws] def hasFailed(refresh: InstanceRefresh): Boolean = {
    refresh.status match {
      case InstanceRefreshStatus.FAILED => true
      case InstanceRefreshStatus.CANCELLED => true
      case _ => false
    }
  }
}
