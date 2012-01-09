
#!/usr/local/bin/ruby
require "highline/import"
require 'find'
require 'fileutils'
require 'yaml'
module Utilities 
    def debug(message)
        debugOn = false
        if debugOn
            puts "\tDebug:\t#{message}"
        end
    end

    def getChefRepo

        yaml = YAML.load_file("#{File.dirname(__FILE__)}/../configs/rcman.conf")
        return yaml["chef-Repo"]
    end

    def log(msg,clear=false)

        conf = YAML.load_file("#{File.dirname(__FILE__)}/../configs/rcman.conf")
        log = conf["log-file"]
        if !File.exists?(log)
            file = File.new(log,'w+')
        end

        if clear
            File.open(log,'w+'){|f| f.write(msg)}
        else
            File.open(log,'a+'){|f| f.write(msg)}
        end
    end

    def checkGems
        requiredGems = ["highline","builder","chef","knife-rackspace"]
        requiredGems.each do |gem|
            begin
                #Gem::Specification.find_by_name(gem)
                gem gem
            rescue Gem::LoadError
                false
                puts "Ruby Gem '#{gem}' not available."
            rescue
                Gem.available?(gem)
                puts "Ruby Gem '#{gem}' not available."
            end
        end
    end

    # Will look in ~/rc-chef for default install
    def checkChefInstall

        checkGems

        defaultChefRepo = File.expand_path("~/.rcman/rc-chef")
        requiredCookbooks = ["java","tomcat7","powerapiroot"]

        if open("/etc/hosts").grep(/chef\-server\-n01.cit.rackspace.com/).empty?
            puts "This box will not be able to communicate with the chef server. Please add the path to your /etc/hosts file"
            exit
        end

        if `which knife`.to_s.empty?
            puts "Please install 'knife' for chef"
            puts "Refer to http://wiki.opscode.com/display/chef/Workstation+Setup for installation instructions"
            exit
        end

        chefRepo = findChefRepo
        
        #check to see if this chef-repo is our repo and contains our cookbooks
        #might have to change this logic if ruby decides to evaluate both conditions even if the first is met
        if !File.exist?("#{chefRepo}/.chef/knife.rb") || open("#{chefRepo}/.chef/knife.rb").grep(/http:\/\/chef\-server\-n01\.cit\.rackspace\.com:4000/).empty?
            puts "No copy of our chef-repo found within your home directory"
            puts "Building chef-repo at '#{File.expand_path('~/')}/.rcman/rc-chef'"
            if File.exists?("#{File.expand_path('~/')}/.rcman")
                FileUtils.rm_r("#{File.expand_path('~/')}/.rcman", :force=>true)
            end
            Dir.mkdir("#{File.expand_path('~/')}/.rcman")
            Dir.mkdir("#{File.expand_path('~/')}/.rcman/rc-chef")
            setUpChef
            chefRepo = File.expand_path("~/.rcman/rc-chef")
        end
        if !File.exists?(File.expand_path("#{chefRepo}/.chef/default-template.erb"))
            FileUtils.cp("#{File.expand_path("./")}/configs/default-template.erb","#{File.expand_path("~/.rcman/rc-chef/.chef/")}")
        end
        if !File.exists?(File.expand_path("#{chefRepo}/.chef/centos-default-template.erb"))
            FileUtils.cp("#{File.expand_path("./")}/configs/centos-default-template.erb","#{File.expand_path("~/.rcman/rc-chef/.chef/")}")
        end
        conf =  {"chef-Repo"=>File.expand_path(chefRepo), "knife-config"=>"#{File.expand_path(chefRepo)}/.chef/knife.rb", "log-file"=>"#{File.expand_path(File.dirname(__FILE__))}/../configs/rcman.log"}

        #write out items for rcman configs
        File.open("#{File.expand_path(File.dirname(__FILE__))}/../configs/rcman.conf",'w') { |f|
            f.write(conf.to_yaml)
        }
    end

    def findChefRepo

        defaultChefRepo = File.expand_path("~/.rcman/rc-chef")
        chefRepo = ""

        if File.directory?(File.expand_path(defaultChefRepo))
            chefRepo = File.expand_path(defaultChefRepo)
            puts "Setting chef-repo to #{File.expand_path(defaultChefRepo)}"
        else
 
            puts "chef-repo not found in #{File.expand_path("~/.rcman/rc-chef")} or #{File.expand_path(defaultChefRepo)}"
            chefRepo = ask "Please input location of chef-repo. Type 'f' to have this script search for it from your home directory or 'na' if you do not have one"
            if chefRepo == 'f'
                #Search for chef-repo here!
                chefRepo = searchForChefRepo
            end
            #puts "Searching in #{File.expand_path} for chef-repo..."
        end
        return chefRepo

    end

    def searchForChefRepo
        Find.find(File.expand_path('~/')) do |f|
            #will display directory traversal is debugger is turned on
            debug f
            # Not going to any hidden files directly under the home directory or any .git directories
            if f.include?("#{File.expand_path('~/')}/.") || File.basename(f) == ".git"
                Find.prune
            end
            if  File.basename(f)==".chef"
                knifeConfig = "#{f}/knife.rb"
                if !open(knifeConfig).grep(/http:\/\/chef\-server\-n01\.cit\.rackspace\.com:4000/).empty?
                    puts "Found proper knife config at #{f}"
                    return  f[0..f.size-7]
                end
            end
        end
        # unable to find proper config
      #  puts "Unable to find proper repo"
        return "na"
    end

    def setUpChef

        username, key = getRsCredentials
        validateKey,userKey = getPemFiles
        puts "Creating checksums directory at #{File.expand_path("~/.rcman/.chef/checksums")}"
        Dir.mkdir("#{File.expand_path('~/')}/.rcman/.chef")
        Dir.mkdir("#{File.expand_path('~/')}/.rcman/.chef/checksums")
        conf = buildKnifeConf(username, key, validateKey.split(/\//).pop, userKey.split(/\//).pop)
        Dir.mkdir("#{File.expand_path('~/')}/.rcman/rc-chef/.chef")
        File.open("#{File.expand_path("~/.rcman/rc-chef/.chef/knife.rb")}",'w+'){ |f| f.write(conf)}
        FileUtils.cp(validateKey,"#{File.expand_path("~/.rcman/rc-chef/.chef/")}")
        FileUtils.cp(userKey,"#{File.expand_path("~/.rcman/rc-chef/.chef/")}")
        FileUtils.cp("#{File.expand_path("./")}/configs/centos-default-template.erb","#{File.expand_path("~/.rcman/rc-chef/.chef/")}")
        FileUtils.cp("#{File.expand_path("./")}/configs/default-template.erb","#{File.expand_path("~/.rcman/rc-chef/.chef/")}")
    end

    def buildKnifeConf(username, apiKey, validateKey, userKey)
        validateKey = validateKey.split(/\//).pop
        userKey = userKey.split(/\//).pop
        
        conf = "current_dir = File.dirname(__FILE__)\n"
        conf += "knife[:rackspace_api_key]\t=\t\"#{apiKey}\"\n"
        conf += "knife[:rackspace_api_username]\t=\t\"#{username}\"\n"
        conf += "log_level\t:error\nlog_location\tSTDOUT\n"
        conf += "node_name\t'#{userKey[0..userKey.size-5]}'\n" #removes the .pem from the username
        conf += "client_key\t\"\#{current_dir}/#{userKey}\"\n"
        conf += "validation_client_name\t'chef-validator'\nvalidation_key\t\"\#{current_dir}/#{validateKey}\"\n"
        conf += "chef_server_url\t'http://chef-server-n01.cit.rackspace.com:4000'\n"
        conf += "cache_type\t'BasicFile'\n"
        conf += "cache_options( :path => \"\#{ENV['HOME']}/.rcman/.chef/checksums\" )\n"
        conf += "cookbook_path\t[\"\#{current_dir}/../cookbooks\"]\n"
        return conf

    end

    def getPemFiles
        # do while loop for ruby
        begin
            pemDir = ask "Please enter the directory in which your .pem files are located. Type 'n' if you do not have these files"
        end while !File.directory?(File.expand_path(pemDir)) && pemDir!='n'

        if pemDir == 'n'
            puts "Please register at *OPSCODE* and generate your .pem files"
            exit
        else
            entries = Dir.entries(File.expand_path(pemDir))
            puts "Entries: #{entries}"
            entries = entries.select{|v| v=~/.+\.pem/} #selects all files with the extension .pem
            validator = entries.select{|v| v=~/.*validation.pem/}
            validateKey = "#{File.expand_path(pemDir)}/#{entries.delete(validator[0])}"
            if entries.size>1
                puts "Multiple pem files found for user-key"
                puts "please select valid user-key"
                for i in 0..entries.size-1
                    puts "#{i}:#{entries[i]}"
                end
                response = ask "User-Key:"
                userKey = "#{File.expand_path(pemDir)}/#{entries[response.to_i]}"
            else
                userKey = "#{File.expand_path(pemDir)}/#{entries.pop}"
            end
        end
        return validateKey,userKey
    end

    def getRsCredentials
        prompt = ask "Do you have a Cloud Servers account with API key? y/n"
        if prompt == 'y'
            rsCloudUser = ask "Please enter your Rackspace Cloud Account Username:"
            rsCloudApi = ask "Please enter your API Key:"
        else
            puts "A Cloud Servers account is needed to build these REPOSE test clusters. Please retrieve account credentials and perform this configuration again"
            exit
        end
        return rsCloudUser, rsCloudApi
    end

end
