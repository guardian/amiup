package com.gu.ami.amiup.aws

import com.typesafe.scalalogging.LazyLogging
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cloudformation.CloudFormationAsyncClient
import software.amazon.awssdk.services.cloudformation.model.{DescribeStacksRequest, DescribeStacksResponse, UpdateStackRequest, UpdateStackResponse}

import scala.concurrent.Future
import scala.jdk.FutureConverters._


object AWS extends LazyLogging {
  def describeStacks(client: CloudFormationAsyncClient, stackName: Option[String]): Future[DescribeStacksResponse] = {
    val request = stackName.fold(DescribeStacksRequest.builder().build()) { name =>
      DescribeStacksRequest.builder().stackName(name).build()
    }
    client.describeStacks(request).asScala
  }

  def updateStack(updateStackRequest: UpdateStackRequest, client: CloudFormationAsyncClient): Future[UpdateStackResponse] = {
    client.updateStack(updateStackRequest).asScala
  }

  def client(profile: String, region: Region): CloudFormationAsyncClient = {
    CloudFormationAsyncClient.builder()
      .credentialsProvider(ProfileCredentialsProvider.builder().profileName(profile).build())
      .region(region)
      .build()
  }
}
