# InstanceConstantsSpec.rb

require '../includes/InstanceConstants.rb'

describe InstanceInfo do
  describe "#REPOSE_INST_AUTH_11" do
     it "Rackspace Auth 1.1 should return expected values" do
        REPOSE_INST_AUTH_11.port().should eq(8887)
        REPOSE_INST_AUTH_11.type().should eq("Rackspace Auth v1.1")
     end
  end
  
  describe "#REPOSE_INST_OS_IDENTITY" do
     it "OpenStack Identity should return expected values" do
        REPOSE_INST_OS_IDENTITY.port().should eq(8888)
        REPOSE_INST_OS_IDENTITY.type().should eq("OpenStack Identity")
     end
  end
  describe "#REPOSE_INST_CLIENT_IP" do
     it "OpenStack Identity should return expected values" do
        REPOSE_INST_CLIENT_IP.port().should eq(8889)
        REPOSE_INST_CLIENT_IP.type().should eq("Client-Ip")
     end
  end
end

