directory "/usr/share/lib" do
   owner "root"
   group "root"
   action :create
end

cookbook_file "/etc/init.d/repose-regression2" do
   source "repose-regression2"
   mode 0744
end

directory "/var/powerapi" do
  mode 770
  owner "root"
  group "root"
end

script "getVersionAndBuildNumbers" do
   interpreter "ruby"
   `rm -rf /var/lib/tomcat7/webapps/*`
   latestUrl = "http://maven.research.rackspacecloud.com/content/repositories/snapshots/com/rackspace/repose/installation/deb/valve/repose-valve/maven-metadata.xml"
   latest = `wget -qO- '#{latestUrl}'`.split(/<\/?version>/).select{|v| v=~ /SNAPSHOT/}.pop


   rootWarUrl = "http://maven.research.rackspacecloud.com/content/repositories/snapshots/com/rackspace/papi/core/web-application/#{latest}"
   mocksUrl = "http://maven.research.rackspacecloud.com/content/repositories/snapshots/com/rackspace/papi/external/testing/test-service-mock/#{latest}/"
   valveUrl = "http://maven.research.rackspacecloud.com/content/repositories/snapshots/com/rackspace/repose/installation/deb/valve/repose-valve/#{latest}"
   filterBundleUrl = "http://maven.research.rackspacecloud.com/content/repositories/snapshots/com/rackspace/repose/installation/deb/filters/repose-filter-bundle/#{latest}"
   eFilterBundleUrl = "http://maven.research.rackspacecloud.com/content/repositories/snapshots/com/rackspace/papi/components/extensions/extensions-filter-bundle/#{latest}"

   rTimeStamp= `wget -qO- "#{rootWarUrl}/maven-metadata.xml"`.split(/<\/?value>/)
   mTimeStamp = `wget -qO- "#{mocksUrl}/maven-metadata.xml"`.split(/<\/?value>/)
   vTimeStamp= `wget -qO- "#{valveUrl}/maven-metadata.xml"`.split(/<\/?value>/)
   fTimeStamp= `wget -qO- "#{filterBundleUrl}/maven-metadata.xml"`.split(/<\/?value>/)
   eTimeStamp= `wget -qO- "#{eFilterBundleUrl}/maven-metadata.xml"`.split(/<\/?value>/)

   rValue= rTimeStamp[rTimeStamp.size-2]
   mValue= mTimeStamp[mTimeStamp.size-2]
   vValue= vTimeStamp[vTimeStamp.size-2]
   fValue= fTimeStamp[fTimeStamp.size-2]
   eValue= eTimeStamp[eTimeStamp.size-2]

   `wget #{rootWarUrl}/web-application-#{rValue}.war -O /var/lib/tomcat7/webapps/ROOT.war`
   `wget #{mocksUrl}/test-service-mock-#{mValue}.war -O /var/lib/tomcat7/webapps/mocks.war`
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



directory "/etc/repose" do
   owner "root"
   group "root"
   action :create
end


template "/etc/repose/system-model.cfg.xml" do
 source "secondnode/rootwarnode/system-model.cfg.xml.erb"
 mode 0644
end

cookbook_file "/etc/repose/destination-router.cfg.xml" do
   source "secondnode/rootwarnode/destination-router.cfg.xml"
   mode 0744
end

cookbook_file "/etc/repose/container.cfg.xml" do
   source "secondnode/rootwarnode/container.cfg.xml"
   mode 0744
end



for nodeNumber in 7..12

   port = 8886 + nodeNumber

   directory "/etc/repose/node#{nodeNumber}" do
      mode 0775
      owner "root"
      group "root"
      action :create
   end

   directory "/var/log/repose/node#{nodeNumber}" do
      mode 0775
      owner "root"
      group "root"
      recursive true
      action :create
   end

   template "/etc/repose/node#{nodeNumber}/container.cfg.xml" do
      source "multimatch/container.cfg.xml.erb"
      mode 644
      variables({
         :nodeNumber => "#{nodeNumber}",
         :port => "#{port}"
      })
   end

   template "/etc/repose/node#{nodeNumber}/log4j.properties" do
      source "multimatch/log4j.properties.erb"
      mode 644
      variables({
         :nodeNumber => "#{nodeNumber}"
      })
   end

   template "/etc/repose/node#{nodeNumber}/system-model.cfg.xml" do
      source "multimatch/node#{nodeNumber}/system-model.cfg.xml.erb"
      mode 644
      variables({
         :nodeNumber => "#{nodeNumber}",
         :port => "#{port}"
      })
   end

   cookbook_file "/etc/repose/node#{nodeNumber}/response-messaging.cfg.xml" do
      source "multimatch/response-messaging.cfg.xml"
      mode 0644
   end

   cookbook_file "/etc/repose/node#{nodeNumber}/validator.cfg.xml" do
      source "multimatch/node#{nodeNumber}/validator.cfg.xml"
      mode 0644
   end

   case nodeNumber
   when 7
      cookbook_file "/etc/repose/node#{nodeNumber}/sspnn-fail.wadl" do
         source "multimatch/node#{nodeNumber}/sspnn-fail.wadl"
         mode 0644
      end

      cookbook_file "/etc/repose/node#{nodeNumber}/sspnn-nc.wadl" do
         source "multimatch/node#{nodeNumber}/sspnn-nc.wadl"
         mode 0644
      end

      cookbook_file "/etc/repose/node#{nodeNumber}/sspnn-pass.wadl" do
         source "multimatch/node#{nodeNumber}/sspnn-pass.wadl"
         mode 0644
      end

   when 8
      cookbook_file "/etc/repose/node#{nodeNumber}/f-fail.wadl" do
         source "multimatch/node#{nodeNumber}/f-fail.wadl"
         mode 0644
      end

   when 9
      cookbook_file "/etc/repose/node#{nodeNumber}/p-pass.wadl" do
         source "multimatch/node#{nodeNumber}/p-pass.wadl"
         mode 0644
      end

   when 10
      cookbook_file "/etc/repose/node#{nodeNumber}/mssfsffpnn-fail.wadl" do
         source "multimatch/node#{nodeNumber}/mssfsffpnn-fail.wadl"
         mode 0644
      end

      cookbook_file "/etc/repose/node#{nodeNumber}/mssfsffpnn-nc.wadl" do
         source "multimatch/node#{nodeNumber}/mssfsffpnn-nc.wadl"
         mode 0644
      end

      cookbook_file "/etc/repose/node#{nodeNumber}/mssfsffpnn-pass.wadl" do
         source "multimatch/node#{nodeNumber}/mssfsffpnn-pass.wadl"
         mode 0644
      end

   when 11
      cookbook_file "/etc/repose/node#{nodeNumber}/mf-fail.wadl" do
         source "multimatch/node#{nodeNumber}/mf-fail.wadl"
         mode 0644
      end

   when 12
      cookbook_file "/etc/repose/node#{nodeNumber}/mp-pass.wadl" do
         source "multimatch/node#{nodeNumber}/mp-pass.wadl"
         mode 0644
      end

   end

end


script "giveTomcatAccess" do
    interpreter "bash"
    code <<-EOH
        chown tomcat7 /etc/repose -R
        chown tomcat7 /var/powerapi -R
        chown tomcat7 /var/repose -R
        chown tomcat7 /var/log/repose -R
        chown tomcat7 /usr/share/repose -R
        chown tomcat7 /usr/share/lib -R
        chmod 0775 /etc/repose -R
        chmod 0775 /var/powerapi -R
        chmod 0775 /var/repose -R
        chmod 0775 /var/log/repose -R
        chmod 0775 /usr/share/repose -R
        chmod 0775 /usr/share/lib -R
    EOH
end
