
case platform
when "debian","ubuntu"
  default["tomcat7"]["init_style"]  = "deb"
when "redhat","centos"
  default["tomcat7"]["init_style"]  = "rhel"
end
