#!/usr/local/bin/ruby

require 'rubygems'
require 'builder'

module Functions

    def getChefRepo

        chefRepo = File.expand_path(File.dirname(__FILE__)).sub(/clusters/,'')
        return chefRepo
    end


    def buildMockNode 
        instanceId = rand(1000)+100000
        hostId = "c5af5b461c884c770d9cb9784e2f6d7f"
        name = "mock#{rand(10000)}"
        flavor = "512"
        image = "Red Hat Enterprise Linux 5.5"
        node = "Instance ID: 20253736\nHost ID: c5af5b461c884c770d9cb9784e2f6d7f\nName: testList21\nFlavor: 512 server\nImage: Red Hat Enterprise Linux 5.5\nMetadata: \nPublic DNS Name: 50-57-38-36.static.cloud-ips.com\nPublic IP Address: 50.57.38.36\nPrivate IP Address: 10.182.37.224\nPassword: testList21k5qTmV7D7\nEnvironment: _default\nRun List: recipe[java], recipe[tomcat7], recipe[powerapiroot]"
        return node
    end

    def buildMockResults(num=3)

        results = Array.new
        info = Array.new
        for i in 0..num
            result = self.buildMockNode
            result.each { |line| 
                info.push(line.split(/:\s?/)[1])
            }
            results.push(info)
        end
        return results
    end

    def updateCookbook

        chefRepo = self.getChefRepo

        coreUrl = "http://maven.research.rackspacecloud.com/content/repositories/snapshots/com/rackspace/papi/core/web-application/0.9.2-SNAPSHOT"
        filterBundleUrl = "http://maven.research.rackspacecloud.com/content/repositories/snapshots/com/rackspace/papi/components/filter-bundle/0.9.2-SNAPSHOT"

        cTimeStamp= `wget -qO- '#{coreUrl}/maven-metadata.xml'`.split(/<\/?value>/)
        fTimeStamp= `wget -qO- '#{filterBundleUrl}/maven-metadata.xml'`.split(/<\/?value>/)

        cValue= cTimeStamp[cTimeStamp.size-2]
        fValue= fTimeStamp[fTimeStamp.size-2]

        puts "Downloading new rpms..."
        `wget #{coreUrl}/web-application-#{cValue}-rpm.rpm -O #{chefRepo}/cookbooks/powerapiroot/files/default/papiCore.rpm`
        `wget #{filterBundleUrl}/filter-bundle-#{fValue}-rpm.rpm -O #{chefRepo}/cookbooks/powerapiroot/files/default/filterBundle.rpm`

        puts "Updating Cookbook on Chef server..."
        `knife cookbook upload powerapiroot -c #{chefRepo}/chef/knife.rb`
    end

    def getLatestArtifacts

        chefRepo = self.getChefRepo

        coreUrl = "http://maven.research.rackspacecloud.com/content/repositories/snapshots/com/rackspace/papi/core/web-application/0.9.2-SNAPSHOT"
        filterBundleUrl = "http://maven.research.rackspacecloud.com/content/repositories/snapshots/com/rackspace/papi/components/filter-bundle/0.9.2-SNAPSHOT"

        cTimeStamp= `wget -qO- '#{coreUrl}/maven-metadata.xml'`.split(/<\/?value>/)
        fTimeStamp= `wget -qO- '#{filterBundleUrl}/maven-metadata.xml'`.split(/<\/?value>/)

        cValue= cTimeStamp[cTimeStamp.size-2]
        fValue= fTimeStamp[fTimeStamp.size-2]

        puts "Downloading new rpms..."
        `wget #{coreUrl}/web-application-#{cValue}-rpm.rpm -O #{chefRepo}/cookbooks/powerapiroot/files/default/papiCore.rpm`
        `wget #{filterBundleUrl}/filter-bundle-#{fValue}-rpm.rpm -O #{chefRepo}/cookbooks/powerapiroot/files/default/filterBundle.rpm`

        puts "Updating Cookbook on Chef server..."
        `knife cookbook upload powerapiroot -c #{chefRepo}/chef/knife.rb`
    end

    def getLatestMockService
        latestUrl = "http://maven.research.rackspacecloud.com/content/repositories/snapshots/com/rackspace/papi/external/testing/test-service-mock/maven-metadata.xml"  
        latest = `wget -qO- '#{latestUrl}'`.split(/<\/?version>/).select{|v| v=~ /SNAPSHOT/}.pop
        coreUrl = "http://maven.research.rackspacecloud.com/content/repositories/snapshots/com/rackspace/papi/external/testing/test-service-mock/#{latest}"
        cTimeStamp= `wget -qO- '#{coreUrl}/maven-metadata.xml'`.split(/<\/?value>/)

        cValue= cTimeStamp[cTimeStamp.size-2]
        `wget #{coreUrl}/test-service-mock-#{cValue}.war -O #{File.expand_path("./")}/files/service.war`
    end
end
