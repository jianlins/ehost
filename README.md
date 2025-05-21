# eHOST

This is an imported and polished version of [eHOST: The Extensible Human Oracle Suite of Tools an open source annotation tool](https://code.google.com/archive/p/ehost/).

The improvement made over original eHOST is summarized here: [ImprovementSummary.md](ImprovementSummary.md)

The change log can be found in [CHANGELOG.md](CHANGELOG.md)

### Support file navigation/search through browser
The original eHOST doesn't support file search on names, which can be cumbersome to use when adjudicating, where adjudicator 
often needs to locate the disagreed files,especially when file names are long. 

The RESTful server is a core component of eHOST's latest versions that allows for external application (e.g. browser) control:
1. **Automatic Startup**:
  - The RESTful server starts automatically if enabled in your configuration
  - Check the `Parameters.RESTFulServer` setting in your configuration

2. **Enable in eHOST.sys configuration file**:
  Set [RESTFUL_SERVER] to true in the eHOST.sys file. By default, the RESTful server will read and use the configuration of [application.properties](application.properties).

3. **Port Configuration**:
  - Default port: 8010
  - The server will automatically find an available port if the configured one is in use
  - Port configuration persists between sessions in the application.properties file

### Server Status Information
When the RESTful server starts, you will see information in:
- The terminal console window showing the server URL
- System logs recording server status and port information

Example terminal output:
``` 
--------------------------------------------------------------
eHOST RESTful Server is running at:
 - Local URL: http://localhost:8010
--------------------------------------------------------------
```
## Working with IAA Reports
### Generating IAA Reports
1. In the eHOST application, navigate to the "Reports" panel:
  - Click on "Annotator Performance," eHOST will generate reports in html format.

2. In the IAA Report dialog:
  - Select annotators to compare from the list
  - Choose annotation classes to include in the comparison
  - Set comparison parameters (span matching, class matching, attribute matching, etc.)
  - Click "Generate Report"
  - Once the report is generated, you can open the report in browser by either clicking the button "Open Existing Reports ni Browser" on the same panel, or clicking the left side of the status bar at the bottom of eHOST app window (displaying the project location).


### Viewing Generated Reports
After generation, HTML reports are saved to your project directory and displayed in the IAA viewer:
1. **Main Index**: The provides links to all report sections `index.html`
2. **Class and Span Matcher**: Shows agreement statistics for annotation spans and classes
3. **Detailed UnMatched**: Lists annotations that differ between annotators. In the unmatched summaries, if RESTful server is enabled, each mismatched file name will be added a hyperlink. These hyperlinks can be used to navigate to the corresponding files inside eHOST.

### Using HTML Reports for Adjudication
One of the most powerful features of the IAA reports is the ability to navigate directly to annotations from the reports:
1. **Direct Navigation**:
  - In the "UnMatched Details" report, file names are clickable
  - Clicking a file name opens that exact document in eHOST

2. **Report Structure**:
  - Reports are organized by annotator pairs
  - Each section shows specific disagreements between annotators
  - Color coding indicates different types of disagreements (span mismatches, class mismatches, etc.)

3. **Adjudication Workflow**:
  - Keep the HTML report open while reviewing annotations
  - Click on disagreements to navigate to them in eHOST
  - Make adjudication decisions in the main eHOST interface
  - Save changes and continue to the next disagreement

### Using eHOST from Multiple Locations
eHOST now supports user-specific configuration:
1. **Configuration Directory**:
  - Default: Current working directory or USER_HOME/.ehost/
  - Specify custom location: `--ehostconfighome=/path/to/config/folder` or simply use `-c=`

2. **Workspace Directory**:
  - Specify on startup: `--workspace=/path/to/workspace/` or simply use `-w=`
  - All projects are located within the workspace

3. **Project Locking**:
  - Prevents multiple users or instances from accessing the same project. This is helpful when project is stored on network drive, and users are accessing the project through difference machines.
  - Automatically locks projects when opened
  - Releases locks when projects are closed

## Troubleshooting
- **Navigation Issues**: If clicking a file in the report doesn't navigate to the correct location, ensure you're using the latest version of eHOST
- **Server Port Conflicts**: If the configured port is in use, eHOST will automatically try the next available port
- **Configuration Problems**: Check the log files in your configuration directory for error messages

******
### RESTful API for outside app controls:

Add following to the *eHOST.sys* file before running eHOST:
> [RESTFUL_SERVER]
>
> true

To check the server status:
http://127.0.0.1:8010/status

To shutdown the server from outside:
http://127.0.0.1:8010/shutdown

To navigate to a project:
http://127.0.0.1:8010/ehost/xxx
* xxx is the project directory name (not absolute path)

To navigate to a project and display a specific file:
http://127.0.0.1:8010/ehost/xxx/yyy
* xxx is the project directory name (not absolute path)
* yyy is the file name or partial file name (if multiple file matched, only first one will be displayed)

You can change the server port by editing the *application.properties* file.

Thanks for comments
