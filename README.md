# eHOST

This is an imported and polished version of [eHOST: The Extensible Human Oracle Suite of Tools an open source annotation tool](https://code.google.com/archive/p/ehost/).

A few highglighted updates: 
1. Rearrange the code under maven framework
2. Allows to delete annotation with single press "Delete" key
3. Sort projects on names
4. Sort files on names
5. Allows to export annotations to excel (Developed by [Chris Leng](https://github.com/chrisleng/ehost))
6. Sync the highlighter of file navigation panel when selecting file from other places (e.g. dropdown list)
7. Save last view file within each project, so that annotators can be easily resumed to that file when open a project next time.
8. Allows remote control through RESTful API.
9. Optimize GUI rendering. Previous version has several redundant refreshing and rendering.

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
