#!/usr/local/bin/ruby
require 'fileutils'
require 'rubygems'
require 'net/ssh'
require 'net/scp'
require 'net/http'
module ValveCluster


    def buildValveServer(name)

        #        updateCookbook
        chefRepo = getChefRepo
        image = 112
        flavor = 4
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


    def buildValveCluster(filterChain,clusterSize,jenkins=false)

        baseName = Time.new.strftime("%d%b%y%M")
        cluster = Array.new

        for i in 0..clusterSize-1
            node = self.buildValveServer("#{baseName}#{i}")
            cluster.push(node)
        end

        #Build host and config files
        log("Building #{clusterSize} node cluster with filter chain: #{filterChain.inspect}",true)

        hosts = buildHostsFile(cluster)
        xml = buildPVConfig(cluster, filterChain)
        rsInstances ="" 
        hostsCsv ="" 
        nodeNames ="" 
        scriptDir = "#{File.expand_path(File.dirname(__FILE__))}/../"
        File.open("#{scriptDir}/files/power-proxy.cfg.xml", 'w'){|f| f.write(xml)}

        cluster.each do |node|
            rsInstances += "#{node[0]},"
            for i in 7..9
                hostsCsv += "\"#{node[7]}\",\"888#{i}\"\n"
            end
            nodeNames += "#{node[2]},"
            puts "Starting repose cluster..."
            Net::SSH.start( "#{node[7]}" , "root", :password => "#{node[9]}") do |ssh|
                ssh.exec! "service repose-valve start"
            end
            self.waitForRepose(node[7])
=begin
            Net::SCP.start( "#{node[7]}" , "root", :password => "#{node[9]}") do |scp|
                for i in 1..3
                    scp.upload!("#{scriptDir}/files/power-proxy.cfg.xml", "/etc/powerapi/node#{i}/power-proxy.cfg.xml")
                end
            end
=end
        end

        hostsCsv = hostsCsv.chop # list of server ips, formatted for our jmeter tests
        rsInstances = rsInstances.chop #comma seperated list of rackspace server ids
        nodeNames =nodeNames.chop #comman seperated list of server hostnames 
        File.open("#{scriptDir}/configs/rsInstances", 'w'){|f| f.write(rsInstances)}
        File.open("#{scriptDir}/configs/nodeNames", 'w'){|f| f.write(nodeNames)}
        File.open("#{scriptDir}/hosts.csv", 'w'){|f| f.write(hostsCsv)}
        if jenkins
            File.open("/tmp/hosts.csv", 'w'){|f| f.write(hostsCsv)}
            File.open("/tmp/nodeNames", 'w'){|f| f.write(nodeNames)}
            File.open("/tmp/rsInstances", 'w'){|f| f.write(rsInstances)}
        end
        return cluster
    end

    def waitForRepose(node)
        for i in 7..9
            uri = URI("http://#{node}:888#{i}/v1/usertest1")
            begin
                sleep 3
                puts "Waiting for repose to start on port 888#{i}"
                resp = Net::HTTP.get_response(uri).code
            end while resp=="200"
        end
    end


end
