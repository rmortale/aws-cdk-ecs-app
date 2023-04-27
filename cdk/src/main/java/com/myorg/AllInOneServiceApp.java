package com.myorg;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.IVpc;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ec2.VpcLookupOptions;
import software.amazon.awscdk.services.ecs.*;

import static com.myorg.Utils.getContextVar;
import static com.myorg.Utils.makeEnv;

public class AllInOneServiceApp {

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

        IVpc vpc = Vpc.fromLookup(serviceStack, "Vpc", VpcLookupOptions.builder()
                .isDefault(true)
                .build());

        Cluster cluster = Cluster.Builder.create(serviceStack, "FargateCluster")
                .clusterName(environmentName + "cluster")
                .vpc(vpc).build();

        FargateTaskDefinition taskDefinition = FargateTaskDefinition.Builder.create(serviceStack, "taskDefinition")
                .memoryLimitMiB(512)
                .cpu(256)
                .build();
        ContainerDefinition fgcontainer = taskDefinition.addContainer("WebContainer", ContainerDefinitionOptions.builder()
                // Use an image from DockerHub
                .image(ContainerImage.fromRegistry("amazon/amazon-ecs-sample"))
                .build());

        FargateService service = FargateService.Builder.create(serviceStack, "Service")
                .cluster(cluster)
                .assignPublicIp(true)
                .taskDefinition(taskDefinition)
                .build();






        app.synth();
    }
}
