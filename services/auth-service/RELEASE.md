# Auth Service Deployment & Release Guide

This document outlines the complete procedure to install, configure, and secure the `auth-service` on the production server.

## 1. Environment Configuration

Create a secure environment file to store sensitive credentials.

**File:** `/etc/soukconect/auth.env`
**Owner:** `root:root` (Access restricted)
permission: `chmod 600`
    JWT_SECRET=<JWT SECRET>
    DB_HOST=<MYSQL HOST>
    DB_PORT=<MYSQL PORT>
    DB_NAME=<MYSQL DB>
    DB_USERNAME=<MYSQL ROOT>
    DB_PASSWORD=<MYSQL DBPASS>
    REDIS_HOST=<REDIS HOST>
    REDIS_PORT=<REDIS PASSWORD>
    REDIS_USERNAME=<REDIS USER>
    REDIS_PASSWORD=<REDIS PASSWORD>
*Note: Ensure this file is readable only by root (`chmod 600`) since the service will read it via systemd.*

## 2. Systemd Service Configuration

Create the systemd unit file to define how the service runs.

**File:** `/etc/systemd/system/auth-service.service`

    [Unit]
    Description=Auth Service
    After=network.target

    [Service]
    Type=simple
    User=soukapi
    WorkingDirectory=/home/soukapi/auth-service-1.0.0-SNAPSHOT
    ExecStart=/home/soukapi/auth-service-1.0.0-SNAPSHOT/start-auth-service.sh
    ExecStop=/home/soukapi/auth-service-1.0.0-SNAPSHOT/stop-auth-service.sh
    PIDFile=/home/soukapi/auth-service-1.0.0-SNAPSHOT/app.pid
    Restart=always

    # Load environment variables from the secure file
    EnvironmentFile=/etc/soukconect/auth.env
    # Pass specific variables to the application process
    PassEnvironment=SPRING_DATASOURCE_PASSWORD SPRING_REDIS_PASSWORD JWT_SECRET

    [Install]
    WantedBy=multi-user.target
    
    Note: 
    After modifying this file, reload the daemon:
    sudo systemctl daemon-reload

## 3. SELinux Configuration

Configure SELinux to allow `systemd` to execute the scripts in the `soukapi` home directory.

    **Run as root:**
    # 1. Add context to policy
    semanage fcontext -a -t bin_t "/home/soukapi/auth-service-1.0.0-SNAPSHOT(/.*)?"

    # 2. Apply context recursively
    restorecon -rv /home/soukapi/auth-service-1.0.0-SNAPSHOT/


## 4. Sudo Permissions for Service Management

    Grant the `soukapi` user permission to manage the service without a password.

    **File:** `/etc/sudoers.d/soukapi`
   #Add the following lines to the file
    soukapi ALL=(ALL) NOPASSWD: /bin/systemctl start auth-service, /usr/bin/systemctl start auth-service
    soukapi ALL=(ALL) NOPASSWD: /bin/systemctl stop auth-service, /usr/bin/systemctl stop auth-service
    soukapi ALL=(ALL) NOPASSWD: /bin/systemctl restart auth-service, /usr/bin/systemctl restart auth-service
    soukapi ALL=(ALL) NOPASSWD: /bin/systemctl status auth-service, /usr/bin/systemctl status auth-service

    *(Added both `/bin/` and `/usr/bin/` paths to be safe)*

## 5. Deployment Workflow

1.  **Stop the service:**
    sudo systemctl stop auth-service
2.  **Upload new artifacts** 
    (JAR and scripts) to `/home/soukapi/auth-service-1.0.0-SNAPSHOT/`.
3.  **Start the service:**
    sudo systemctl start auth-service
**Important:** The service MUST be managed only via `systemctl` because it depends on the secure environment variables loaded by the systemd unit. Running the script directly as user `soukapi` will fail because it cannot read the environment file.