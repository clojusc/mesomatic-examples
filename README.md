# mesomatic-example

*A Clojure Port of the Mesos Java Example*

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

This example is a Clojure and [Mesomatic](https://github.com/pyr/mesomatic) port of the [Mesos Java example](https://github.com/apache/mesos/tree/master/src/examples/java).


## Dependencies [&#x219F;](#contents)

Mesomatic depends upon having the following installed and, where applicable, running on your machine (or cluster):

* Java
* Maven
* ``lein``
* The native Mesos library (matching the version used in this project).

The latest version of this example is built against Mesos 0.28.2 and mesomatic 0.28.0-r0. You may look at previous releases to see if there are older versions of Mesos supported (should you be interested).

All other dependencies (including the Java bindings) are downloaded automatically by ``lein`` when you run the example.


## Usage [&#x219F;](#contents)

You'll need to have mesos running in order to see this example in action. If you've built your own Mesos, you can switch to the build directory, and after running ``make check`` (which run tests and generates necessary files/dirs for you), you can start up a local mesos:

```bash
$ ./bin/mesos-local.sh
```

Then, from the ``mesomatic-example`` clone directory:

```bash
$ lein mesomatic 127.0.0.1:5050 framework
```

This example project needs access to the native Mesos library. If you get errors like the following

* ``Failed to load native Mesos library``
* ``java.lang.UnsatisfiedLinkError: no mesos in java.library.path``

then one of the techniques you may employ to address this is setting the ``MESOS_NATIVE_JAVA_LIBRARY`` environment variable in your OS shell. For example, if you have built Mesos in ``/opt/mesos/0.28.2``

```bash
$ export MESOS_NATIVE_JAVA_LIBRARY=/opt/mesos/0.28.2/build/src/.libs/libmesos.so
```

At which point you may re-try the usage example above.


## Documentation [&#x219F;](#contents)

The project's auto-generated documentation (such that it is) is available here:

* [http://clojusc.github.io/mesomatic-example/](http://clojusc.github.io/mesomatic-example/)


## License [&#x219F;](#contents)

Copyright © 2016 Duncan McGreggor

Apache License, Version 2.0.
