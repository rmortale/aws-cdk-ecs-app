package com.myorg;

import org.bukkit.event.Listener;
import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.elasticloadbalancing.LoadBalancer;
import software.amazon.awscdk.services.elasticloadbalancingv2.ListenerAction;
import software.amazon.awscdk.services.globalaccelerator.ListenerOptions;
import software.amazon.awscdk.services.globalaccelerator.PortRange;
import software.amazon.awscdk.services.iam.*;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;

import java.util.Arrays;
import java.util.Map;

import static com.myorg.Utils.getContextVar;
import static com.myorg.Utils.makeEnv;
import static java.util.Collections.singletonList;

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
