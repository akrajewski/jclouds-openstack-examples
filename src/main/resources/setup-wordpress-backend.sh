#!/bin/bash

# Minor dns config change
sudo -- bash -c "echo 127.0.1.1 `cat /etc/hostname` >> /etc/hosts"

# Install mysql-server without password prompt
echo mysql-server mysql-server/root_password password qazxsw | sudo debconf-set-selections
echo mysql-server mysql-server/root_password_again password qazxsw | sudo debconf-set-selections
sudo apt-get install -y mysql-server

# Prepare database
echo "CREATE DATABASE wordpress;" | mysql --user=root --password=qazxsw
echo "GRANT ALL PRIVILEGES ON wordpress.* TO 'wordpress'@'localhost' IDENTIFIED BY 'dbpass';" | mysql --user=root --password=qazxsw
echo "GRANT ALL PRIVILEGES ON wordpress.* TO 'wordpress'@'%' IDENTIFIED BY 'dbpass';" | mysql --user=root --password=qazxsw
sudo sed -i 's/127.0.0.1/0.0.0.0/' /etc/mysql/my.cnf

# Restart mysql
sudo service mysql restart
