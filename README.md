> **Note**
> This repository is now archieved in favour of [using Riff-Raff to update AMIs in CloudFormation templates](https://riffraff.gutools.co.uk/docs/magenta-lib/types#amicloudformationparameter).
> If you've got questions on how to do this, please contact the DevX Operations team.

AMIUP
=====

Amiup is a command-line tool for bulk-updating AMIs in CloudFormation
templates.

If you have CloudFormation stacks that get their AMI via a parameter
you can run `amiup` to update multiple stacks from a single command.

If you are in the Guardian, you might consider using
[amiable](https://amiable.gutools.co.uk/) for recommendations on which
ami you should upgrade to.

## Usage

In
[the releases section of this repo](https://github.com/guardian/amiup/releases)
is a jar that can be executed directly.

    java -jar amiup.jar --existing ami-123456 --new ami-567890 --profile <AWS profile> [--region <aws region name>]

Using the credentials you provide, `amiup` will first find all the
matching CloudFormation stacks. These will be stacks with an `AMI`
parameter (use the `--parameter` argument to choose a different
parameter name) that contains the `--existing` ami.

For each stack it finds, it will perform a CloudFormation update that
changes the AMI parameter's value to the ami given as the `--new`
argument. Once it has kicked off the CloudFormation update it will
poll stacks until the update completes, letting you know the current
status of each stack.

### YOLO Mode

The `yolo` mode of `amiup` will start an instance refresh on the
autoscaling group once the CloudFormation template has been updated.
Instances will be replaced with a rolling update; running instances
will be terminated before new ones launch.

Rolling updates can fail due to failed health checks or if instances
are on standby or are protected from scale in. If the update process
fails, any instances already replaced will not be rolled back to their
previous configuration.

To run `amiup` in `yolo` mode the ASG name, stack name and new AMI are
required arguments:

```bash
java -jar amiup.jar yolo --asg <asg name> --stack <stack name> --new ami-567890 --profile <AWS profile> [--region <aws region name>]
```

## Development / alternate usage

You can also check out the project and run it directly using
`sbt`. Open a command line from within the project and execute the
following:

    ./sbt "run <args>"
    
    This will fetch all the dependencies, compile the project and execute
    it the same way as running the jar directly would.
    
    ### Building
    
    You can build your own jar by running the `assembly` command.
    
    ./sbt assembly
    
    This will run the tests and then create a "fat jar" containing all the
    project's dependencies. You'll see the location the jar gets saved to
    near the bottom of that command's output.
    
    ...
    [info] Packaging <path to jar>/amiup.jar ...
    [info] Done packaging.
