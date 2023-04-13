package com.myorg;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;

import static com.myorg.Utils.makeEnv;

/**
 * This is an empty app that we can use for bootstrapping the CDK with the "cdk bootstrap" command.
 * We could do this with other apps, but this would require us to enter all the parameters
 * for that app, which is uncool.
 */
public class BootstrapApp {

    public static void main(final String[] args) {
        App app = new App();

        String region = (String) app.getNode().tryGetContext("region");
        Utils.requireNonEmpty(region, "context variable 'region' must not be null");

        String accountId = (String) app.getNode().tryGetContext("accountId");
        Utils.requireNonEmpty(accountId, "context variable 'accountId' must not be null");

        Environment awsEnvironment = makeEnv(accountId, region);

        Stack bootstrapStack = new Stack(app, "Bootstrap", StackProps.builder()
                .env(awsEnvironment)
                .build());

        app.synth();
    }

}
