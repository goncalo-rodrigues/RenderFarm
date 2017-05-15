package ist.cnv.worker;


import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;

public class AWSWorkerFactory {
    public static final String IMAGEID = "ami-42128022"; //TODO replace this with our image name
    public static final String INSTANCETYPE = "t2.micro";
    public static final String SECURITYGROUP = "default"; //TODO replace this , this was in sdk sample code


    private AmazonEC2 ec2 ;
    private RunInstancesRequest runInstanceRequest = null;

    public AWSWorkerFactory(){
        AWSCredentials credentials;
        try {
            credentials = new ProfileCredentialsProvider().getCredentials();
        } catch (Exception e) {
            System.out.println("Failed to load Credentials");
            throw new AmazonClientException(
                    "Cannot load the credentials from the credential profiles file. " +
                            "Please make sure that your credentials file is at the correct " +
                            "location (~/.aws/credentials), and is in valid format.",
                    e);

        }
        ec2 = new AmazonEC2Client(credentials);
        Region region = Region.getRegion(Regions.US_WEST_2);
        ec2.setRegion(region);

        //we create 1 request that will e always the same
        runInstanceRequest = new RunInstancesRequest();
        runInstanceRequest.withImageId(IMAGEID)
                .withInstanceType(INSTANCETYPE)
                .withSecurityGroups(SECURITYGROUP)
                .withMaxCount(1)
                .withMinCount(1);
    }

    public Worker createWorker(){
        if (ec2==null){
            System.out.println("ec3 == null");
        }
        RunInstancesResult  result = ec2.runInstances(runInstanceRequest);
        String id = result.getReservation().getInstances().get(0).getInstanceId();
        DescribeInstancesRequest describeInstanceRequest = new DescribeInstancesRequest().withInstanceIds(id);
        DescribeInstancesResult describeInstanceResult = ec2.describeInstances(describeInstanceRequest);
        String address = describeInstanceResult.getReservations().get(0).getInstances().get(0).getPublicDnsName();//TODO check if get ip is best
        return new Worker(id,address);
    }

    public void terminateWorker(Worker worker){
        String instanceID = worker.getId();
        TerminateInstancesRequest terminateRequest = new TerminateInstancesRequest();
        terminateRequest.withInstanceIds(instanceID);
        ec2.terminateInstances(terminateRequest);
    }

    public boolean isWorkerReady(Worker worker){
        String instanceID = worker.getId();
        return true;//FIXME change to server request
    }

    public static void main(String[] args){
        AWSWorkerFactory factory = new AWSWorkerFactory();
        Worker worker1 = factory.createWorker();
        System.out.println("CREATED  instanceid="+worker1.getId()+" address:"+worker1.getAddress());
        try {
            Thread.sleep(80000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        factory.terminateWorker(worker1);
        System.out.println("finisherd");
    }

}
