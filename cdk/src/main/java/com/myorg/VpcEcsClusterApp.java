package com.myorg;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.IVpc;
import software.amazon.awscdk.services.ec2.SubnetConfiguration;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ssm.StringParameter;
import software.constructs.Construct;

import java.util.List;

import static com.myorg.Utils.*;
import static com.myorg.Utils.getContextVar;
import static java.util.Arrays.asList;

public class VpcEcsClusterApp {

    private static final String PARAMETER_VPC_ID = "vpcId";
    private static final String PARAMETER_ECS_CLUSTER_NAME = "ecsClusterName";
    private static final String PARAMETER_PUBLIC_SUBNET_ONE = "publicSubnetIdOne";
    private static final String PARAMETER_PUBLIC_SUBNET_TWO = "publicSubnetIdTwo";

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

        Cluster ecsCluster = Cluster.Builder.create(networkStack, "ecs")
                .vpc(vpc)
                .clusterName(environmentName + "-ecs-cluster")
                .build();

        Tags.of(networkStack).add("environment", environmentName);

        StringParameter vpcId = StringParameter.Builder.create(networkStack, "vpcId")
                .parameterName(createParameterName(environmentName, PARAMETER_VPC_ID))
                .stringValue(vpc.getVpcId())
                .build();

        StringParameter cluster = StringParameter.Builder.create(networkStack, "ecsClusterName")
                .parameterName(createParameterName(environmentName, PARAMETER_ECS_CLUSTER_NAME))
                .stringValue(ecsCluster.getClusterName())
                .build();

        StringParameter publicSubnetOne = StringParameter.Builder.create(networkStack, "publicSubnetOne")
                .parameterName(createParameterName(environmentName, PARAMETER_PUBLIC_SUBNET_ONE))
                .stringValue(vpc.getPublicSubnets().get(0).getSubnetId())
                .build();

        StringParameter publicSubnetTwo = StringParameter.Builder.create(networkStack, "publicSubnetTwo")
                .parameterName(createParameterName(environmentName, PARAMETER_PUBLIC_SUBNET_TWO))
                .stringValue(vpc.getPublicSubnets().get(1).getSubnetId())
                .build();


        app.synth();
    }

    private static String createParameterName(String environmentName, String parameterName) {
        return environmentName + "-Network-" + parameterName;
    }

    public static List<String> getPublicSubnetsFromParameterStore(Construct scope, String environmentName) {
        String subnetOneId = StringParameter.fromStringParameterName(scope, PARAMETER_PUBLIC_SUBNET_ONE, createParameterName(environmentName, PARAMETER_PUBLIC_SUBNET_ONE))
                .getStringValue();
        String subnetTwoId = StringParameter.fromStringParameterName(scope, PARAMETER_PUBLIC_SUBNET_TWO, createParameterName(environmentName, PARAMETER_PUBLIC_SUBNET_TWO))
                .getStringValue();
        return asList(subnetOneId, subnetTwoId);
    }

    public static String getEcsClusterNameFromParameterStore(Construct scope, String environmentName) {
        return StringParameter.fromStringParameterName(scope, PARAMETER_ECS_CLUSTER_NAME, createParameterName(environmentName, PARAMETER_ECS_CLUSTER_NAME))
                .getStringValue();
    }

    public static String getVpcIdFromParameterStore(Construct scope, String environmentName) {
        return StringParameter.fromStringParameterName(scope, PARAMETER_VPC_ID, createParameterName(environmentName, PARAMETER_VPC_ID))
                .getStringValue();
    }

}
