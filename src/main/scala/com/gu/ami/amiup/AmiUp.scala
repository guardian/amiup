package com.gu.ami.amiup

import cats.data.EitherT
import cats.instances.future._
import com.gu.ami.amiup.aws.{AWS, PollDescribeStackStatus, UpdateCloudFormation}
import com.gu.ami.amiup.util.RichFuture._
import scopt.OptionParser
import software.amazon.awssdk.regions.Region

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._


object AmiUp {
  def main(args: Array[String]): Unit = {
    argParser.parse(args, Arguments.empty()) match {
      case parsedArgs @ Some(Arguments(newAmi, profile, parameterName, existingAmiOpt, stacksOpt, region)) =>
        val client = AWS.client(profile, region)

        val result = for {
          // find stacks
          matchingStacks <- EitherT.right(
            UpdateCloudFormation.findStacks(existingAmiOpt.toLeft(stacksOpt.get), parameterName, client)
          )
          // validate stacks
          stacks <- EitherT.fromEither[Future](
            UpdateCloudFormation.validateStacks(parameterName, matchingStacks)
          )
          // get confirmation
          _ <- EitherT.fromEither[Future](
            UI.confirmStacks(stacks)
          )
          // make update stacks calls
          _ <- EitherT.right(UpdateCloudFormation.updateStacks(stacks, newAmi, parameterName, client))
          // watch the progress for all the stacks
          finished <- PollDescribeStackStatus.pollUntilComplete(stacks, client)(UI.displayProgress)
        } yield finished

        // give it 5 minutes to complete (CF is sometimes slow)
        result.value.awaitAsEither(5.minutes)(_.getMessage).joinRight match {
          case Right(_) =>
            UI.complete()
            System.exit(0)
          case Left(errMessage) =>
            UI.error(errMessage)
            System.exit(1)
        }

      case None =>
        // parsing cmd line args failed, help message will have been displayed
        System.exit(1)
    }
  }

  val argParser = new OptionParser[Arguments]("amiup") {
    opt[String]("new").required()
      .action { (amiId, args) =>
        args.copy(newAmi = amiId)
      } text "The ID of the new AMI - stacks will be updated to use this value"
    opt[String]("existing").optional()
      .action { (amiId, args) =>
        args.copy(existingAmi = Some(amiId))
      } text "The existing AMI ID - stacks with this value in their AMI parameter will be updated"
    opt[String]('p', "parameter").optional()
      .action { (parameterName, args) =>
        args.copy(parameterName = parameterName)
      } text "The CloudFormation parameter name that should be updated (defaults to AMI)"
    opt[Seq[String]]("stacks").optional()
      .action { (stacks, args) =>
        args.copy(stackIds = Some(stacks))
      } text "You can optionally specify CloudFormation stacks by ID"
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
          // TODO: either remove or fix the region validation as this no longer throws here,
          //  an invalid region now causes a UnknownHostException when the API calls are made
          Region.of(region)
          success
        } catch {
          case e: IllegalArgumentException =>
            failure(s"Invalid AWS region name, $region")
        }
      } action { (region, args) =>
        args.copy(region = Region.of(region))
      } text "AWS region name (defaults to eu-west-1)"
    help('h', "help").text("Prints this usage text")
    note(
      """
        |Update the AMI parameter of your cloudformation stacks.
        |
        |You will need to either provide an the ID of the AMI you wish to
        |replace, or a list of cloudformation stack identifiers to update.
        |In the first case, amiup will search all your stacks to find those
        |that use the deprecated AMI, and in the latter case it will perform
        |the update on the stack(s) you specify.
      """.stripMargin)
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
