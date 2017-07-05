# CQRS Manager for Distributed Reactive Services

CQRS Manager for Distributed Reactive Services (herein abbreviated
CMDR), is a reference implementation for the key component in a
specific [architecture](./doc/architecture.png) for building
distributed information services following a
[Log-centric REST+CQRS+ES](https://speakerdeck.com/bobbycalderwood/commander-better-distributed-applications-through-cqrs-event-sourcing-and-immutable-logs)
design.

The role of the CMDR component is to handle incoming Commands by:

1. Ensuring their conformity to Schemas (coming soon)
2. Writing them down to the Log (Kafka in this implementation)

Additionally, CMDR indexes all Commands and Events from their respective Kafka topics in order to:

1. Respond to read (GET) requests for information about Commands and Events
2. Provide a Server Sent Events (SSE) interface to both Commands (`/commands/updates`) and Events (`/events/updates`)

## Status

**IMPORTANT!** This is alpha-quality software, meant mostly to
demonstrate the
[Log-centric REST+CQRS+ES](https://speakerdeck.com/bobbycalderwood/commander-better-distributed-applications-through-cqrs-event-sourcing-and-immutable-logs)
[architecture](./doc/architecture.png) described in the linked talks, and
to facilitate learning and discussion.

As this implementation reaches maturity, and becomes suited for
production-use, this note will be removed from the README.

## Documentation

CMDR is the component in your system that handles all incoming
actions/writes, but it's totally ignorant of your business logic and
domain. You'll need to build microservices that implement your
business logic, and read-only REST endpoints to expose the resulting
data.  You'll end up with a system architecture that looks
[like this](./doc/architecture.png), and has
[many benefits](./doc/rationale.md).

This README will help you get CMDR running, but doesn't tell you how
to integrate with it or why you might want to.  See the
[rationale](./doc/rationale.md) for why you'd use CMDR, and
[the contract documentation](./doc/using.md) for details about how to
integrate with CMDR.

## Running in development

CMDR is a [Leiningen](http://leiningen.org/) project, and behaves as
you would expect in terms of running tests, launching REPLs, packaging
uberjars, etc.

CMDR runs as two microservices
(`com.capitalone.commander.indexer` and
`com.capitalone.commander.rest`) that coordinate via
[Apache Kafka](http://kafka.apache.org/) and a JDBC-compliant database
like [PostgreSQL](https://www.postgresql.org/).  In order to run the
CMDR services, you'll first need to run a Kafka Cluster and a
database.

### Supporting Services

There is a handy `Makefile` for running and interacting with the
supporting services and/or the example application.  This `Makefile`
uses `docker` and `docker-compose` under the hood to orchestrate the
various services.

Running the supporting services (Kafka, ZooKeeper, PostgreSQL) via the
`Makefile` is the easiest way to get started.  However, if you want to
run the supporting services manually, you can use a package manager to
install the services.

#### Makefile

The easiest way to get CMDR's supporting services up-and-running
quickly in development is via the `Makefile`.

``` sh
$ make services
$ make service-bootstrap # bootstraps database, etc.
```

Then run the CMDR services locally from the REPL or via `lein
run` as described below, and visit http://localhost:3000/ui/ to see
the Swagger/OpenAPI user interface to the CMDR service.

Or you can run an entire example system (including both CMDR
services and an example business logic service):

``` sh
$ make example
```

Then you can visit the CMDR service at http://localhost:3000/ui/, and
the sample application's REST API at http://localhost:8080/customers.

#### Running Manually

The following instructions provide an example for those running on Mac
OS X using the [Homebrew](http://brew.sh/) package manager to install
the supporting services.

##### Install

###### Kafka and ZooKeeper

Kafka uses [Apache ZooKeeper](https://zookeeper.apache.org/) to maintain
runtime state and configuration consistently across the cluster.
You'll need to install both Kafka and ZooKeeper.

``` sh
$ brew update
$ brew install kafka
```

###### PostgresQL

To install postgres:

``` sh
$ brew install postgresql
```

##### Running

###### Kafka and ZooKeeper

In one shell:

``` sh
$ zookeeper-server-start /usr/local/etc/kafka/zookeeper.properties
```

In a second shell:

``` sh
$ kafka-server-start /usr/local/etc/kafka/server.properties
```


###### PostgreSQL

In third shell:

``` sh
$ postgres -D /usr/local/var/postgres
```

Then bootstrap the database in a fourth shell:

``` sh
$ bin/run com.capitalone.commander.database 'jdbc:postgresql://localhost/postgres?user=postgres&password=postgres' commander commander commander
```

You'll want to use `commander` as the password for the `commander` user for
the dev environment to work.

# Clojure REPL

In the third shell:

``` sh
$ lein repl
```

Connect an [nrepl](https://github.com/clojure/tools.nrepl) client -- perhaps in your favorite editor, or else type directly into the REPL
session in your shell:

``` clojure
(go)
```

### Workflow

This application follows the
[reloaded workflow](http://thinkrelevance.com/blog/2013/06/04/clojure-workflow-reloaded)
using the
[component pattern](https://github.com/stuartsierra/component),
providing a clean system architecture and reloadable REPL development
environment.

#### Running in development

At the Clojure REPL, you first need to run database migrations.

``` clojure
(migrate-database)
```

You can then run the system:

``` clojure
(go)
```

And then reload the entire app, refreshing all the code:

``` clojure
(reset)
```

You can also run the tests right from the REPL (in addition to running
via `lein test`):

``` clojure
(test)
```

#### Running tests

To run tests:

``` sh
$ lein test
```

or

``` sh
$ lein auto test
```

to run tests automatically every time a file is saved.

### Swagger/OpenAPI

When running the API, you can view the
[Swagger/OpenAPI UI](http://localhost:3000/ui/).

Swagger/OpenAPI functionality provided by
[pedestal-api](https://github.com/oliyh/pedestal-api).

## Build

### Uberjar

You can build a self-contained JAR that includes all dependencies via:

``` sh
$ lein do clean, uberjar
```

You then run the application:

``` sh
$ java -jar target/uberjar/cmdr-standalone.jar -m com.capitalone.commander.indexer
$ # OR
$ java -jar target/uberjar/cmdr-standalone.jar -m com.capitalone.commander.rest
```

### Docker

The Docker image is based on the
[Alpine Linux version](https://github.com/Quantisan/docker-clojure/blob/534102a7ee7e6f4825e86a648a52c44cc25eb39d/alpine/Dockerfile)
of the official
[Clojure repository on Docker Hub](https://hub.docker.com/_/clojure/)

``` sh
$ docker build -t my-cmdr-build .
```

You can then run either of the CMDR services via the built image:

``` sh
$ docker run my-cmdr-build com.capitalone.commander.indexer
$ # OR
$ docker run my-cmdr-build com.capitalone.commander.rest
```

## Contributors

We welcome your interest in Capital One’s Open Source Projects (the
“Project”). Any Contributor to the project must accept and sign a CLA
indicating agreement to the license terms. Except for the license
granted in this CLA to Capital One and to recipients of software
distributed by Capital One, you reserve all right, title, and interest
in and to your contributions; this CLA does not impact your rights to
use your own contributions for any other purpose.

[Link to CLA](https://docs.google.com/forms/d/19LpBBjykHPox18vrZvBbZUcK6gQTj7qv1O5hCduAZFU/viewform)

This project adheres to the
[Open Source Code of Conduct](http://www.capitalone.io/codeofconduct/). By
participating, you are expected to honor this code.


## License

Copyright 2016 Capital One Services, LLC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and limitations under the License.
