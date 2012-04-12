#!usr/local/bin/ruby

class ReposeNode

   def initialize(server)
       @server = server
   end

   def status
       @server.refresh
       return @server.status
   end

   def ip
       return @server.addresses.fetch(:public)[0] 
   end
   
   def name
       return @server.name
   end

   def password
       return @server.adminPass
   end

   def delete
       @server.delete!
   end

   def getServer
       return @server
   end

   def getId
       return @server.id
   end
end
