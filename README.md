# jclouds-openstack-examples

This project presents usage example of JClouds Openstack API.
It creates a small Wordpress application consisting of backend database
server and a pool of frontend web servers. Only Nova and Neutron APIs are used.

## Building

This is a Maven project. Make sure you have Maven installed and available in your path.
Then just cd into the project root and run:
    
    $ mvn package

## Config

Edit the contents of the main.Config class source code and set the correct
Openstack auth params

## Running 

From project root run:

    $ java -cp java -cp "target/*:target/lib/*" main.Program
