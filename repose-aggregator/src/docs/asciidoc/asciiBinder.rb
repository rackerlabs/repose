#!/usr/bin/env ruby
require 'colorize'
require 'ascii_binder'

puts "-" * 20
puts "Ruby version: #{RUBY_VERSION}"
puts "Ruby platform: #{RUBY_PLATFORM}"
puts "-" * 20

puts "Roses are red".red
puts "Violets are blue".blue
puts "I can use JRuby/Gradle".green
puts "And now you can too!".yellow

puts "Input:  #{ENV['DIR_IN']}"
puts "Output: #{ENV['DIR_OUT']}"
puts "AsciiBinder: #{AsciiBinder::VERSION}"

exit
