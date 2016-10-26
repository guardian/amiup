package com.gu.ami.amiup.aws

import com.amazonaws.AmazonWebServiceRequest
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.cloudformation.AmazonCloudFormationAsyncClient
import com.amazonaws.services.cloudformation.model._
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.{Future, Promise}


object AWS extends LazyLogging {
  def describeStacks(client: AmazonCloudFormationAsyncClient): Future[DescribeStacksResult] = {
    val request = new DescribeStacksRequest()
    asFuture(client.describeStacksAsync)(request)
  }

  def updateStack(updateStackRequest: UpdateStackRequest, client: AmazonCloudFormationAsyncClient): Future[UpdateStackResult] = {
    asFuture(client.updateStackAsync)(updateStackRequest)
  }

  def client(profile: String): AmazonCloudFormationAsyncClient = {
    new AmazonCloudFormationAsyncClient(new ProfileCredentialsProvider(profile))
  }

  private class AwsAsyncPromiseHandler[R <: AmazonWebServiceRequest, T](promise: Promise[T]) extends AsyncHandler[R, T] {
    def onError(e: Exception) = {
      logger.error("AWS call failed", e)
      promise failure e
    }
    def onSuccess(r: R, t: T) = promise success t
  }

  private def asFuture[R <: AmazonWebServiceRequest, T]
  (awsClientMethod: Function2[R, AsyncHandler[R, T], java.util.concurrent.Future[T]])
  : Function1[R, Future[T]] = { awsRequest =>

    val p = Promise[T]()
    awsClientMethod(awsRequest, new AwsAsyncPromiseHandler(p))
    p.future
  }
}
