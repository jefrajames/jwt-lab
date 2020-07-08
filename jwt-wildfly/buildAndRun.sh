#!/bin/sh
mvn clean package && docker build -t org.jefrajames/jwtdemo .
docker rm -f jwtdemo || true && docker run -d -p 8080:8080 -p 4848:4848 --name jwtdemo org.jefrajames/jwtdemo 
