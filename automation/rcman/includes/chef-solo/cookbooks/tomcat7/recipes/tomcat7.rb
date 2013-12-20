

case node["tomcat7"]["init_style"]
when "deb"
  directory "/tmp/tomcat7" do
    action :create
  end

  ["libcommons-collections3-java_3.2.1-4_all.deb", "libecj-java_3.5.1-2.1_all.deb", "tomcat7_7.0.19-1_all.deb", "libcommons-dbcp-java_1.4-1_all.deb", "libservlet3.0-java_7.0.19-1_all.deb", "tomcat7-common_7.0.19-1_all.deb",   "libcommons-pool-java_1.5.6-1_all.deb", "libtomcat7-java_7.0.19-1_all.deb"].each do |debfile|
    cookbook_file "/tmp/tomcat7/#{debfile}" do
      source "/deb/#{debfile}"
    end
  end

  package "libecj-java" do
    action :install
    source "/tmp/tomcat7/libecj-java_3.5.1-2.1_all.deb"
    provider Chef::Provider::Package::Dpkg
  end

  package "libcommons-pool-java" do
    action :install
    source "/tmp/tomcat7/libcommons-pool-java_1.5.6-1_all.deb"
    provider Chef::Provider::Package::Dpkg
  end

  package "libcommons-collections3-java" do
    action :install
    source "/tmp/tomcat7/libcommons-collections3-java_3.2.1-4_all.deb"
    provider Chef::Provider::Package::Dpkg
  end

  package "libservlet3.0-java" do
    action :install
    source "/tmp/tomcat7/libservlet3.0-java_7.0.19-1_all.deb"
    provider Chef::Provider::Package::Dpkg
  end

  package "libcommons-dbcp-java" do
    action :install
    source "/tmp/tomcat7/libcommons-dbcp-java_1.4-1_all.deb"
    provider Chef::Provider::Package::Dpkg
  end

  package "libtomcat7-java" do
    action :install
    source "/tmp/tomcat7/libtomcat7-java_7.0.19-1_all.deb"
    provider Chef::Provider::Package::Dpkg
  end

  package "tomcat7-common" do
    action :install
    source "/tmp/tomcat7/tomcat7-common_7.0.19-1_all.deb"
    provider Chef::Provider::Package::Dpkg
  end

  package "tomcat7" do
    action :install
    source "/tmp/tomcat7/tomcat7_7.0.19-1_all.deb"
    provider Chef::Provider::Package::Dpkg
  end

  cookbook_file "/var/lib/tomcat7/conf/context.xml" do
      source "context.xml"
      mode 0755
  end

when "rhel"

  cookbook_file "/var/lib/apache-tomcat-7.0.20.tar.gz" do
    source "/rhel/apache-tomcat-7.0.20.tar.gz"
  end

  cookbook_file "/etc/init.d/tomcat7" do
    source "rhel/tomcat7"
    mode 0755
  end

  script "install_tomacat7" do
    interpreter "bash"
    cwd "/var/lib"
    code <<-EOH
    tar -xzvf apache-tomcat-7.0.20.tar.gz
    mv apache-tomcat-7.0.20 tomcat7
    rm apache-tomcat-7.0.20.tar.gz -f
    rm -rf /var/lib/tomcat7/webapps/ROOT
    EOH
  end

  user "tomcat7" do
    comment "tomcat7 user"
    home "/var/lib/tomcat7"
    shell "/dev/null"
  end

  script "flushIptables" do
    interpreter "bash"
    code <<-EOH
    chkconfig --add tomcat7
    chkconfig --level tomcat7 on
    /sbin/iptables -P INPUT ACCEPT
    /sbin/iptables -P OUTPUT ACCEPT
    /sbin/iptables -F
    EOH
  end

  script "startTomcat7" do
    interpreter "bash"
    code <<-EOH
    service tomcat7 start
    EOH
  end


end
