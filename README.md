# eHOST

This is an imported and polished version of [eHOST: The Extensible Human Oracle Suite of Tools, an open source annotation tool](https://code.google.com/archive/p/ehost/).

**📖 Full user documentation is published at [jianlins.github.io/ehost](https://jianlins.github.io/ehost/)** — installation, annotation, IAA reports, adjudication, pre-annotation and the RESTful API. The source of that site lives in [docs/](docs/).

* Improvements over the original eHOST: [ImprovementSummary.md](ImprovementSummary.md)
* Change log: [CHANGELOG.md](CHANGELOG.md)
* Releases: [github.com/jianlins/ehost/releases](https://github.com/jianlins/ehost/releases)

## Quick start

Requires Java 8 or newer.

```bash
java -jar eHOST.jar
```

Optionally point eHOST at a workspace and/or a configuration folder:

```bash
java -jar eHOST.jar -w /path/to/workspace -c /path/to/config
```

If `-c` is omitted, eHOST reads `eHOST.sys` and `application.properties` from `USER_HOME/.ehost/`,
creating them from the bundled defaults on first run. See
[1.2 Launching eHOST](https://jianlins.github.io/ehost/wiki/1.2_Launching_eHost.html).

## Building from source

The build targets Java 8 and **must** be run with a JDK 8 — `umls/GetCUI.java` uses `javax.xml.ws`,
which was removed from the JDK in 11. Maven picks its JDK from `JAVA_HOME`, so check `mvn -version`
rather than `java -version`.

```bash
bash script/mvn_install_jar.sh   # script\mvn_install_jar.bat on Windows — installs the custom jars in lib/
mvn clean package                # produces target/deploy/eHOST.jar
```

## RESTful server

eHOST ships with a small built-in web server. It powers navigation from IAA reports back into the
application, serves the reports themselves over HTTP so that several users can share one installation,
and lets external tools drive eHOST.

Enable it from the **System Config** toolbar button, or by adding this to `eHOST.sys`:

```
[RESTFUL_SERVER]
true
```

Common endpoints (default port 8010):

| URL | Purpose |
|---|---|
| `http://127.0.0.1:8010/status` | Server status page |
| `http://127.0.0.1:8010/ehost/{project}` | Open a project |
| `http://127.0.0.1:8010/ehost/{project}/{file}` | Open a file in a project |
| `http://127.0.0.1:8010/reports/index.html` | View the currently served IAA report |
| `http://127.0.0.1:8010/shutdown` | Shut down eHOST |

> Full details — port selection, CORS, multi-user deployment and troubleshooting — are in the
> [RESTful Server Guide](https://jianlins.github.io/ehost/RESTful-Server-Guide.html)
> ([source](docs/RESTful-Server-Guide.md)).

## Contributing

Issues and pull requests are welcome. Documentation changes go in [docs/](docs/); the site is built by
GitHub Pages with Jekyll, so every page is Markdown.
