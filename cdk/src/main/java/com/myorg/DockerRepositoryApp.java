package com.myorg;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ecr.LifecycleRule;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.iam.AccountPrincipal;

import java.util.Collections;

import static com.myorg.Utils.getContextVar;
import static com.myorg.Utils.makeEnv;

public class DockerRepositoryApp {

    public static void main(String[] args) {
        App app = new App();

        String accountId = getContextVar(app,"accountId");
        String region = getContextVar(app,"region");
        String applicationName = getContextVar(app,"applicationName");
        String stackName = applicationName + "-docker-repository";
        String stackId = stackName + "-stack";

        Environment awsEnvironment = makeEnv(accountId, region);

        Stack dockerRepositoryStack = new Stack(app, stackId, StackProps.builder()
                .stackName(stackName)
                .env(awsEnvironment)
                .build());

        Repository ecrRepository = Repository.Builder.create(dockerRepositoryStack, stackName)
                .repositoryName(stackName)
                .removalPolicy(RemovalPolicy.DESTROY)
                .lifecycleRules(Collections.singletonList(LifecycleRule.builder()
                        .rulePriority(1)
                        .description("limit to 10 images")
                        .maxImageCount(10)
                        .build()))
                .build();

        // grant pull and push to all users of the account
        ecrRepository.grantPullPush(new AccountPrincipal(accountId));
        app.synth();
    }



}
