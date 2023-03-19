#!/bin/bash
docker run -d -p 6379:6379 --name tuplespace redis:7
echo docker rm -f tuplespace, to remove the container again
