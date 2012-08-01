if node["powerapi-valve"]["image"] == "deb"
   package "rpm" do
      action :install
      provider Chef::Provider::Package::Apt 
   end
end

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
    latest = `wget -qO- '#{latestUrl}'`.split(/<\/?version>/).select{|v| v=~ /SNAPSHOT/}.pop


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

for nodeNumber in 1..6
   directory "/etc/repose/node#{nodeNumber}" do
      mode 0775
      owner "root"
      group "root"
   end

   directory "/var/powerapi/logs/node#{nodeNumber}" do
      mode 0775
      owner "root"
      group "root"
      recursive true
   end

   ["container.cfg.xml"].each do |config|
      template "/etc/repose/node#{nodeNumber}/#{config}" do
         source "#{config}.erb"
         mode 0644
         variables(
            :port => 8886 + nodeNumber
         )
      end
   end

   ["rate-limiting.cfg.xml", "content-normalization.cfg.xml"].each do |config|
      cookbook_file "/etc/repose/node#{nodeNumber}/#{config}" do
         source config
         mode 0644
      end
   end

   case nodeNumber
   when 1
      #Client Auth for RS Cloud Auth 1.1
      template "/etc/repose/node#{nodeNumber}/versioning.cfg.xml" do
         source "versioning.cfg.xml.erb"
         mode 0644
      end

      template "/etc/repose/node#{nodeNumber}/system-model.cfg.xml" do
         source "system-model.cfg.xml.erb"
         mode 0644
      end

      cookbook_file "/etc/repose/node1/client-auth-n.cfg.xml" do
         source "/auth1.1/client-auth-n.cfg.xml"
         mode 0644
      end

      cookbook_file "/etc/repose/node1/header-normalization.cfg.xml" do
         source "/header-normalization.cfg.xml"
         mode 0644
      end

      cookbook_file "/etc/repose/node1/uri-normalization.cfg.xml" do
         source "/uri-normalization.cfg.xml"
         mode 0644
      end

   when 2
      #Client Auth for OpenStack Identity
      template "/etc/repose/node#{nodeNumber}/versioning.cfg.xml" do
         source "versioning.cfg.xml.erb"
         mode 0644
      end

      template "/etc/repose/node#{nodeNumber}/system-model.cfg.xml" do
         source "system-model.cfg.xml.erb"
         mode 0644
      end

     cookbook_file "/etc/repose/node2/client-auth-n.cfg.xml" do
         source "/keystone/client-auth-n.cfg.xml"
         mode 0644
      end

      cookbook_file "/etc/repose/node2/openstack-authorization.cfg.xml" do
         source "openstack-authorization.cfg.xml"
         mode 0644
      end

      cookbook_file "/etc/repose/node2/header-normalization.cfg.xml" do
         source "/header-normalization.cfg.xml"
         mode 0644
      end

      cookbook_file "/etc/repose/node2/uri-normalization.cfg.xml" do
         source "/uri-normalization.cfg.xml"
         mode 0644
      end

   when 3
      #Client IP Identity Node
      ["uri-identity.cfg.xml", "content-normalization.cfg.xml", "response-messaging.cfg.xml", "ip-identity.cfg.xml", "header-identity.cfg.xml", "rate-limiting.cfg.xml", "dist-datastore.cfg.xml", "responsefor5xx", "content-identity-auth-1-1.cfg.xml", "header-id-mapping.cfg.xml", "default.wadl", "group1.wadl", "group2.wadl", "test.xsd", "validator.cfg.xml"].each do |config|
         cookbook_file "/etc/repose/node3/#{config}" do
            source "/client-ip/#{config}"
            mode 0644
         end
      end

      template "/etc/repose/node3/system-model.cfg.xml" do
         source "client-ip/system-model.cfg.xml.erb"
         mode 0644
      end

   when 4..6
      #Distirubted Datastore
      ["ip-identity.cfg.xml","ip-identity2.cfg.xml","client-auth-n.cfg.xml"].each do |config|
         cookbook_file "/etc/repose/node#{nodeNumber}/#{config}" do
            source "/dist-datastore/#{config}"
            mode 0644
         end
      end

      template "/etc/repose/node#{nodeNumber}/system-model.cfg.xml" do
         source "dist-datastore/system-model.cfg.xml.erb"
         mode 0644
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

