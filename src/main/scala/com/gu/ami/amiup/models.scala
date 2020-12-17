package com.gu.ami.amiup

import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cloudformation.model.Stack


case class AMI(amiId: String)

case class Arguments(
  newAmi: String,
  profile: String,
  parameterName: String = "AMI",
  existingAmi: Option[String] = None,
  stackIds: Option[Seq[String]] = None,
  region: Region = Region.EU_WEST_1
)
object Arguments {
  def empty() = Arguments("", "")
}

case class StackProgress(
  stack: Stack,
  started: Boolean,
  finished: Boolean,
  failed: Boolean
)
