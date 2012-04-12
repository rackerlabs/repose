
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
REPOSE_INST_DD_1 = InstanceInfo.new(8890, "Dist-Datastore")
REPOSE_INST_DD_2 = InstanceInfo.new(8891, "Dist-Datastore")
REPOSE_INST_DD_3 = InstanceInfo.new(8892, "Dist-Datastore")
REPOSE_INST_ROOT_WAR = InstanceInfo.new(8080, "RootWarNode")

REPOSE_INST_ROOT_CONTEXT = InstanceInfo.new(8080, "RootContextNode")

REPOSE_INSTANCE1 = [REPOSE_INST_AUTH_11, REPOSE_INST_OS_IDENTITY, REPOSE_INST_CLIENT_IP, REPOSE_INST_DD_1, REPOSE_INST_DD_2, REPOSE_INST_DD_3,REPOSE_INST_ROOT_WAR]
REPOSE_INSTANCE2 = [REPOSE_INST_ROOT_CONTEXT]
