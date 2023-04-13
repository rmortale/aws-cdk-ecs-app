package com.myorg;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.IVpc;
import software.amazon.awscdk.services.ec2.SubnetConfiguration;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecs.Cluster;

import static com.myorg.Utils.*;
import static com.myorg.Utils.getContextVar;
import static java.util.Arrays.asList;

public class VpcEcsClusterApp {

    public static void main(String[] args) {
        App app = new App();

        String environmentName = getContextVar(app,"environmentName");
        String accountId = getContextVar(app,"accountId");
        String region = getContextVar(app,"region");
        String stackName = environmentName + "-network";
        String stackId = stackName + "-stack";

        Environment awsEnvironment = makeEnv(accountId, region);

        Stack networkStack = new Stack(app, stackId, StackProps.builder()
                .stackName(stackName)
                .env(awsEnvironment)
                .build());

        SubnetConfiguration publicSubnets = SubnetConfiguration.builder()
                .subnetType(SubnetType.PUBLIC)
                .name(environmentName + "-public-subnet")
                .build();

        Vpc vpc = Vpc.Builder.create(networkStack, "vpc")
                .natGateways(0)
                .maxAzs(2)
                .subnetConfiguration(asList(publicSubnets))
                .build();

        Cluster.Builder.create(networkStack, "ecs")
                .vpc(vpc)
                .clusterName(environmentName + "-ecs-cluster")
                .build();

        Tags.of(networkStack).add("environment", environmentName);

        app.synth();
    }
}
