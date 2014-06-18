#!/bin/bash

# Minor dns config change
sudo -- bash -c "echo 127.0.1.1 `cat /etc/hostname` >> /etc/hosts"

# Install wordpress
sudo apt-get install -y wordpress

# Install mysql-server without password prompt
echo mysql-server mysql-server/root_password password qazxsw | sudo debconf-set-selections
echo mysql-server mysql-server/root_password_again password qazxsw | sudo debconf-set-selections
sudo apt-get install -y mysql-server 

# Link absolute path to WordPress to /var/www
sudo ln -s /usr/share/wordpress /var/www/wordpress

# Prepare database
echo "CREATE DATABASE wordpress; GRANT ALL PRIVILEGES ON wordpress.* TO 'wordpress'@'localhost' IDENTIFIED BY 'dbpass';" | mysql --user=root --password=qazxsw

# Prepare WordPress config
sudo cp /usr/share/wordpress/wp-config-sample.php /etc/wordpress/wp-config.php
sudo sed -i 's/password_here/dbpass/' /etc/wordpress/wp-config.php
sudo sed -i 's/username_here/wordpress/' /etc/wordpress/wp-config.php
sudo sed -i 's/database_name_here/wordpress/' /etc/wordpress/wp-config.php

# Restart apache
sudo service apache2 restart
