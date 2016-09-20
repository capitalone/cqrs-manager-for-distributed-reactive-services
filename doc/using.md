# Data Contract

CQRS Manager for Distributed Reactive Systems (herein abbreviated
CMDR) is one component in a distributed architecture that coordinates
via a log (currenly Kafka).  There are two primary topics for users of
CMDR to be concerned with.

## Commands Topic

The name of the commands topic is configured in the CMDR processes as
`COMMANDS_TOPIC`.

Only CMDR shall write to the commands topic.

Only one command processor should handle each command type.  The
command processor is responsible to receive each command, determine
whether the command is semantically valid (CMDR will only write
syntactically valid commands to this topic), optionally write out to a
data store, and then emit one or more events to the events topic.

### Command Schema

TODO: schema management

A Command is provided to the CMDR HTTP API as a map in the following
form (in a variety of acceptable media types, but the semantics are
the same):

``` edn
{:action :create-foo
 :data   {:a "map data payload"}}
```

CMDR adds a UUID `id` field, so that when the command processor
receives it, its Kafka key is the UUID, and the Kafka value is a
[Fressian](https://github.com/Datomic/fressian) map (serialization
format subject to change in future) in the following format:

``` edn
{:id     #uuid "ab675ea0-7f6d-11e6-bcdd-540e9df06c05"
 :action :create-foo
 :data   {:a "map data payload"}}
```

### Providing External Synchrony

TODO: completing commands, and `?sync=true`

## Events Topic

The name of the commands topic is configured in the CMDR processes as
`EVENTS_TOPIC`.

CMDR doesn't write to the events topic, only application processes do.

### Event Schema

When an command or event processor emits, or when an event processor
receives, an event, it has a UUID Kafka key, and the Kafka value is a
[Fressian](https://github.com/Datomic/fressian) map (serialization
format subject to change in future) in the following format:

``` edn
{:id     #uuid "075415a0-7f6e-11e6-bcdd-540e9df06c05"
 :parent #uuid "ab675ea0-7f6d-11e6-bcdd-540e9df06c05"
 :action :foo-created
 :data   {:a "map data payload"
          :likely "with more keys generated upstream"}}
```

## Command Schema Management

TODO
