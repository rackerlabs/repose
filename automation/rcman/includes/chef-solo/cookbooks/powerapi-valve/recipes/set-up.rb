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

cookbook_file "/etc/init.d/repose-valve" do
    source "repose-valve"
    mode 0744
end

script "getVersionAndBuildNumbers" do
    interpreter "ruby"

    latestUrl = "http://maven.research.rackspacecloud.com/content/repositories/snapshots/com/rackspace/papi/core/valve/maven-metadata.xml"
    latest = `wget -qO- '#{latestUrl}'`.split(/<\/?version>/).select{|v| v=~ /SNAPSHOT/}.pop


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
end

package "filterBundle" do
  action :install
  source "/root/filterBundle.deb"
  provider Chef::Provider::Package::Dpkg
end

directory "/var/powerapi" do
  mode 770
  owner "root"
  group "root"
end

for i in 1..node["powerapi-valve"]["numNodes"].to_i
  directory "/etc/powerapi/node#{i}" do
    mode 0775
    owner "root"
    group "root"
  end

  directory "/var/powerapi/logs/node#{i}" do
    mode 0775
    owner "root"
    group "root"
  end
end

for i in 1..node["powerapi-valve"]["numNodes"].to_i
  ["power-proxy.cfg.xml"].each do |config|
    template "/etc/powerapi/node#{i}/#{config}" do
      source "roles/#{config}.erb"
      mode 0644
      variables(
            :filter_list => node["powerapi-valve"]["filterList"],
            :num_nodes => node["powerapi-valve"]["numNodes"]
      )
    end
  end

  ["rate-limiting.cfg.xml","container.cfg.xml"].each do |config|
    cookbook_file "/etc/powerapi/node#{i}/#{config}" do
        source config
        mode 0644
    end
  end

  if i.odd?
      cookbook_file "/etc/powerapi/node1/client-auth-n.cfg.xml" do
        source "/auth1.1/client-auth-n.cfg.xml"
        mode 0644
      end
  else
      cookbook_file "/etc/powerapi/node2/client-auth-n.cfg.xml" do
        source "/keystone/client-auth-n.cfg.xml"
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

