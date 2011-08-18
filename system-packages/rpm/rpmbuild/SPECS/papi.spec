%global _confDir /etc/powerapi
%global _depDirLogs /var/powerapi/logs
%global _artifactDir /usr/share/powerapi/filters
%global _tomcatBase /var/lib/tomcat7
#This is where tomcat is supposed to be installed
%global _appBase /var/lib/tomcat7/webapps
%global _filterBundleVer 1.0.8-20110810.214301-137
%global _appWARName v1.1

Name:           papi
Summary:        Power API
Version:        1.0.0
Release:        1%{?dist}

Group:          application/software
License:        Rackspace
URL:            http://maven-n01.rcloudtech.rackspacecloud.com/m2/content/groups/public/com/rackspace/cloud/powerapi
Source0:        ROOT.war
Source1:        filter-bundle-1.0.8-SNAPSHOT.ear
Source2:        container.cfg.xml
Source3:        rate-limiting.cfg.xml
Source4:        power-proxy.cfg.xml 
Source5:        versioning.cfg.xml 

#BuildRoot:      %{_tmppath}/%{name}-%{version}-%{release}-root-%(%{__id_u} -n)
BuildRoot:      /home/malconis/papi/rpmbuild
BuildArch:      noarch

#Requires:       tomcat7
#Requires:       java-1.6.0-sun


%description
Power API is a stack of reusable, software components that can be leveraged by service developers to perform common API processing tasks.
Source code: https://source.admin.stabletransit.com/thesystem/API/api-auth/trunk/

%pre
#useradd powerapi -s /dev/null -b /var/ #not needed anymore as the applicaiton will piggy-back off of tomcat

%install
rm -rf %{buildroot}
mkdir -p %{buildroot}/%{_appBase}
mkdir -p %{buildroot}/%{_tomcatBase}/conf
mkdir -p %{buildroot}/%{_confDir} -m 0655 
mkdir -p %{buildroot}/%{_depDirLogs} -m 0770
mkdir -p %{buildroot}/%{_artifactDir} -m 0555
unzip -d %{buildroot}/%{_appBase}/%{_appWARName} %{SOURCE0}
cp %{SOURCE1} %{buildroot}/%{_artifactDir}/filter-bundle-%{_filterBundleVer}.ear
cp %{SOURCE2} %{buildroot}/%{_confDir}/container.cfg.xml
cp %{SOURCE3} %{buildroot}/%{_confDir}/rate-limiting.cfg.xml
cp %{SOURCE4} %{buildroot}/%{_confDir}/power-proxy.cfg.xml
cp %{SOURCE5} %{buildroot}/%{_confDir}/versioning.cfg.xml

%clean
rm -rf %{buildroot}

%files
%defattr(-,root,root,-)
%{_appBase}/%{_appWARName}
%attr(0644, root, tomcat7) %{_artifactDir}/filter-bundle-%{_filterBundleVer}.ear
%config %attr(0644, tomcat7, tomcat7) %{_confDir}/container.cfg.xml
%config %attr(0644, tomcat7, tomcat7) %{_confDir}/rate-limiting.cfg.xml
%config %attr(0644, tomcat7, tomcat7) %{_confDir}/power-proxy.cfg.xml
%config %attr(0644, tomcat7, tomcat7) %{_confDir}/versioning.cfg.xml
%dir %attr(0775, root, tomcat7) /var/powerapi/logs

%post
#No known post processing



%changelog
* Fri Aug 12 2011 Franard <john.hopper@rackspace.com> 1.1-1
- initial build







