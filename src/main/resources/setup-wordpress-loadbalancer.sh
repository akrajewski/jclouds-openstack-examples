#!/bin/bash

# Minor dns config change
sudo -- bash -c "echo 127.0.1.1 `cat /etc/hostname` >> /etc/hosts"

# Install apache web server
sudo apt-get install -y apache2

# Turn on proxy mods
sudo ln -s /etc/apache2/mods-available/proxy.load /etc/apache2/mods-enabled/proxy.load 
sudo ln -s /etc/apache2/mods-available/proxy.conf /etc/apache2/mods-enabled/proxy.conf
sudo ln -s /etc/apache2/mods-available/proxy_balancer.load /etc/apache2/mods-enabled/proxy_balancer.load
sudo ln -s /etc/apache2/mods-available/proxy_balancer.conf /etc/apache2/mods-enabled/proxy_balancer.conf
sudo ln -s /etc/apache2/mods-available/proxy_http.load /etc/apache2/mods-enabled/proxy_http.load

sudo -- bash -c 'cat << EOF > /etc/apache2/mods-enabled/proxy_balancer.conf
<IfModule mod_proxy_balancer.c>

<Proxy balancer://wordpress>
{balancer_members}
	Order allow,deny
	Allow from all
</Proxy>

ProxyPass / balancer://wordpress

</IfModule>
EOF'

sudo service apache2 restart
