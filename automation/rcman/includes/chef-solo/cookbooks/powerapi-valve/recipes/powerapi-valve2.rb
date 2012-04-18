

directory "/usr/share/lib" do
   owner "root"
   group "root"
   action :create
end

script "getVersionAndBuildNumbers" do
   interpreter "ruby"
   `rm -rf /var/lib/tomcat7/webapps/*`
   latestUrl = "http://maven.research.rackspacecloud.com/content/repositories/snapshots/com/rackspace/papi/core/valve/maven-metadata.xml"
   latest = `wget -qO- '#{latestUrl}'`.split(/<\/?version>/).select{|v| v=~ /SNAPSHOT/}.pop


   rootWarUrl = "http://maven.research.rackspacecloud.com/content/repositories/snapshots/com/rackspace/papi/core/web-application/#{latest}"
   mocksUrl = "http://maven.research.rackspacecloud.com/content/repositories/snapshots/com/rackspace/papi/external/testing/test-service-mock/#{latest}/"
   valveUrl = "http://maven.research.rackspacecloud.com/content/repositories/snapshots/com/rackspace/papi/core/valve/#{latest}"
   filterBundleUrl = "http://maven.research.rackspacecloud.com/content/repositories/snapshots/com/rackspace/papi/components/filter-bundle/#{latest}"

   rTimeStamp= `wget -qO- "#{rootWarUrl}/maven-metadata.xml"`.split(/<\/?value>/)
   mTimeStamp = `wget -qO- "#{mocksUrl}/maven-metadata.xml"`.split(/<\/?value>/)
   vTimeStamp= `wget -qO- "#{valveUrl}/maven-metadata.xml"`.split(/<\/?value>/)
   fTimeStamp= `wget -qO- "#{filterBundleUrl}/maven-metadata.xml"`.split(/<\/?value>/)

   rValue= rTimeStamp[rTimeStamp.size-2]
   mValue= mTimeStamp[mTimeStamp.size-2]
   vValue= vTimeStamp[vTimeStamp.size-2]
   fValue= fTimeStamp[fTimeStamp.size-2]

   `wget #{rootWarUrl}/web-application-#{rValue}.war -O /var/lib/tomcat7/webapps/ROOT.war`
   `wget #{mocksUrl}/test-service-mock-#{mValue}.war -O /var/lib/tomcat7/webapps/mocks.war`
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


template "/etc/repose/power-proxy.cfg.xml" do
 source "secondnode/rootwarnode/power-proxy.cfg.xml.erb"
 mode 0644
end

cookbook_file "/etc/repose/root-context-router.cfg.xml" do
   source "secondnode/rootwarnode/root-context-router.cfg.xml"
   mode 0744
end

cookbook_file "/etc/repose/container.cfg.xml" do
   source "secondnode/rootwarnode/container.cfg.xml"
   mode 0744
end

script "giveTomcatAccess" do
    interpreter "bash"
    code <<-EOH
        chown tomcat7 /etc/repose -R
        chown tomcat7 /var/repose -R
        chown tomcat7 /usr/share/repose -R
        chown tomcat7 /usr/share/lib -R
        chmod 0775 /etc/repose -R
        chmod 0775 /var/repose -R
        chmod 0775 /usr/share/repose -R
        chmod 0775 /usr/share/lib -R
    EOH
end

