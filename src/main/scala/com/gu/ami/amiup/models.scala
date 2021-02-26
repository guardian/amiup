package com.gu.ami.amiup

import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.autoscaling.model.InstanceRefreshStatus
import software.amazon.awssdk.services.cloudformation.model.Stack


case class AMI(amiId: String)

sealed trait AmiupMode
case object Yolo extends AmiupMode
case object Safe extends AmiupMode

case class Arguments(
  mode: AmiupMode,
  newAmi: String,
  profile: String,
  parameterName: String = "AMI",
  existingAmi: Option[String] = None,
  stackIds: Option[Seq[String]] = None,
  stackName: Option[String] = None,
  asgName: Option[String] = None,
  region: Region = Region.EU_WEST_1
)
object Arguments {
  def empty(): Arguments = Arguments(Safe, "", "")
}

case class StackProgress(
  stack: Stack,
  started: Boolean,
  finished: Boolean,
  failed: Boolean
)

case class InstanceRefreshProgress(
  asgName: String,
  started: Boolean,
  failed: Boolean,
  finished: Boolean,
  status: Option[InstanceRefreshStatus],
  statusReason: Option[String],
)
