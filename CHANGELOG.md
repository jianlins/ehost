## Version 1.39b5 (2026-03-23):

### Enhanced
- **Adjudication Restart Warning**: Added confirmation warning dialog when starting a new adjudication to prevent accidental loss of previous adjudication work ([docs/enhancements/009-adjudication-restart-warning.md](docs/enhancements/009-adjudication-restart-warning.md))

## Version 1.39b4 (2026-03-22):

### Enhanced
- **Adjudication XML Optimization**: Final optimization with backward compatibility fixes ([docs/enhancements/008-adjudication-xml-optimization.md](docs/enhancements/008-adjudication-xml-optimization.md))

## Version 1.39b3 (2026-03-21):

### Enhanced
- **Adjudication Detection Simplified**: Removed XML metadata and streamlined detection logic ([docs/enhancements/007-simplify-adjudication-detection.md](docs/enhancements/007-simplify-adjudication-detection.md))
- **Save Prompts on Mode Switch**: Added prompts to prevent data loss when switching modes ([docs/enhancements/006-save-prompt-on-mode-switch.md](docs/enhancements/006-save-prompt-on-mode-switch.md), [docs/bugs/EHOST-004-no-save-prompt-on-mode-switch.md](docs/bugs/EHOST-004-no-save-prompt-on-mode-switch.md))
- **Adjudication Resume Robustness**: Improved state persistence and resume detection ([docs/enhancements/005-adjudication-resume-robustness.md](docs/enhancements/005-adjudication-resume-robustness.md), [docs/bugs/EHOST-003-adjudication-resume-failure.md](docs/bugs/EHOST-003-adjudication-resume-failure.md))
- **Report Serving Improvements**: Debug logging, URL decoding, and no-cache headers ([docs/enhancements/004-http-report-serving.md](docs/enhancements/004-http-report-serving.md))

## Version 1.39b2 (2026-03-06):

### Enhanced
- **HTTP Report Serving for Multi-User Support**: IAA reports served via HTTP instead of file:// URIs ([docs/enhancements/004-http-report-serving.md](docs/enhancements/004-http-report-serving.md))
- **IAA Adjudication Comparison**: Compare annotators against adjudicated gold standard ([docs/enhancements/003-iaa-adjudication-comparison.md](docs/enhancements/003-iaa-adjudication-comparison.md))
- **IAA Report HTML Output**: Improved attribute display in unmatched/matched reports ([docs/enhancements/002-iaa-report-attribute-display.md](docs/enhancements/002-iaa-report-attribute-display.md))

### Fixed
- **Missing annotator info on class change**: Fixed annotator name/ID not set when changing annotation class ([docs/bugs/EHOST-002-missing-annotator-info-on-class-change.md](docs/bugs/EHOST-002-missing-annotator-info-on-class-change.md))
- **Duplicate adjudication elements bug**: Fixed annotations saved twice in adjudication mode ([docs/bugs/EHOST-001-duplicate-adjudication-elements.md](docs/bugs/EHOST-001-duplicate-adjudication-elements.md))

## Version 1.39b1 (2026-03-04):

### Enhanced
- **IAA Adjudication Comparison**: Compare annotators against adjudicated gold standard ([docs/enhancements/003-iaa-adjudication-comparison.md](docs/enhancements/003-iaa-adjudication-comparison.md))
- **IAA Report HTML Output**: Improved attribute display in unmatched/matched reports ([docs/enhancements/002-iaa-report-attribute-display.md](docs/enhancements/002-iaa-report-attribute-display.md))

### Fixed
- **Missing annotator info on class change**: Fixed annotator name/ID not set when changing annotation class ([docs/bugs/EHOST-002-missing-annotator-info-on-class-change.md](docs/bugs/EHOST-002-missing-annotator-info-on-class-change.md))
- **Duplicate adjudication elements bug**: Fixed annotations saved twice in adjudication mode ([docs/bugs/EHOST-001-duplicate-adjudication-elements.md](docs/bugs/EHOST-001-duplicate-adjudication-elements.md))

## Version 1.38:
- **Improved Configuration Management**:
    - Comprehensive refactoring of `PropertiesUtil` class to centralize configuration handling
    - Added caching mechanism for configuration properties to improve performance
    - Implemented dynamic loading of configuration from multiple sources with proper fallbacks
    - Added functionality to update configuration parameters at runtime

- **Enhanced RESTful Server Capabilities**:
    - Added intelligent port selection with automatic retry on port conflicts
    - Implemented configuration updates when port changes occur
    - Improved server startup with better error handling and logging
    - Added detailed server status information in terminal output

- **Optimized Application Initialization**:
    - Implemented a structured initialization sequence with progress feedback
    - Created class to handle the startup process `InitializationManager`
    - Added visual progress indication during startup with customizable durations
    - Separated configuration, settings, and GUI loading for better organization

## Version 1.37
- **Improved Multithreaded Support**:
    - Refactored application to better handle concurrent operations
    - Added heartbeat control mechanism for monitoring system health
    - Implemented thread-safe operations for critical components

- **GUI Architecture Improvements**:
    - Separated rendering and navigation functions into distinct classes
    - Reduced GUI component size and complexity for better maintainability
    - Fixed threading issues in UI loading mechanism
    - Improved interaction between RESTful server and GUI components

## Version 1.36
- **Splash Screen and Startup Improvements**:
    - Reimplemented splash screen with more reliable display mechanism
    - Added dynamic version information display from Maven project properties
    - Created smooth progress animation during startup
    - Added detailed status messages during initialization

- **RESTful API Enhancements**:
    - Added comprehensive URL information in the terminal
    - Improved status page with additional user instructions
    - Created clickable URL integration on the main GUI status panel
    - Enhanced CORS configuration for better web client compatibility

## Version 1.35
- **Web Integration Improvements**:
    - Fixed CORS issues when loading AJAX responses from local HTML reports
    - Implemented dynamic version information loading from version.properties
    - Created synchronized version management with Maven project properties

## Version 1.34
- **IAA (Inter-Annotator Agreement) Report Improvements**:
    - Enhanced the analysis process for better performance
    - Implemented more accurate comparison of annotations between annotators
    - Added memory optimization for processing large annotation sets

## Version 1.33
- **Configuration Management Enhancements**:
    - Implemented standardized configuration loading mechanism
    - Added support for user-specific configuration directories
    - Created default configuration file creation with sensible defaults


## Version 1.32:
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
6. Add a project lock to prevent multiple users (or single user open mutliple instances of eHOST) from working on the same project at the same time. Because in that situation, the saving will be competition with each other and result in annotation lost.
7. In the unmatched report, file names are clickable---will navigate to the corresponding file in eHOST after click.

## Version in 1.3.1:

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
