#!/usr/local/bin/ruby

require 'yaml'
require 'fileutils'

module ConfigureRcman

    def writeCredentials(user,apikey)

        createRcmanConfDir
        cred = {"user"=>user, "apiKey"=>apikey}
        File.open("#{File.expand_path("~/.rcman/rscredentials")}",'w'){ |f|
            f.write(cred.to_yaml)
        }
    end

    def getCredentialsConfig
        return  yaml = YAML.load_file("#{File.expand_path("~/.rcman/rscredentials")}")
    end

    def createRcmanConfDir
        rcmanDir = "~/.rcman"
        if !File.directory?(File.expand_path(rcmanDir))
            FileUtils.mkdir File.expand_path(rcmanDir)
        end
    end
end
