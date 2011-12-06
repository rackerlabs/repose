#!/usr/local/bin/ruby

require 'fileutils'
require 'rubygems'
require 'net/ssh'
require 'net/scp'
require 'net/http'

require File.join(File.dirname(__FILE__), 'InstanceConstants.rb')
require File.join(File.dirname(__FILE__), 'ReposeInstance.rb')

module ValveCluster
   def buildValveServer(name)
        # updateCookbook
        chefRepo = getChefRepo
        image = 112
        flavor = 3
        cmd = "knife rackspace server create -r 'recipe[java],recipe[powerapi-valve]' --server-name #{name} --node-name #{name} --image #{image} --flavor #{flavor} --template-file #{chefRepo}/.chef/default-template.erb -c #{chefRepo}/.chef/knife.rb"

        puts cmd
        puts "Building node #{name}..."

        result = `#{cmd} | tail -n12`.split(/\n/)
        log(result.inspect)
        
        #puts result
        nodes = `knife node list -c #{chefRepo}/chef/knife.rb`.split(/\n/)
        output = `knife node show #{name} -c #{chefRepo}/.chef/knife.rb`
        
        if output.include?("ERROR")
            puts "Node was not built!"
            exit
        else
            info = Array.new
            result.each{|line| info.push(line.split(/:\s?/)[1])}
            return info
        end
    end

    def buildHostCsvString(cluster)
       csvString = ""
       
        cluster.each do |node|
           host = "#{node[7]}"
           
           REPOSE_INSTANCES.each do |instanceType|
              reposeInstance = ReposeInstance.new(host, instanceType.port(), instanceType.type())
              csvString += "#{reposeInstance.to_s()}\n"
           end
        end
        
        csvString
    end
    
    def buildValveCluster(filterChain,clusterSize,jenkins=false)
        baseName = Time.new.strftime("%d%b%y%M")
        cluster = Array.new
        clusterSize =1 #hard setting this to one for now.

        for i in 0..clusterSize-1
            node = self.buildValveServer("#{baseName}#{i}")
            cluster.push(node)
        end

        #Build host and config files
        log("Building #{clusterSize} node cluster with filter chain: #{filterChain.inspect}",true)

        hosts = buildHostsFile(cluster)
        xml = buildPVConfig(cluster, filterChain)
        rsInstances = "" 
        hostsCsv = buildHostCsvString(cluster)
        nodeNames = "" 
        scriptDir = "#{File.expand_path(File.dirname(__FILE__))}/../"

        cluster.each do |node|
            host = "#{node[7]}"
            rsInstances += "#{node[0]},"
            nodeNames += "#{node[2]},"
            
            puts "Starting repose cluster..."
            
            Net::SSH.start( "#{host}" , "root", :password => "#{node[9]}") do |ssh|
                ssh.exec! "service repose-valve start"
            end
            
            self.waitForRepose(node[7])
        end

        hostsCsv = hostsCsv.chop # list of server ips, formatted for our jmeter tests
        rsInstances = rsInstances.chop #comma seperated list of rackspace server ids
        nodeNames =nodeNames.chop #comman seperated list of server hostnames 
        
        File.open("#{scriptDir}/configs/rsInstances", 'w') { |f| f.write(rsInstances) }
        File.open("#{scriptDir}/configs/nodeNames", 'w') { |f| f.write(nodeNames) }
        File.open("#{scriptDir}/hosts.csv", 'w') { |f| f.write(hostsCsv) }

        if jenkins
            File.open("/tmp/hosts.csv", 'w') { |f| f.write(hostsCsv) }
            File.open("/tmp/nodeNames", 'w') { |f| f.write(nodeNames) }
            File.open("/tmp/rsInstances", 'w') { |f| f.write(rsInstances) }
        end
        return cluster
    end

    def waitForRepose(node)
        for i in 7..8
            uri = URI("http://#{node}:888#{i}/v1/usertest1")
            begin
                sleep 3
                puts "Waiting for repose to start on port 888#{i}"
                resp = Net::HTTP.get_response(uri).code
            end while resp=="200"
        end
    end
end
