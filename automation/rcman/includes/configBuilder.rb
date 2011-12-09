
#!/usr/local/bin/ruby

require 'rubygems'
require 'builder'

module ConfigBuilder 

    def genRCFilterChain(passedFilters)
        defaultFilterChain = ["dist-datastore","versioning","client-auth","rate-limiting"]
        #rcFilterChain = Array.new
        rcFilterChain = Array.new

        #since the number of nodes in this is static we will always need the dist-datastore when we have rate-limiting
        if passedFilters.include?("rate-limiting") && !passedFilters.include?("dist-datastore")
            passedFilters.push("dist-datastore")
        end

        #we are building a cluster using the root war deploy strategy which requires versioning to work
        if !passedFilters.include?("versioning")
            passedFilters.push("versioning")
        end

        defaultFilterChain.each{ |filter|
            if passedFilters.include?(filter)
                rcFilterChain.push(filter)
            end
        }

        return rcFilterChain
    end

    def buildPConfig(hosts,filterChain)
        output = String.new
        builder = Builder::XmlMarkup.new(:target=>output, :indent=>2)
        builder.tag!("power-proxy", "xmlns" => "http://docs.rackspacecloud.com/power-api/system-model/v1.0") do |p|
            hosts.each do |host|
                p.host("id"=>host[2], "hostname"=>host[2], "service-port"=>"8080") do |h|

                    h.filters do |f|
                        #f.filter("name"=>"dist-datastore")
                        filterChain.each do |fil|

                            f.filter("name"=>fil)
                        end
                    end
                end
            end
        end
        return output
    end

    def buildHostsFile(results)

        hosts = String.new
        hosts = "127.0.0.1     localhost localhost.localdomain\n50.57.98.109     chef-server-n01.cit   chef-server-n01.cit.rackspace.com\n"

        results.each do |node|

            hosts += "\n#{node[7]}\t#{node[2]}\n"
        end

        return hosts
    end

    def buildRConfig(value="3", includeAbs=true)

        output = String.new
        builder = Builder::XmlMarkup.new(:target=>output, :indent=>2)

        builder.tag!("rate-limiting", "xmlns"=>"http://docs.rackspacecloud.com/power-api/rate-limiting/v1.0") do |rl|
            rl.tag!("request-endpoint", "uri-regex"=>"/_service/resources/mockendservice/v\d/[^/]+/limits/?", "include-absolute-limits"=>includeAbs.to_s)
            rl.tag!("limit-group", "id"=>"admin-limits", "roles"=>"Admin", "default"=>"true") do |lg|
                lg.limit("uri"=>"/_service/resources/mockendservice/v*", "uri-regex"=>"/_service/resources/mockendservice/(v\d/[^/]+).*", "http-methods"=>"GET", "unit"=>"MINUTE", "value"=>value)
            end
            rl.tag!("limit-group", "id"=>"customer-limits", "roles"=>"customer") do |lg|
                lg.limit("uri"=>"/service/*", "uri-regex"=>"/service/([^/]*).*", "http-methods"=>"GET", "unit"=>"MINUTE", "value"=>value)
            end
            rl.tag!("limit-group", "id"=>"testing-limits", "roles"=>"test") do |lg|
                lg.limit("uri"=>"/service/*", "uri-regex"=>"/service/([^/]*).*", "http-methods"=>"GET", "unit"=>"MINUTE", "value"=>"10")
            end
        end

        return output

    end

    def buildVConfig(hostId, hostName, port="8080")
        output = String.new
        builder = Builder::XmlMarkup.new(:target=>output, :indent=>2)
        xtype1 = ["application/xml","application/vnd.vendor.service-v1+xml","application/vnd.vendor.service+xml; version=1", "application/v1+xml", "application/vnd.rackspace; x=v1; y=xml","application/vnd.rackspace; rnd=1; x=v1; rnd2=2; y=xml"]
        jtype1 = ["application/json","application/vnd.vendor.service-v1+json", "application/vnd.vendor.service+json; version=1", "application/v1+json", "application/vnd.rackspace; x=v1; y=json", "application/vnd.rackspace; rnd=1; x=v1; rnd2=2; y=json"]
        xtype2 = ["application/xml","application/vnd.vendor.service-v2+xml","application/vnd.vendor.service+xml; version=2", "application/v2+xml", "application/vnd.rackspace; x=v2; y=xml","application/vnd.rackspace; rnd=1; x=v2; rnd2=2; y=xml"]
        jtype2 = ["application/json","application/vnd.vendor.service-v2+json", "application/vnd.vendor.service+json; version=2", "application/v2+json", "application/vnd.rackspace; x=v2; y=json", "application/vnd.rackspace; rnd=1; x=v2; rnd2=2; y=json"]

        builder.tag!("versioning", "xmlns"=>"http://docs.rackspacecloud.com/power-api/versioning/v1.0") do |rl|
            rl.tag!("service-root", "href"=>"#{hostName}:#{port}/")

            rl.tag!("version-mapping", "id"=>"/v1", "pp-host-id"=>hostId, "context-path"=>"/_v1/resources/mockendservice/v1","status"=>"DEPRECATED") do |vm|
                vm.tag!("media-types") do |mt|
                    for i in 1..xtype1.size-1
                        mt.tag!("media-type", "base"=>xtype1[0], "type"=>xtype1[i])
                    end
                    for i in 1..jtype2.size-1
                        mt.tag!("media-type", "base"=>jtype1[0], "type"=>jtype1[i])
                    end
                end
            end
            rl.tag!("version-mapping", "id"=>"/v2", "pp-host-id"=>hostId,"context-path"=>"/_v2/resources/mockendservice/v2", "status"=>"CURRENT") do |vm|
                vm.tag!("media-types") do |mt|
                    for i in 1..xtype2.size-1
                        mt.tag!("media-type", "base"=>xtype2[0], "type"=>xtype2[i])
                    end

                    for i in 1..jtype2.size-1
                        mt.tag!("media-type", "base"=>jtype2[0], "type"=>jtype2[i])
                    end
                end
            end
        end
        return output
    end

    def buildCaConfig(delegatable=false)
        output = String.new
        builder = Builder::XmlMarkup.new(:target=>output, :indent=>2)

        builder.tag!("client-auth", "xmlns"=>"http://docs.rackspacecloud.com/power-api/client-auth/v1.0") do |ca|
            ca.tag!("rackspace-auth", "delegatable"=>delegatable.to_s,"keystone-active"=>"true", "xmlns"=>"http://docs.rackspacecloud.com/power-api/client-auth/rs-auth-1.1/v1.0") do |ra|
                ra.tag!("authentication-server", "username"=>"auth", "password"=>"auth123", "uri"=>"http://localhost:8080/_service/resources/v1.1")
                ra.tag!("account-mapping", "id-regex"=>"/_service/resources/mockendservice/v\\d/([\\w-]+)/?", "type"=>"CLOUD")
                ra.tag!("user-roles") do |ur|
                    ur.tag!("default") do |d|
                        d.tag!("roles") do |r|
                            r.tag!("role","sysadmin")
                            r.tag!("role","netadmin")
                            r.tag!("role","developer")
                        end
                    end
                    ur.tag!("user","name"=>"usertest1") do |u|
                        u.tag!("roles") do |r|
                            r.tag!("role","firstuser")
                        end
                    end
                    ur.tag!("user","name"=>"usertest2") do |u|
                        u.tag!("roles") do |r|
                            r.tag!("role","seconduser")
                        end
                    end
                    ur.tag!("user","name"=>"usertest3") do |u|
                        u.tag!("roles") do |r|
                            r.tag!("role","thirduser")
                        end
                    end
                    ur.tag!("user","name"=>"usertest4") do |u|
                        u.tag!("roles") do |r|
                            r.tag!("role","fourthuser")
                        end
                    end
                end
            end
        end

        return output
    end

    def updateCookbook

        chefRepo = getChefRepo

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

end
