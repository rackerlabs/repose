
#!/usr/local/bin/ruby

require 'rubygems'
require 'builder'

module ValveConfigBuilder
    def buildPVConfig(hosts,filterChain)
        output = String.new
        builder = Builder::XmlMarkup.new(:target=>output, :indent=>2)
        builder.tag!("power-proxy", "xmlns" => "http://docs.rackspacecloud.com/power-api/system-model/v1.0") do |p|
            hosts.each do |host|
                p.host("id"=>host[2], "hostname"=>host[2], "service-port"=>"8887") do |h|

                    h.filters do |f|
                        #f.filter("name"=>"dist-datastore")
                        filterChain.each do |fil|

                            f.filter("name"=>fil)
                        end
                    end
                end
                p.host("id"=>host[2], "hostname"=>host[2], "service-port"=>"8888") do |h|

                    h.filters do |f|
                        #f.filter("name"=>"dist-datastore")
                        filterChain.each do |fil|

                            f.filter("name"=>fil)
                        end
                    end
                end
                p.host("id"=>host[2], "hostname"=>host[2], "service-port"=>"8889") do |h|

                    h.filters do |f|
                        #f.filter("name"=>"dist-datastore")
                        filterChain.each do |fil|

                            f.filter("name"=>fil)
                        end
                    end
                end
                p.host("id"=>"service", "hostname"=>host[2],"service-port"=>"8080")
            end
        end
        return output
    end
end
