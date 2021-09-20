# eHOST

This is an imported and polished version of [eHOST: The Extensible Human Oracle Suite of Tools an open source annotation tool](https://code.google.com/archive/p/ehost/).

Compiled jars can be downloaded from the [Releases](https://github.com/jianlins/ehost/releases)

A few highglighted updates: 
1. Rearrange the code under maven framework
2. Allows to delete annotation with single press "Delete" key
3. Allows hot key navigation among documents (Ctrl+PageUp, Ctrl+PageDown)
4. Sort projects on names
5. Sort files on names
6. Allows to export annotations to excel (Developed by [Chris Leng](https://github.com/chrisleng/ehost))
7. Sync the highlighter of file navigation panel when selecting file from other places (e.g. dropdown list)
8. Save last view file within each project, so that annotators can be easily resumed to that file when open a project next time.
9. Allows remote control through RESTful API.
10. Optimize GUI rendering. Previous version has several redundant refreshing and rendering.

******
To enable RESTful server for outside app controls:

Add following to the *eHOST.sys* file before running eHOST:
> [RESTFUL_SERVER]
>
> true

To check the server status:
http://127.0.0.1:8009/status

To shutdown the server from outside:
http://127.0.0.1:8009/shutdown

To navigate to a project:
http://127.0.0.1:8009/ehost/xxx
* xxx is the project directory name (not absolute path)

To navigate to a project and display a specific file:
http://127.0.0.1:8009/ehost/xxx/yyy
* xxx is the project directory name (not absolute path)
* yyy is the file name or partial file name (if multiple file matched, only first one will be displayed)

You can change the server port by editing the *application.properties* file.

Thanks for comments
