# ![Groovity](groovity-logo.png) Dynamic JVM web scripting

## Foundations: Open Source and Standards

Groovity is a scripting language and runtime built upon a mature open-source foundation including Java, Groovy and Maven.  It can be used to develop everything from simple utility scripts to modular Servlet 3.0 applications.  It has full support for web standards such as HTTP/REST, WebSockets, HTTP Signature, Digest and Basic authentication, CORS, URL patterns and content negotiation.  It features a fully modern asynchronous computing framework including asynchronous communication channels and a deadlock-free recursive fork-join implementation.  

## Development: Rich features, Rapid cycles

Groovity's mission is to empower the developer.  Compilation can be performed at runtime and each script has an isolated classloader and lifecycle supporting hot-swap. Even complex applications can be developed with just seconds between saving a source code file and testing the full application - no restarts required.  Groovity features a rich inline template syntax for full-featured JSP style templates with the ability to define custom global tags or functions.  Unit tests can be developed natively in Groovity and perform real HTTP requests against the application. Compiled scripts can be packaged into modules and composed into standard java web applications.

Developers can leverage built-in functions for basic things like logging, caching, REST and WebSocket calls. A variety of additional modules are available for groovity to simplify things such as cryptography and mapping data from relational or document stores to domain class models.  A sample application and modular portal framework are provided to demonstrate these capabilities.

## Enterprise: Flexible Integration

Groovity is designed to integrate easily with enterprise systems. There are Java plugin APIs to set default global variables, integrate with different sources of configuration data, and even custom ways to locate script source code.  Groovity could run as an appliance in AWS and load its configuration and scripts dynamically from S3!  Full maven lifecycle support including compilation, testing and packaging makes it easy to build and distribute re-usable groovity components and leverage automated build pipelines for continuous integration and delivery of applications.  It also has a notion of security policies that can be used to integrate with different sources of authentication and authorization.

## Operations: Heavy-duty Instrumented Flyweight

Groovity scripts take the flyweight pattern as far as possible. Many aspects of the script are pre-processed after compilation to minimize the amount of memory and CPU needed each time a script instance is created and invoked.  This allows a groovity application to achieve very high performance for handling serious load.  Every script is automatically instrumented to gather real-world performance data, and a web-based management module is provided to visualize performance data, check thread health, and more.  Groovity is designed to run in a cluster formation, and the management tool can aggregate performance and health data from the whole cluster in a single screen.

## Evolution: Strategic stability

Groovity uses Semantic Versioning for releases, which gives you predictability of upgrades and clarity as to when breaking changes are introduced.  Each stable Groovity release has a three-part number, **MAJOR.MINOR.PATCH**, e.g. `2.3.4`.  An increment to each of these levels communicates the following level of change:

* **MAJOR**: may contain breaking changes to the compiler, core tags, APIs and dependencies
* **MINOR**: may contain new features and updated dependencies, backwards compatible with previous releases in the same MAJOR series
* **PATCH**: may contain bug fixes and updated dependencies, backwards compatible with previous releases in the same MINOR series

Groovity also publishes pre-release versions for the purpose of testing, validating and gathering feedback on changes.  These versions will be identified with the three-part-number of the stable release they are intended to become, followed by a pre-release version indicator that communicates how close to the finish line the development team believes the pre-release to be.

* **alpha**: release is experimental and subject to significant changes
* **beta**:  release is usable but has rough edges, gaps or known issues
* **rc**:    release is relatively stable and ready for verification/acceptance

So a series of pre-releases leading up to a stable release might look like:

* 2.1.0-alpha.1
* 2.1.0-alpha.2
* 2.1.0-alpha.3
* 2.1.0-beta.1
* 2.1.0-beta.2
* 2.1.0-rc.1
* 2.1.0

### [Get Started](https://github.com/disney/groovity/wiki/Getting-Started)
