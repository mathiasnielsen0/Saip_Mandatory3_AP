#!/bin/bash
docker run -d -p 5672:5672 -p 15672:15672 --name mq rabbitmq:3-management
echo docker rm -f mq, to stop the rabbit MQ again!
