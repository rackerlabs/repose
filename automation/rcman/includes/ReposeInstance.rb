class ReposeInstance
   def initialize(hostIp, port, type)
      @hostIp = hostIp
      @port = port
      @type = type
   end
   
   def hostIp()
      @hostIp
   end
   
   def port()
      @port
   end
   
   def type()
      @type
   end
   
   def to_s()
      "#{@hostIp},#{@port},#{@type}"
   end
end
