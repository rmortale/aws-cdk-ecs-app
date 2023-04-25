package com.myorg;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.CfnSecurityGroup;
import software.amazon.awscdk.services.ec2.CfnSecurityGroupIngress;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.iam.*;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;

import java.util.Arrays;
import java.util.Map;

import static com.myorg.Utils.getContextVar;
import static com.myorg.Utils.makeEnv;
import static java.util.Collections.singletonList;

public class ServiceApp {

    private static final String awslogsDateTimeFormat = "%Y-%m-%dT%H:%M:%S.%f%z";

    public static void main(String[] args) {
        App app = new App();

        String environmentName = getContextVar(app, "environmentName");
        String accountId = getContextVar(app, "accountId");
        String region = getContextVar(app, "region");
        String applicationName = getContextVar(app, "applicationName");
        //String springProfile = getContextVar(app,"springProfile");
        String dockerImageUrl = getContextVar(app, "dockerImageUrl");

        String prefix = environmentName + "-" + applicationName;

        Environment awsEnvironment = makeEnv(accountId, region);

        Stack serviceStack = new Stack(app, prefix + "-service-stack", StackProps.builder()
                .stackName(prefix + "-service")
                .env(awsEnvironment)
                .build());

        LogGroup logGroup = LogGroup.Builder.create(serviceStack, "ecsLogGroup")
                .logGroupName(prefix + "-logs")
                .retention(RetentionDays.ONE_WEEK)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        Role ecsTaskExecutionRole = Role.Builder.create(serviceStack, "ecsTaskExecutionRole")
                .assumedBy(ServicePrincipal.Builder.create("ecs-tasks.amazonaws.com").build())
                .path("/")
                .inlinePolicies(Map.of(
                        prefix + "-ecsTaskExecutionRolePolicy",
                        PolicyDocument.Builder.create()
                                .statements(singletonList(PolicyStatement.Builder.create()
                                        .effect(Effect.ALLOW)
                                        .resources(singletonList("*"))
                                        .actions(Arrays.asList(
                                                "ecr:GetAuthorizationToken",
                                                "ecr:BatchCheckLayerAvailability",
                                                "ecr:GetDownloadUrlForLayer",
                                                "ecr:BatchGetImage",
                                                "logs:CreateLogStream",
                                                "logs:PutLogEvents"))
                                        .build()))
                                .build()))
                .build();

        Role ecsTaskRole = Role.Builder.create(serviceStack, "ecsTaskRole")
                .assumedBy(ServicePrincipal.Builder.create("ecs-tasks.amazonaws.com").build())
                .path("/").build();

        CfnTaskDefinition.ContainerDefinitionProperty container = CfnTaskDefinition.ContainerDefinitionProperty.builder()
                .name(prefix + "-container")
                .image(dockerImageUrl)
                .logConfiguration(CfnTaskDefinition.LogConfigurationProperty.builder()
                        .logDriver("awslogs")
                        .options(Map.of(
                                "awslogs-group", logGroup.getLogGroupName(),
                                "awslogs-region", awsEnvironment.getRegion(),
                                "awslogs-stream-prefix", prefix + "-stream",
                                "awslogs-datetime-format", awslogsDateTimeFormat))
                        .build())
                .portMappings(singletonList(CfnTaskDefinition.PortMappingProperty.builder()
                        .containerPort(8080)
                        .build()))
                //.environment(toKeyValuePairs(serviceInputParameters.environmentVariables))
                .stopTimeout(2)
                .build();

//        CfnTaskDefinition taskDefinition = CfnTaskDefinition.Builder.create(serviceStack, "taskDefinition")
//                // skipped family
//                .cpu(String.valueOf(256))
//                .memory(String.valueOf(512))
//                .networkMode("awsvpc")
//                .requiresCompatibilities(singletonList("FARGATE"))
//                .executionRoleArn(ecsTaskExecutionRole.getRoleArn())
//                .taskRoleArn(ecsTaskRole.getRoleArn())
//                .containerDefinitions(singletonList(container))
//                .build();

        FargateTaskDefinition taskDefinition = FargateTaskDefinition.Builder.create(serviceStack, "taskDefinition")
                .memoryLimitMiB(512)
                .cpu(256)
                .build();
        ContainerDefinition fgcontainer = taskDefinition.addContainer("WebContainer", ContainerDefinitionOptions.builder()
                // Use an image from DockerHub
                .image(ContainerImage.fromRegistry("amazon/amazon-ecs-sample"))
                .build());

//        FargateService service = FargateService.Builder.create(serviceStack, "Service")
//                .cluster(cluster)
//                .taskDefinition(taskDefinition)
//                .build();

        CfnSecurityGroup ecsSecurityGroup = CfnSecurityGroup.Builder.create(serviceStack, "ecsSecurityGroup")
                .vpcId(VpcEcsClusterApp.getVpcIdFromParameterStore(serviceStack, environmentName))
                .groupDescription("SecurityGroup for the ECS containers")
                .build();

        // allow the internet to access the containers
        CfnSecurityGroupIngress ecsIngressFromInternet = CfnSecurityGroupIngress.Builder.create(serviceStack, "ecsIngressFromInternet")
                .ipProtocol("-1")
                .cidrIp("0.0.0.0/0")
                .groupId(ecsSecurityGroup.getAttrGroupId())
                .build();

        // allow ECS containers to access each other
        CfnSecurityGroupIngress ecsIngressFromSelf = CfnSecurityGroupIngress.Builder.create(serviceStack, "ecsIngressFromSelf")
                .ipProtocol("-1")
                .sourceSecurityGroupId(ecsSecurityGroup.getAttrGroupId())
                .groupId(ecsSecurityGroup.getAttrGroupId())
                .build();

        CfnService service = CfnService.Builder.create(serviceStack, "ecsService")
                .cluster(VpcEcsClusterApp.getEcsClusterNameFromParameterStore(serviceStack, environmentName))
                .launchType("FARGATE")
                .deploymentConfiguration(CfnService.DeploymentConfigurationProperty.builder()
                        .maximumPercent(200)
                        .minimumHealthyPercent(50)
                        .build())
                .desiredCount(2)
                .taskDefinition(taskDefinition.getTaskDefinitionArn())

//                .loadBalancers(singletonList(CfnService.LoadBalancerProperty.builder()
//                        .containerName(containerName(applicationEnvironment))
//                        .containerPort(serviceInputParameters.containerPort)
//                        .targetGroupArn(targetGroup.getRef())
//                        .build()))
                .networkConfiguration(CfnService.NetworkConfigurationProperty.builder()
                        .awsvpcConfiguration(CfnService.AwsVpcConfigurationProperty.builder()
                                .assignPublicIp("ENABLED")
                                .securityGroups(singletonList(ecsSecurityGroup.getAttrGroupId()))
                                .subnets(VpcEcsClusterApp.getPublicSubnetsFromParameterStore(serviceStack, environmentName))
                                .build())
                        .build())
                .build();

        app.synth();
    }
}
