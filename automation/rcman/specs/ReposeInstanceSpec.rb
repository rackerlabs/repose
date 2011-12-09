# ReposeInstanceSpec.rb

require '../includes/ReposeInstance.rb'

describe ReposeInstance do
  describe "#to_s" do
     it "should return instance info as a formatted CSV line" do
        instance = ReposeInstance.new("10.1.1.1", 8081, "type")
        
        instance.to_s().should eq("10.1.1.1,8081,type")
     end
  end
end

