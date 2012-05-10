#!usr/local/bin/ruby

require 'rubygems'
require 'cloudservers'
require 'yaml'
require File.dirname(__FILE__)+'/rcmanconf'
require File.dirname(__FILE__)+'/reposenode'
require File.dirname(__FILE__)+'/logging'
include Logging
include ConfigureRcman

module Clouds

    def loginCs
        config = getCredentialsConfig
        apiKey = config["apiKey"]
        username = config["user"]

        return CloudServers::Connection.new(:username => username, :api_key => apiKey)
    end

    def buildServer(cs,name,image=112,flavor=3)
        begin
            server = ReposeNode.new(cs.create_server(:name => name, :imageId => image, :flavorId => flavor))
        rescue => e
            puts "Error encountered while building server: #{e}"
        end

      return server     
    end

    def waitForServer(server)
        num = 0
        status = server.status
        p "Building repose server..."
        while status!="ACTIVE"
            if status == "ERROR" || num>60
                p "error occured with server"
                exit
            end
            num+=1

            sleep 5
            status = server.status
        end
    end

    def deleteServer(cs,serverId)
        server = cs.get_server(serverId)
        server.delete!
    end


    def executeCommand(server, command)

        Net::SSH.start( server.ip , "root", :password => server.password, :paranoid => false) do |ssh|
            channel = ssh.open_channel do |ch|
                ch.exec command do |ch, success|
                    raise "could not execute command" unless success
                  # "on_data" is called when the process writes something to stdout
                    ch.on_data do |c, data|
                        logger.debug(data)
                    end
                      # "on_extended_data" is called when the process writes something to stderr
                    ch.on_extended_data do |c, type, data|
                        logger.warn(data)
                    end
                    ch.on_close { puts "done!" }
                end
            end
          channel.wait
        end
    end
end
