package com.gu.ami.amiup

import com.amazonaws.regions.{Region, Regions}


case class AMI(amiId: String)

case class Arguments(newAmi: String, profile: String, parameterName: String = "AMI", existingAmi: Option[String] = None, stackIds: Option[Seq[String]] = None, region: Region = Region.getRegion(Regions.EU_WEST_1))
object Arguments {
  def empty() = Arguments("", "")
}
