Connectors
===

Goal: *Demonstrate various **different process** connectors*.

Nygard (2017)'s pattern *Decoupling Middleware* mentions three types of
connectors for *different process* data and control flow: remote
procedure call (RPC), messaging, and shared memory/tuplespace.

This demonstration code base allows experimentation with all three
types.

*Note*: This ReadMe is best viewed in IntelliJ which understands the
MarkDown markup language used.

Case Study
---

Our case study is a *temperature sensor* (service/component) in a
chemical plant which simply measures the temperature in a given
process, like 104,5 Celcius.

Various other services/components needs the temperature information for
monitoring/alerting/actuation, like showing the temperature on a display,
sending SMS alerts in case the temperature is above a threshold, or
actuating a cooling system, or opening an emergency valve, in case a
temperature limit is exceeded.

To keep complexity low, only a single 'temperature monitor' is
provided, which simply writes a log message with the temperature read.

Also only a single temperature sensor is read by the server, denoted
'sensor 217'.

**Note:** This case study is a highly biased one towards a *shared
memory* connector, as no need for control flow is really necessary.

Project structure
---

The current project consists of four project

    core: shared code with a (faked) temperature sensor
    rest: the RPC code base, using ReST as RPC connector
    mq: the Messaging code base, using RabbitMQ as intermediary
    tuple: the Tuplespace code base, using Redis as intermediary and
    surrogate of a tuplespace
    
The sections below clarify how each of the three demo's are run, some
exercises to do, and a summary of the connector type's characteristics.

All demo code bases are to be run in a shell/dos prompt/terminal!  All
*intermediates* are run as docker containers, which requires issuing
rather complex docker commands - at present, just consider them *magic
spells* that make these intermediates appear. We will return to Docker
later in the course.

Remote Procedure Call
---

There are plenty of options in the RPC space: Java RMI and other
language specific technologies, gRPC, SOAP, raw HTTP, REST, and
more. And of course, my own FRDS.Broker library :).

REST is picked here as it is pretty widespread currently (2023).  I
use the SparkJava framework for the server side, and Java11's
HttpClient library. If you are on a Java10 or earlier, you have to
recode the monitor, I myself like the UniRest http client library
(also more than the Java11 library, but...)

As RPC is an *explicit invocation* pattern (Avgeriou, 2005), there is
no need for any intermediate.

Start the temperature server

    gradle restserver
    
which will read the temperature value and store it internally every
1.5 second, as well as log the value.

Start the temperature monitor

    gradle restmonitor
    
which will display the temperature every 2.5 seconds. Alternatively,
you can use a browser as monitor, simply browse to
[http://localhost:4567/temperature/217],
and 'shift-f5' / refresh the page ever so often to read the latest value.

**Exercise:** Run a server and one (or more) monitors (each in a
seperate shell). Note how some temperature readings are 'missed' by
the monitor. Try to kill the server, and see the obvious result.

*Summary:* The restserver will measure (faked) temperatures and simply
store it internally, so every RCP (a http GET request on path
'temperature/217') will fetch this value. The restmonitor periodically
makes the RPC call to fetch the latest temperature. **RPC is an
explicit invocation style, and highly vulnarable to server
failures. The connector transfers both control and data.**

Messaging
---

Messaging requires an intermediate, and I have choosen RabbitMQ, as
their tutorials are really great, and it is a widely used MessageQueue
(MQ) system which offers quite a lot of behaviour out of the box (RPC,
Pub-Sub, ...).

To run a RabbitMQ server, start it as docker container

    docker run -d -p 5672:5672 -p 15672:15672 --name mq rabbitmq:3-management
    
or run the script: `./startmq.sh`.

(Docker is a container based virtual machine, we will return to it
later in the course).

To see what is going on, open the RabbitMQ dashboard using your
browser on [http://localhost:15672/mgmt], and log in using 'guest' and
'guest' as user and password. The dashboard is a bit overwhelming, but
use the tabs on top to review exchanges, queues, etc.

(Stop the RabbitMq again with `docker rm -f mq`.)

Start the temperature server

    gradle mqserver
    
which will publish the temperature tuple every 1.5 second on a
RabbitMQ exchange. You can see the data flowing in, on the dashboard.

Start the monitor 

    gradle mqmonitor

which will display the temperature as soon as they are available in
the queue.

**Exercise:** Try to run a server and one monitors at the same time;
note how their pace is of course lock-step and that the monitor never
skips a measurements. Try to stop one or the other, and see what
happens. Try to start a second monitor, wait a bit, and then stop the
first monitor. Explain what happens.

Rewrite the server so any temperature above 106 Celcius is also
transmitted with topic 'plant.alert' and make another 'alertmonitor'
which only displays these temperatures. (Hint: Search for RabbitMQ's
'Topic' tutorial in Java, on WWW).

Rewrite so all monitors gets all temperature readings.

*Summary:* The mqserver will write (faked) temperatures into a
*exchange* named 'measurements' using a specific topic
'plant.temperature'. The monitor will bind a *queue* 'plant-queue' to
the exchange using a routing key, like '*.*', and receive all
messages. If no message is fetched from the queue, it is queued
(deliverymode 2 = persistent), and even survive RabbitMq restarts
(exchange and queue are 'durable'). Thus the producer and consumer are
decoupled in time. It is an **event oriented connector as consumers
are guarantied to always receive all messages sent.**

The connector transfers primarily data, but of course the event-driven
nature also is highly supportive of control flow.

Tuplespace
---

Tuplespace/shared memory requires an intermediate, the *distributed
shared memory* with the two operations *read* and
*write*. Experimentation with Tuplespace implementations in Java were
discouraging, so I have opted for using an fast, distributed, cache,
namely Redis. Redis is a key-value store, so operations *set* and
*get* act nicely as a shared memory. Redis can be configured to flush
contents to disk and thereby act as an ordinary database server.

To run a Redis, start it as a docker container

    docker run -d -p 6379:6379 --name tuplespace redis:7

or run the script: `./startredis.sh`.

To see what is going on, use the redis-cli on the running container

    docker exec -ti tuplespace redis-cli
    
The temperature value stored can be seen by

    redis> get sensor_217
    
('exit' to quit the redis-cli)

You can stop the redis server by `docker rm -f tuplespace`.
    
Start the temperature server

    gradle tupleserver
    
which will store the temperature tuple every 1.5 second, as well as
log it to the shell.

Start the monitor 

    gradle tuplemonitor

which will read the temperature every 2.5 seconds and display it.

**Exercise:** Try to run the both at the same time; note how their
pace is different and that the monitor often skips some
measurements. Try to stop one or the other, and see what happens. Try
to run multiple monitors at the same time.

*Summary*: The tupleserver will read a (faked) temperature
periodically, and *write* it to shared memory. The tuplemonitor will
periodically *read* the temperature from shared memory. Thus the
producer and consumer are decoupled in time. It is a **state oriented
connector** as only the last written value is available.

This connector can only handle data flow, never control flow, except
by tedious polling.

Version
------

2020: Initial version.

2023: Refactored to update to Gradle 7, and updating of drivers for
MQ, Redis.
