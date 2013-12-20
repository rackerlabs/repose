#!usr/local/bin/ruby

require File.join(File.dirname(__FILE__), 'InstanceConstants.rb')
require File.join(File.dirname(__FILE__), 'ReposeInstance.rb')

module ReposeFileUtils

    def buildHostsFile1(host)
        csvString = ""
        REPOSE_INSTANCE1.each do |instanceType|
            reposeInstance = ReposeInstance.new(host, instanceType.port(), instanceType.type())
            csvString += "#{reposeInstance.to_s()}\n"
        end
        return csvString;
    end

    def buildHostsFile2(host)
        csvString = ""
        REPOSE_INSTANCE2.each do |instanceType|
            reposeInstance = ReposeInstance.new(host, instanceType.port(), instanceType.type())
            csvString += "#{reposeInstance.to_s()}\n"
        end
        return csvString;
    end

    def uploadChefFiles(server)
        Net::SCP.start( server.ip , "root", :password => server.password, :paranoid => false) do |scp|
          scp.upload!("#{File.expand_path(File.dirname(__FILE__))}/chef-solo", "/root/", :recursive => true)
        end
    end

    def writeToFile(file, data)
        File.open(file, 'a') { |f| f.write(data) }
    end

    def newFile(file)
        File.new "/tmp/#{file}","w"
    end

end
