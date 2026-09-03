#!/bin/bash

docker-compose down
docker pull thanicz/eotm:latest
docker-compose up -d
docker image prune -a -f
