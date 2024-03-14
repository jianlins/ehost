# eHOST

This is an imported and polished version of [eHOST: The Extensible Human Oracle Suite of Tools an open source annotation tool](https://code.google.com/archive/p/ehost/).

Compiled jars can be downloaded from the [Releases](https://github.com/jianlins/ehost/releases)

What's new in 1.32:
1. Fix comments not saved issue
2. Fix opening a new project, the main application window will automatically hide.
3. If not local configuration files (from where the command is executed) are not available, try to read from USER_HOME/.ehost/ folder. If still empty, set the default configuration files under USER_HOME/.ehost. So that multiple users can share a single copy of eHOST software without making multiple copies.
4. To customize the ehost configuration folder, use add --ehostconfighome=/path/to/config/folder. For example: 
```bash
java -jar eHOST-xxx.jar --ehostconfighome=/home/ehost_config/
#or
java -jar eHOST-xxx.jar -c /home/ehost_config/
```
5. Similarly, you can also use command argument to set workspace when open eHOST, using --workspace=/path/to/workspace. For example:
```bash
java -jar eHOST-xxx.jar --workspace=/home/ehost_workspace/
#or
java -jar eHOST-xxx.jar -w /home/ehost_workspace/
```
4. Add a project lock to prevent multiple users (or single user open mutliple instances of eHOST) from working on the same project at the same time. Because in that situation, the saving will be competition with each other and result in annotation lost.
5. In the unmatched report, file names are clickable---will navigate to the corresponding file in eHOST after click.

What's new in 1.3.1:

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
