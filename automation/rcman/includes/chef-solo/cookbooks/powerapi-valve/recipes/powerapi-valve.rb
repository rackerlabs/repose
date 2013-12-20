directory "/usr/share/lib" do
  owner "root"
  group "root"
  action :create
end

cookbook_file "/etc/init.d/repose-regression" do
  source "repose-regression"
  mode 0744
end

script "getVersionAndBuildNumbers" do
  interpreter "ruby"

  latestUrl = "http://maven.research.rackspacecloud.com/content/repositories/snapshots/com/rackspace/repose/installation/deb/valve/repose-valve/maven-metadata.xml"
  latest = `wget -qO- '#{latestUrl}'`.split(/<\/?version>/).select { |v| v=~ /SNAPSHOT/ }.pop


  valveUrl = "http://maven.research.rackspacecloud.com/content/repositories/snapshots/com/rackspace/repose/installation/deb/valve/repose-valve/#{latest}"
  filterBundleUrl = "http://maven.research.rackspacecloud.com/content/repositories/snapshots/com/rackspace/repose/installation/deb/filters/repose-filter-bundle/#{latest}"
  eFilterBundleUrl = "http://maven.research.rackspacecloud.com/content/repositories/snapshots/com/rackspace/papi/components/extensions/extensions-filter-bundle/#{latest}"

  vTimeStamp= `wget -qO- "#{valveUrl}/maven-metadata.xml"`.split(/<\/?value>/)
  fTimeStamp= `wget -qO- "#{filterBundleUrl}/maven-metadata.xml"`.split(/<\/?value>/)
  eTimeStamp= `wget -qO- "#{eFilterBundleUrl}/maven-metadata.xml"`.split(/<\/?value>/)

  vValue= vTimeStamp[vTimeStamp.size-2]
  fValue= fTimeStamp[fTimeStamp.size-2]
  eValue= eTimeStamp[eTimeStamp.size-2]

  `wget #{valveUrl}/repose-valve-#{vValue}.deb -O /root/repose-valve.deb`
  `wget #{filterBundleUrl}/repose-filter-bundle-#{fValue}.deb -O /root/filterBundle.deb`
  `wget #{eFilterBundleUrl}/extensions-filter-bundle-#{eValue}.ear -O /root/extensions-filter-bundle.ear`

end

script "getJolokia" do
  interpreter "bash"
  code <<-EOH
    wget http://labs.consol.de/maven/repository/org/jolokia/jolokia-jvm/1.0.6/jolokia-jvm-1.0.6-agent.jar -O /root/jolokia-jvm.jar
  EOH
end


package "papiCore" do
  action :install
  source "/root/repose-valve.deb"
  provider Chef::Provider::Package::Dpkg
  options "--force-all"
end

package "filterBundle" do
  action :install
  source "/root/filterBundle.deb"
  provider Chef::Provider::Package::Dpkg
  options "--force-all"
end

script "placeExtensionsFb" do
  interpreter "bash"
  code <<-EOH
   mv /root/extensions-filter-bundle.ear /usr/share/repose/filters/
  EOH
end

directory "/var/powerapi" do
  mode 770
  owner "root"
  group "root"
end

for valveGroup in 1..3
  directory "/etc/repose/valveGroup#{valveGroup}" do
    mode 0775
    owner "root"
    group "root"
  end

  directory "/var/powerapi/logs/valveGroup#{valveGroup}" do
    mode 0775
    owner "root"
    group "root"
    recursive true
  end


  ["rate-limiting.cfg.xml", "content-normalization.cfg.xml"].each do |config|
    cookbook_file "/etc/repose/valveGroup#{valveGroup}/#{config}" do
      source config
      mode 0644
    end
  end

  via=""
  cbrl=""

  case valveGroup
    when 1

      ["versioning.cfg.xml", "system-model.cfg.xml","versioning-2.cfg.xml"].each do |config|
        template "/etc/repose/valveGroup1/#{config}" do
          source "valveGroup1/#{config}.erb"
          mode 0644
        end
      end
      #Client Auth for RS Cloud Auth 1.1 And Client Auth for OpenStack Identity

      ["client-auth-keystone.cfg.xml", "client-auth-v1.1.cfg.xml","dist-datastore.cfg.xml"].each do |config|
        cookbook_file "/etc/repose/valveGroup1/#{config}" do
          source "/valveGroup1/#{config}"
          mode 0644
        end


      end

      ["header-normalization.cfg.xml", "uri-normalization.cfg.xml","openstack-authorization.cfg.xml"].each do |config|
        cookbook_file "/etc/repose/valveGroup1/#{config}" do
          source "#{config}"
          mode 0644
        end
      end
      
      #add normalization files here.  loop through all of them and add to valveGroup1
      ["empty-uri-target-w-media-uri-normalization.xml","only-media-variant-uri-normalization.xml","no-http-methods-w-media-uri-normalization.xml","uri-normalization-w-media.xml","no-regex-w-media-uri-normalization.xml"].each do |config|
        cookbook_file "/etc/repose/valveGroup1/#{config}" do
          source "/valveGroup1/normalization/#{config}"
          mode 0644
        end
      end

      via="via=\"Repose (Cloud Integration)\""

    when 2
      #Client IP Identity Node
      ["uri-identity.cfg.xml", "content-normalization.cfg.xml", "response-messaging.cfg.xml", "ip-identity.cfg.xml",
       "header-identity.cfg.xml", "rate-limiting.cfg.xml", "rate-limiting-2.cfg.xml", "rate-limiting-3.cfg.xml" , "dist-datastore.cfg.xml",
       "responsefor5xx", "content-identity-auth-1-1.cfg.xml", "header-id-mapping.cfg.xml", "default.wadl",
       "group1.wadl", "group2.wadl", "test.xsd", "validator.cfg.xml", "keystone-auth.cfg.xml","client-auth-keystone-no-groups.cfg.xml","client-auth-v1.1-no-groups.cfg.xml"].each do |config|
        cookbook_file "/etc/repose/valveGroup2/#{config}" do
          source "/valveGroup2/#{config}"
          mode 0644
        end
      end

      template "/etc/repose/valveGroup2/system-model.cfg.xml" do
        source "valveGroup2/system-model.cfg.xml.erb"
        mode 0644
      end
      via=""

    when 3
      #Distirubted Datastore
      ["compression.cfg.xml","add-element.xsl", "identity.xsl","headers.xsl","queries.xsl", "headers-io.xsl", "remove-element.xsl",
          "translation.cfg.xml","translation-request-headers-query.cfg.xml","translation-response-headers-query.cfg.xml", 
          "translation-request.cfg.xml","translation-request-docfalse.cfg.xml","translation-response-docfalse.cfg.xml", 
          "translation-multi.cfg.xml","headers-a.xsl", "headers-b.xsl", "ip-identity.cfg.xml", "ip-identity2.cfg.xml", 
          "client-auth-n.cfg.xml", "dist-datastore.cfg.xml","translation-compression.cfg.xml","translation-json.cfg.xml","request-json.xsl","response-json.xsl"].each do |config|
        cookbook_file "/etc/repose/valveGroup3/#{config}" do
          source "/valveGroup3/#{config}"
          mode 0644
        end
      end

      template "/etc/repose/valveGroup3/system-model.cfg.xml" do
        source "valveGroup3/system-model.cfg.xml.erb"
        mode 0644
      end
      via=""
      cbrl="content-body-read-limit=\"768\""
  end

  ["container.cfg.xml"].each do |config|
    template "/etc/repose/valveGroup#{valveGroup}/#{config}" do
      source "#{config}.erb"
      mode 0644
      variables({
                    :via => via,
                    :cbrl => cbrl
                })
    end
  end
end

if node["powerapi-valve"]["image"] == "rhel"
  script "flushIptables" do
    interpreter "bash"
    code <<-EOH
         /sbin/iptables -P INPUT ACCEPT
         /sbin/iptables -P OUTPUT ACCEPT
         /sbin/iptables -F
    EOH
  end
end

