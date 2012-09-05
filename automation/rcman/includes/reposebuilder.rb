#!usr/local/bin/ruby

require File.dirname(__FILE__)+'/csbuilder'
require File.dirname(__FILE__)+'/logging'
require File.dirname(__FILE__)+'/reposefileutils'
include Clouds
include Logging
include ReposeFileUtils

require File.join(File.dirname(__FILE__), 'InstanceConstants.rb')
require File.join(File.dirname(__FILE__), 'ReposeInstance.rb')

require 'net/ssh'
require 'net/scp'
require 'net/http'
require 'etc'
require 'fileutils'

module ReposeBuilder

    def buildReposeBox1

        serverName = Time.new.strftime("rcman-%d%b%y-%H%M%S-#{Etc.getlogin}-1")
        puts "Building #{serverName}..."

        cs = loginCs
        server = buildServer(cs,serverName,112,3) 

        logger.info("Repose instance built: #{server.ip} : #{server.password} : #{server.getId}")

        waitForServer server

        p "done!"


        puts "Uploading install files..."
        uploadChefFiles(server)
        p "done!"

        #executeCommand(server,"tar -C /root -xzvf /root/chef-solo.tar") 

        puts "Installing ruby and chef..."
        executeCommand(server,"/bin/bash /root/chef-solo/install.sh")

        puts "Installing repose..."
        executeCommand(server,"/usr/bin/chef-solo -c /root/chef-solo/solo.rb -j /root/chef-solo/papi_node.json")

        executeCommand(server,"/usr/bin/chef-solo -c /root/chef-solo/solo.rb -j /root/chef-solo/papi_node.json")

        puts "Starting repose..."
        executeCommand(server,"/usr/sbin/service repose-regression start")
        executeCommand(server,"/usr/sbin/service tomcat7 restart")

        logger.info("Repose instance built: #{server.ip} : #{server.password} : #{server.getId}")

        hostsCsv = buildHostsFile1 server.ip
        File.open("/tmp/hosts.csv", 'w') { |f| f.write(hostsCsv) }
        File.open("/tmp/rsInstances",'w'){|f| f.write(server.getId)}
        puts "Repose Regression Box built:\t#{server.ip}"
        return server

    end

    def buildReposeBox2

        serverName = Time.new.strftime("rcman-%d%b%y-%H%M%S-#{Etc.getlogin}-2")
        puts "Building #{serverName}..."

        cs = loginCs
        server = buildServer(cs,serverName,112,2) 

        logger.info("Repose instance built: #{server.ip} : #{server.password} : #{server.getId}")

        waitForServer server

        p "done!"


        puts "Uploading install files..."
        uploadChefFiles(server)
        p "done!"

        #executeCommand(server,"tar -C /root -xzvf /root/chef-solo.tar") 

        puts "Installing ruby and chef..."
        executeCommand(server,"/bin/bash /root/chef-solo/install.sh")

        puts "Installing repose..."
        executeCommand(server,"/usr/bin/chef-solo -c /root/chef-solo/solo.rb -j /root/chef-solo/papi_node2.json")

        executeCommand(server,"/usr/bin/chef-solo -c /root/chef-solo/solo.rb -j /root/chef-solo/papi_node2.json")

        puts "Starting repose..."
        executeCommand(server,"/usr/sbin/service tomcat7 restart")

        logger.info("Repose instance built: #{server.ip} : #{server.password} : #{server.getId}")

        hostsCsv = buildHostsFile2 server.ip
        File.open("/tmp/hosts.csv", 'a') { |f| f.write(hostsCsv) }
        File.open("/tmp/rsInstances",'a'){|f| f.write(",#{server.getId}")}
        puts "Repose Regression Box 2 built:\t#{server.ip}"
        return server
    end

    def waitForRepose(node,portList)
        portList.each do |i|
            uri = URI("http://#{node}:#{i}/v1/usertest1")
            count=0
            begin
                if count>60
                    raise "An error occured while starting repose..."
                end
                sleep 3
                puts "Waiting for repose to start on port #{i}"
                resp = Net::HTTP.get_response(uri)
                code = resp.code
                body = resp.body
                count+=1
            end while(resp=="200"&&body.size<1)
        end
    end

    def deleteReposeBox
        begin
            cs = loginCs
            server = File.open("/tmp/rsInstances",'r').readlines[0].split(/,/)
            server.each do |s|
                s=s.strip
                p s
                begin
                    deleteServer(cs,s)
                rescue CloudServers::Exception::ItemNotFound
                    puts "Cloud Server #{s} not found"
                rescue => e
                    puts e
                end
            end
            FileUtils.rm("/tmp/rsInstances",:force=>true)
        rescue => e
            puts "Error reading /tmp/rsInstances:\t #{e}"
        end

    end
end
