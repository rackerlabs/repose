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
        server = buildServer(cs,serverName,104,3) 
        logger.info("Repose instance built: #{server.ip} : #{server.password} : #{server.getId}")
        waitForServer server
        puts "Building #{serverName}...done!"

        puts "Uploading install files..."
        uploadChefFiles(server)
        puts "Uploading install files...done!"

        puts "Installing Repose..."
        installRepose(server,"papi_node")
        puts "Installing Repose...done!"

        puts "Starting repose..."
        valveNodes(server, "start")
        tomcatNodes(server,"restart")
        puts "Starting repose...done!"

        logger.info("Repose instance built: #{server.ip} : #{server.password} : #{server.getId}")

        hostsCsv = buildHostsFile1 server.ip

        writeToFile("/tmp/hosts.csv", hostsCsv)

        puts "Repose Regression Box built:\t#{server.ip}"
        return server

    end

    def buildReposeBox2

        serverName = Time.new.strftime("rcman-%d%b%y-%H%M%S-#{Etc.getlogin}-2")

        puts "Building #{serverName}..."
        cs = loginCs
        server = buildServer(cs,serverName,104,5) 
        logger.info("Repose instance built: #{server.ip} : #{server.password} : #{server.getId}")
        waitForServer server
        puts "Building #{serverName}...done!"

        puts "Uploading install files..."
        uploadChefFiles(server)
        puts "Uploading install files...done!"

        puts "Installing Repose..."
        installRepose(server,"papi_node2")
        puts "Installing Repose...done!"

        puts "Starting repose..."
        valveNodes2(server, "start")
        tomcatNodes(server,"restart")
        puts "Starting repose...done!"

        logger.info("Repose instance built: #{server.ip} : #{server.password} : #{server.getId}")

        hostsCsv = buildHostsFile2 server.ip
        writeToFile("/tmp/hosts.csv", hostsCsv)
        puts "Repose Regression Box 2 built:\t#{server.ip}"
        return server
    end

    def installRepose(server,cookbook)

        puts "Installing ruby and chef..."
        executeCommand(server,"/bin/bash /root/chef-solo/install.sh")
        puts "Installing ruby and chef...done"

        puts "Installing repose..."
        executeCommand(server,"/usr/bin/chef-solo -c /root/chef-solo/solo.rb -j /root/chef-solo/#{cookbook}.json")
        puts "Installing repose...done"

        puts "Installing repose again..."
        executeCommand(server,"/usr/bin/chef-solo -c /root/chef-solo/solo.rb -j /root/chef-solo/#{cookbook}.json")
        puts "Installing repose again...done"
    end

    def valveNodes(server,command)
        executeCommand(server,"/usr/sbin/service repose-regression #{command}")
    end

    def valveNodes2(server,command)
        executeCommand(server,"/usr/sbin/service repose-regression2 #{command}")
    end

    def tomcatNodes(server,command)
        executeCommand(server,"/usr/sbin/service tomcat7 #{command}")
    end

    def waitForRepose(node,portList)
        portList.each do |i|
            uri = URI("http://#{node}:#{i}/v1/usertest1")
            print "Waiting for repose to start on port #{i} (#{uri})"
            count=0
            begin
                if count>60
                    raise "An error occured while starting repose..."
                end
                sleep 5
                resp = Net::HTTP.get_response(uri)
                code = resp.code
                body = resp.body
                print "."
                count+=1
            end while(code=="500"&&body.include?("initialized"))
            puts
        end
    end

    def deleteReposeBox
        begin
            cs = loginCs
            server = File.open("/tmp/rsInstances",'r').readlines[0].split(/,/)
            server.each do |s|
                s=s.strip
                puts s
                begin
                    deleteServer(cs,s)
                rescue CloudServers::Exception::ItemNotFound
                    puts "Cloud Server #{s} not found"
                rescue => e
                    puts e
                end
            end
        rescue => e
            puts "Error reading /tmp/rsInstances:\t #{e}"
        ensure
            FileUtils.rm("/tmp/rsInstances",:force=>true)
        end

    end
end
