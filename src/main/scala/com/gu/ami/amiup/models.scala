package com.gu.ami.amiup

import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cloudformation.model.Stack


case class AMI(amiId: String)

sealed trait AmiupMode
case object Yolo extends AmiupMode
case object Safe extends AmiupMode

case class Arguments(
  mode: AmiupMode,
  newAmi: String, // used for both
  profile: String, // think we need for both?
  parameterName: String = "AMI", // used for both
  existingAmi: Option[String] = None, // standard, but maybe discuss removing?
  stackIds: Option[Seq[String]] = None, // standard
  stackName: Option[String] = None, // yolo (can we merge with above?)
  asgName: Option[String] = None, // yolo
  region: Region = Region.EU_WEST_1  // used for both
)
object Arguments {
  def empty() = Arguments(Safe, "", "")
}

case class StackProgress(
  stack: Stack,
  started: Boolean,
  finished: Boolean,
  failed: Boolean
)
