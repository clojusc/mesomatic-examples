# mesomatic-hello

*A 'Hello, World' for Clojure and Mesos using Mesomatic*

[![][mesos-logo]][mesos-logo-large]

[mesos-logo]: resources/images/Apache-Mesos-logo-x250.png
[mesos-logo-large]: resources/images/Apache-Mesos-logo-x1000.png


#### Contents

* [About](#about-)
* [Dependencies](#dependencies-)
* [Usage](#usage-)
* [Documentation](#documentation-)
* [License](#license-)


## About [&#x219F;](#contents)

This example project demonstrates a minimal "Hello, World" Mesos framework for Clojure using the [Mesomatic library](https://github.com/pyr/mesomatic). This example covers only *very basic* usage of Mesos.

Other places to go for learning more about how to use [Mesomatic](https://github.com/pyr/mesomatic):

* An [example project](https://github.com/oubiwann/mesomatic-example) that follows in the tradition of the [Java](https://github.com/apache/mesos/tree/master/src/examples/java), and [Python](https://github.com/apache/mesos/tree/master/src/examples/python) Mesos examples
* A [Docker-based example project](https://github.com/oubiwann/mesomatic-example-docker) which demonstrates running the Mesomatic port of the Mesos example in a Docker container
* [Bundes](https://github.com/pyr/bundes/) - an actual, full Mesomatic application - this is the canonical example of Mesosmatic and one of the significant drivers for its development


## Dependencies [&#x219F;](#contents)

Mesomatic depends upon having the following installed and, where applicable, running on your machine (or cluster):

* Java
* Maven
* ``lein``
* The native Mesos library (matching the version used in this project).

The latest version of this example is built against Mesos 0.27.0 and mesomatic 0.27.0-r0. You may look at previous releases to see if there are older versions of Mesos supported (should you be interested).

All other dependencies (including the Java bindings) are downloaded automatically by ``lein`` when you run the example.


## Usage [&#x219F;](#contents)

```bash
$ lein mesomatic 127.0.0.1:5050 framework
```

This example project needs access to the native Mesos library. If you get errors like the following

* ``Failed to load native Mesos library``
* ``java.lang.UnsatisfiedLinkError: no mesos in java.library.path``

then one of the techniques you may employ to address this is setting the ``MESOS_NATIVE_JAVA_LIBRARY`` environment variable in your OS shell. For example, if you have built Mesos in ``/opt/mesos/0.27.0``

```bash
$ export MESOS_NATIVE_JAVA_LIBRARY=/opt/mesos/0.27.0/build/src/.libs/libmesos.so
```

At which point you may re-try the usage example above.


## Documentation [&#x219F;](#contents)

The project's auto-generated documentation is available here (such that it is):

* [http://clojusc.github.io/mesomatic-hello/](http://clojusc.github.io/mesomatic-hello/)


## License [&#x219F;](#contents)

Copyright © 2016 Duncan McGreggor

Apache License, Version 2.0.
