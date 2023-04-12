package com.myorg;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ecr.LifecycleRule;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.iam.AccountPrincipal;

import java.util.Collections;

public class DockerRepositoryApp {

    public static void main(String[] args) {
        App app = new App();

        String accountId = (String) app.getNode().tryGetContext("accountId");
        Validations.requireNonEmpty(accountId, "context variable 'accountId' must not be null");

        String region = (String) app.getNode().tryGetContext("region");
        Validations.requireNonEmpty(region, "context variable 'region' must not be null");

        String applicationName = (String) app.getNode().tryGetContext("applicationName");
        Validations.requireNonEmpty(applicationName, "context variable 'applicationName' must not be null");

        Environment awsEnvironment = makeEnv(accountId, region);

        Stack dockerRepositoryStack = new Stack(app, "DockerRepositoryStack", StackProps.builder()
                .stackName(applicationName + "-DockerRepository")
                .env(awsEnvironment)
                .build());

        Repository ecrRepository = Repository.Builder.create(dockerRepositoryStack, "ecrRepository")
                .repositoryName(applicationName + "-repository".toLowerCase())
                .removalPolicy(RemovalPolicy.DESTROY)
                .lifecycleRules(Collections.singletonList(LifecycleRule.builder()
                        .rulePriority(1)
                        .description("limit to 10 images")
                        .maxImageCount(10)
                        .build()))
                .build();

        // grant pull and push to all users of the account
        ecrRepository.grantPullPush(new AccountPrincipal(accountId));
    }


    static Environment makeEnv(String account, String region) {
        return Environment.builder()
                .account(account)
                .region(region)
                .build();
    }
}
