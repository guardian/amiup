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
