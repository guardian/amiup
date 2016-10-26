package com.gu.ami.amiup

import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.cloudformation.model.Stack


case class AMI(amiId: String)

case class Arguments(
  newAmi: String,
  profile: String,
  parameterName: String = "AMI",
  existingAmi: Option[String] = None,
  stackIds: Option[Seq[String]] = None,
  region: Region = Region.getRegion(Regions.EU_WEST_1)
)
object Arguments {
  def empty() = Arguments("", "")
}

case class StackProgress(
  stack: Stack,
  started: Boolean,
  finished: Boolean
)
