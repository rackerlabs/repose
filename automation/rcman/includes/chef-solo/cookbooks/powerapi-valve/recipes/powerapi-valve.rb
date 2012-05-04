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

   latestUrl = "http://maven.research.rackspacecloud.com/content/repositories/snapshots/com/rackspace/papi/core/valve/maven-metadata.xml"
   latest = `wget -qO- '#{latestUrl}'`.split(/<\/?latest>/).select{|v| v=~ /SNAPSHOT/}.shift


   valveUrl = "http://maven.research.rackspacecloud.com/content/repositories/snapshots/com/rackspace/papi/core/valve/#{latest}"
   filterBundleUrl = "http://maven.research.rackspacecloud.com/content/repositories/snapshots/com/rackspace/papi/components/filter-bundle/#{latest}"

   vTimeStamp= `wget -qO- "#{valveUrl}/maven-metadata.xml"`.split(/<\/?value>/)
   fTimeStamp= `wget -qO- "#{filterBundleUrl}/maven-metadata.xml"`.split(/<\/?value>/)

   vValue= vTimeStamp[vTimeStamp.size-2]
   fValue= fTimeStamp[fTimeStamp.size-2]

   `wget #{valveUrl}/valve-#{vValue}.deb -O /root/repose-valve.deb`
   `wget #{filterBundleUrl}/filter-bundle-#{fValue}.deb -O /root/filterBundle.deb`
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

      template "/etc/repose/node#{nodeNumber}/power-proxy.cfg.xml" do
         source "power-proxy.cfg.xml.erb"
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

      template "/etc/repose/node#{nodeNumber}/power-proxy.cfg.xml" do
         source "power-proxy.cfg.xml.erb"
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

      cookbook_file "/etc/repose/node1/uri-normalization.cfg.xml" do
         source "/uri-normalization.cfg.xml"
         mode 0644
      end

   when 3
      #Client IP Identity Node
      ["uri-identity.cfg.xml", "content-normalization.cfg.xml", "response-messaging.cfg.xml", "ip-identity.cfg.xml", "header-identity.cfg.xml", "rate-limiting.cfg.xml", "dist-datastore.cfg.xml", "responsefor5xx", "content-identity-auth.cfg.xml", "header-id-mapping.cfg.xml"].each do |config|
         cookbook_file "/etc/repose/node3/#{config}" do
            source "/client-ip/#{config}"
            mode 0644
         end
      end

      template "/etc/repose/node3/power-proxy.cfg.xml" do
         source "client-ip/power-proxy.cfg.xml.erb"
         mode 0644
      end

   when 4..6
      #Distirubted Datastore
      template "/etc/repose/node#{nodeNumber}/power-proxy.cfg.xml" do
         source "dist-datastore/power-proxy.cfg.xml.erb"
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

