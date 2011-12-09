#!/usr/local/bin/ruby
require 'fileutils'
module Cluster


    def buildServer(name)

        #        updateCookbook
        chefRepo = getChefRepo
        image = 51
        flavor = 2
        cmd = "knife rackspace server create -r 'recipe[java],recipe[tomcat7],recipe[powerapiroot]' --server-name #{name} --node-name #{name} --image #{image} --flavor #{flavor} --template-file #{chefRepo}/.chef/centos-default-template.erb -c #{chefRepo}/.chef/knife.rb"

        #puts cmd
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


    def buildCluster(filterChain,clusterSize,jenkins=false)

        baseName = Time.new.strftime("%d%b%y%M")
        cluster = Array.new

        for i in 0..clusterSize-1
            node = self.buildServer("#{baseName}#{i}")
            cluster.push(node)
        end

        #Build host and config files
        log("Building #{clusterSize} node cluster with filter chain: #{filterChain.inspect}",true)

        hosts = buildHostsFile(cluster)
        xml = buildPConfig(cluster, filterChain)
        rl = buildRConfig
        ca = buildCaConfig
        rsInstances ="" 
        hostsCsv ="" 
        nodeNames ="" 
        scriptDir = "#{File.expand_path(File.dirname(__FILE__))}/../"
        if !File.directory?("#{File.expand_path(scriptDir)}/files")
            Dir.mkdir("#{File.expand_path(scriptDir)}/files")
        end
        File.open("#{scriptDir}/files/hosts", 'w'){ |f| f.write(hosts)}
        File.open("#{scriptDir}/files/power-proxy.cfg.xml", 'w'){|f| f.write(xml)}
        File.open("#{scriptDir}/files/rate-limiting.cfg.xml",'w'){|f| f.write(rl)}
        File.open("#{scriptDir}/files/client-auth-n.cfg.xml",'w'){|f| f.write(ca)}

        puts "Copying hosts and config files to servers"

        cluster.each do |node|
            rsInstances += "#{node[0]},"
            hostsCsv += "\"#{node[7]}\"\n"
            nodeNames += "#{node[2]},"

            puts "Transfering files to #{node[2]}"

            Net::SCP.start( "#{node[7]}" , "root", :password => "#{node[9]}") do |scp|
                scp.upload!("#{scriptDir}/files/hosts", "/etc/hosts")
                scp.upload!("#{scriptDir}/configs/context.xml", "/var/lib/tomcat7/conf/")
                scp.upload!("#{scriptDir}/files/power-proxy.cfg.xml", "/etc/powerapi/")
                version = buildVConfig(node[2], node[7])
                File.open("#{scriptDir}/files/versioning.cfg.xml",'w'){ |f| f.write(version) }
                scp.upload!("#{scriptDir}/files/versioning.cfg.xml", "/etc/powerapi/")
                scp.upload!("#{scriptDir}/files/rate-limiting.cfg.xml", "/etc/powerapi/")
                scp.upload!("#{scriptDir}/files/client-auth-n.cfg.xml", "/etc/powerapi/")
            end
            Net::SSH.start( "#{node[7]}" , "root", :password => "#{node[9]}") do |ssh|
                ssh.exec! "/etc/init.d/tomcat7 start"
            end
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


    def destroyServer(instanceId, nodeName, chefRepo=getChefRepo)
        puts "Deleting #{nodeName}:#{instanceId}..."
        cmd1 = "knife rackspace server delete #{instanceId} -c #{chefRepo}/.chef/knife.rb -y"
        cmd2 = "knife node delete #{nodeName} -c #{chefRepo}/.chef/knife.rb -y"
        cmd3 = `knife client delete #{nodeName} -c #{chefRepo}/.chef/knife.rb -y`
        output1 = `#{cmd1}`
        output2 = `#{cmd2}`
        output3 = `#{cmd3}`
        puts output1,output2
    end


    def destroyCluster
        scriptDir = File.expand_path("#{File.dirname(__FILE__)}/..")
        puts scriptDir
        if !File.exists?("#{scriptDir}/configs/nodeNames") && !File.exists?("#{scriptDir}/configs/rsInstances")
            puts "No cluster present. Continuing..."
        else
            serverInstances = File.open("#{scriptDir}/configs/rsInstances",'r').readlines[0].split(/,/)
            nodeNames = File.open("#{scriptDir}/configs/nodeNames", 'r').readlines[0].split(/,/)

            #     serverInstances = File.open("/tmp/rsInstances",'r').readlines[0].split(/,/)
            #     nodeNames = File.open("/tmp/nodeNames", 'r').readlines[0].split(/,/)

            chefRepo = getChefRepo

            puts "Destroying cluster..."
            for i in 0..serverInstances.size-1
                #puts "knife node show #{nodeNames[i]} -c #{chefRepo}/chef/knife.rb"
                output = `knife node show #{nodeNames[i]} -c #{chefRepo}/.chef/knife.rb`
                if output.include?("ERROR")
                    puts "Node not present"
                else
                    self.destroyServer(serverInstances[i], nodeNames[i])
                end
            end
        end
            FileUtils.rm("#{scriptDir}/configs/rsInstances",:force=>true)
            FileUtils.rm("#{scriptDir}/configs/nodeNames",:force=>true)
            FileUtils.rm("#{scriptDir}/hosts.csv",:force=>true)
    end
end
