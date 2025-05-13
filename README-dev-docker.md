# DSpace 7 Development with Docker

This guide explains how to use the development Docker setup for DSpace 7.

## Overview

The `docker-compose-dev.yml` file provides a development environment for DSpace 7 that includes:

1. A development container (`dspace-dev`) that mounts your local source code and runs the application in development mode
2. A PostgreSQL database container (`dspacedb`) with pgcrypto extension
3. A Solr container (`dspacesolr`) with all required cores pre-configured

This setup allows you to make changes to your local code and see them reflected immediately in the running application.

## Prerequisites

- Docker and Docker Compose installed on your system
- Git repository of DSpace 7 cloned locally

## Getting Started

### 1. Start the development environment

```bash
# Navigate to the DSpace directory
cd /path/to/DSpace7/dspace

# Start the development environment
# Use -p to specify a project name (optional)
docker compose -f docker-compose-dev.yml up -d
```

### 2. Access the application

- DSpace REST API: http://localhost:8080/server
- Solr Admin UI: http://localhost:8983/solr/

### 3. Remote Debugging

The development container exposes port 5006 for remote debugging. You can connect your IDE to this port to debug the application.

In IntelliJ IDEA:
1. Go to Run → Edit Configurations
2. Add a new "Remote JVM Debug" configuration
3. Set the host to "localhost" and the port to "5006"
4. Save and start the debug session

### 4. Working with the development container

```bash
# View logs
docker logs -f dspace-dev

# Execute commands in the container
docker exec -it dspace-dev /bin/bash

# Run DSpace commands
docker exec -it dspace-dev /dspace/bin/dspace [command]
```

### 5. Database and Solr persistence

The database and Solr data are stored in Docker volumes, so they persist between container restarts. If you need to start with a fresh database or Solr instance, you can remove these volumes:

```bash
# Remove volumes (WARNING: This will delete all data)
docker compose -f docker-compose-dev.yml down -v
```

## Customizing the Development Environment

You can customize the development environment by modifying the `docker-compose-dev.yml` file or by setting environment variables. Some common customizations include:

### Overriding Build Arguments

You can override the build arguments defined in the dev.Dockerfile by setting environment variables before running docker-compose:

```bash
# Override DSPACE_DIR and LOG4J_CONFIG_FILE
export DSPACE_DIR=/custom/dspace/path
export LOG4J_CONFIG_FILE=/custom/path/to/log4j2.xml
export JDK_VERSION=17  # Use a different JDK version
export DOCKER_REGISTRY=my-registry.example.com  # Use a custom Docker registry

# Then run docker-compose
docker compose -f docker-compose-dev.yml up -d
```

These environment variables will be passed as build arguments to the dev.Dockerfile during the build process.

### Other Customizations

- Adding environment variables to override DSpace configuration
- Mounting additional directories for custom modules
- Changing port mappings

## Troubleshooting

### Connection issues between containers

If containers cannot connect to each other, ensure they are on the same network. The `dspacenet` network is defined in the docker-compose file.

### Maven build issues

If you encounter Maven build issues, you can execute Maven commands directly in the container:

```bash
docker exec -it dspace-dev mvn -f dspace/modules/server-boot/pom.xml clean package
```

### Remote debugging not working

Ensure that:
1. Port 5006 is published in the docker-compose file
2. Your IDE is configured to connect to localhost:5006
3. The JVM arguments in the container include the debug configuration

## Additional Resources

- [DSpace Documentation](https://wiki.lyrasis.org/display/DSDOC7x)
- [DSpace 7 REST API Documentation](https://wiki.lyrasis.org/display/DSDOC7x/REST+API)