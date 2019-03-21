## RESTFul Remote control
This function allows other apps interact with ehost through RESTFul API

By default, the host address is 127.0.0.1, and the port is 8009
You can use a different configuration by put a file named "application.properties" under the ehost's root directory.
An example configuration can be found at https://github.com/jianlins/ehost/blob/master/src/main/resources/application.properties

### Navigate to a project under current workspace
http://host:port/ehost/{projectName}

### Navigate to a specific file within a project under current workspace
http://host:port/ehost/{projectName}/{fileName}


### Navigate to a specific file within a project may or maynot under current workspace
Use POST to:
http://host:port/path/
with json object:
```json
{
	"projectpath":"{project absolute path}",
	"file":"{fileName}"
}
```
### Shutdown eHOST
http://host:port/shutdown

### Check eHOST current status
http://host:port/status