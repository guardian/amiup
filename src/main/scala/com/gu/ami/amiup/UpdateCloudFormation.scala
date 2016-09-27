package com.gu.ami.amiup

import com.amazonaws.services.cloudformation.AmazonCloudFormationAsyncClient
import com.amazonaws.services.cloudformation.model.ListStacksRequest
import collection.JavaConverters._


object UpdateCloudFormation {
  val allowedStatuses = List("CREATE_COMPLETE", "ROLLBACK_COMPLETE", "UPDATE_COMPLETE", "UPDATE_ROLLBACK_COMPLETE")

  def findStacks(sourceAmi: String, parameterName: String): List[String] = {
    ???
  }

  def updateStacks(stacks: String, newAmi: String)(implicit client: AmazonCloudFormationAsyncClient) = {
    ???
  }

  private val listStacksRequest = {
    new ListStacksRequest()
        .withStackStatusFilters(allowedStatuses.asJava)
  }
}
