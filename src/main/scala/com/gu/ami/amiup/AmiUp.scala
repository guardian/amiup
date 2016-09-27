package com.gu.ami.amiup

import com.amazonaws.regions.{Region, Regions}
import scopt.OptionParser

object AmiUp {
  def main(args: Array[String]): Unit = {
    argParser.parse(args, Arguments.empty()) match {
      case parsedArgs @ Some(Arguments(newAmi, parameterName, sourceAmi, stacksOpt, profile, region)) =>
        println(parsedArgs)
      case None =>
        // parsing cmd line args failed, help message will be displayed
        System.exit(1)
    }
  }

  val argParser = new OptionParser[Arguments]("amiup") {
    arg[String]("<new ami id>")
      .action { (amiId, args) =>
        args.copy(newAmi = amiId)
      } text "The ID of the new AMI - stacks will be updated to use this value"
    opt[String]("existing").optional()
      .action { (amiId, args) =>
        args.copy(existingAmi = Some(amiId))
      } text "The existing AMI ID - stacks with this value in their AMI parameter will be updated"
    opt[Seq[String]]("stacks").optional()
      .action { (stacks, args) =>
        args.copy(stackIds = Some(stacks))
      } text "You can optionally specify CloudFormation stacks by ID"
    opt[String]('p', "parameter").optional()
      .action { (parameterName, args) =>
        args.copy(parameterName = parameterName)
      } text "The CloudFormation parameter name that should be updated (defaults to AMI)"
    opt[String]("profile").required()
      .validate { profileName =>
        if (profileName.isEmpty) failure("You must provide an AWS profile for the cloudformation operation")
        else success
      } action { (profileName, args) =>
        args.copy(profile = profileName)
      } text "Specify the AWS profile to use for authenticating CloudFormation changes"
    opt[String]("region").optional()
      .validate { region =>
        try {
          Region.getRegion(Regions.fromName(region))
          success
        } catch {
          case e: IllegalArgumentException =>
            failure(s"Invalid AWS region name, $region")
        }
      } action { (region, args) =>
        args.copy(region = Region.getRegion(Regions.fromName(region)))
      } text "AWS region name (defaults to eu-west-1)"
    checkConfig { args =>
      (args.existingAmi.isEmpty, args.stackIds.isEmpty) match {
        case (true, true) =>
          failure("You must provide stacks to update, or an existing AMI to search for")
        case (false, false) =>
          failure("You must provide either stacks to update, or an existing AMI to search for (not both)")
        case _ =>
          success
      }
    }
  }
}
