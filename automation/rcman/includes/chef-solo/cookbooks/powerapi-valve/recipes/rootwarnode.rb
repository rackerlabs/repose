
directory "/var/powerapi/logs/node7"  do
  owner "tomcat7"
  group "tomcat7"
  mode "0770"
  action :create
end

script "getVersionAndBuildNumbers" do
   interpreter "ruby"
   `rm -rf /var/lib/tomcat7/webapps/*`
   latestUrl = "http://maven.research.rackspacecloud.com/content/repositories/snapshots/com/rackspace/papi/core/valve/maven-metadata.xml"
   latest = `wget -qO- '#{latestUrl}'`.split(/<\/?version>/).select{|v| v=~ /1.+SNAPSHOT/}.pop


   rootWarUrl = "http://maven.research.rackspacecloud.com/content/repositories/snapshots/com/rackspace/papi/core/web-application/#{latest}"
   mocksUrl = "http://maven.research.rackspacecloud.com/content/repositories/snapshots/com/rackspace/papi/external/testing/test-service-mock/#{latest}/"

   rTimeStamp= `wget -qO- "#{rootWarUrl}/maven-metadata.xml"`.split(/<\/?value>/)
   mTimeStamp = `wget -qO- "#{mocksUrl}/maven-metadata.xml"`.split(/<\/?value>/)

   rValue= rTimeStamp[rTimeStamp.size-2]
   mValue= mTimeStamp[mTimeStamp.size-2]

   `wget #{rootWarUrl}/web-application-#{rValue}.war -O /var/lib/tomcat7/webapps/ROOT.war`
   `wget #{mocksUrl}/test-service-mock-#{mValue}.war -O /var/lib/tomcat7/webapps/mocks.war`
end


template "/etc/repose/power-proxy.cfg.xml" do
 source "rootwarnode/power-proxy.cfg.xml.erb"
 mode 0644
end

template "/etc/repose/versioning.cfg.xml" do
 source "rootwarnode/versioning.cfg.xml.erb"
 mode 0644
end

cookbook_file "/etc/repose/container.cfg.xml" do
   source "rootwarnode/container.cfg.xml"
   mode 0744
end

cookbook_file "/etc/repose/client-auth-n.cfg.xml" do
   source "rootwarnode/client-auth-n.cfg.xml"
   mode 0744
end

cookbook_file "/etc/repose/ip-identity.cfg.xml" do
   source "rootwarnode/ip-identity.cfg.xml"
   mode 0744
end

cookbook_file "/etc/repose/rate-limiting.cfg.xml" do
   source "rootwarnode/rate-limiting.cfg.xml"
   mode 0744
end

script "giveTomcatAccess" do
    interpreter "bash"
    code <<-EOH
        chown tomcat7 /etc/repose -R
        chown tomcat7 /var/powerapi -R
        chown tomcat7 /usr/share/repose -R
        chown tomcat7 /usr/share/lib -R
        chmod 0775 /etc/repose -R
        chmod 0775 /var/powerapi -R
        chmod 0775 /usr/share/repose -R
        chmod 0775 /usr/share/lib -R
    EOH
end

