# ReposeInstanceSpec.rb

require '../includes/valveCluster.rb'

include ValveCluster

describe "Building Valve Clusters" do
  describe "#buildHostCsvString" do
     it "should return instance info as a formatted CSV line" do
        clusterNodeLine = "Instance ID: 20253736\nHost ID: c5af5b461c884c770d9cb9784e2f6d7f\nName: testList21\nFlavor: 512 server\nImage: Red Hat Enterprise Linux 5.5\nMetadata: \nPublic DNS Name: 50-57-38-36.static.cloud-ips.com\nPublic IP Address: 50.57.38.36\nPrivate IP Address: 10.182.37.224\nPassword: testList21k5qTmV7D7\nEnvironment: _default\nRun List: recipe[java], recipe[tomcat7], recipe[powerapiroot]"
        
        info = Array.new()

        clusterNodeLine.split(/\n/).each do |line| 
           info.push(line.split(/:\s?/)[1])
        end
        
        csvLine = buildHostCsvString([info])
        
        csvLine.should eq("50.57.38.36,8887,Rackspace Auth v1.1\n50.57.38.36,8888,OpenStack Identity\n")
     end
  end
end

