case platform
  when "centos","redhat"
    default["powerapi-valve"]["image"] = "rhel"
  else
    default["powerapi-valve"]["image"] = "deb" 
end

default["powerapi-valve"]["numNodes"] = 2
default["powerapi-valve"]["filterList"] = ["dist-datastore","versioning","client-auth","rate-limiting"]
