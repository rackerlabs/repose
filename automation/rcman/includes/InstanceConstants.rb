
class InstanceInfo
   def initialize(port, type)
      @port = port
      @type = type
   end
   
   def port()
      @port
   end
   
   def type()
      @type
   end
end

REPOSE_INST_AUTH_11 = InstanceInfo.new(8887, "Rackspace Auth v1.1")
REPOSE_INST_OS_IDENTITY = InstanceInfo.new(8888, "OpenStack Identity")
REPOSE_INST_CLIENT_IP = InstanceInfo.new(8889, "Client-Ip")

REPOSE_INSTANCES = [REPOSE_INST_AUTH_11, REPOSE_INST_OS_IDENTITY]
