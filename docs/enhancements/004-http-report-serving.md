# EHOST-004: HTTP Report Serving for Multi-User Support

## Summary
IAA reports are now served via eHOST's built-in HTTP server instead of being opened as local `file://` URIs. This enables multiple users sharing the same installation to each view shared reports through their own eHOST instance, with navigation links automatically routing to the correct user's eHOST.

## Problem
When multiple users share an eHOST installation on a Windows Server (e.g., via RDP):
- Each user's eHOST instance runs on a different port (8001–8020)
- IAA report HTML files previously embedded hardcoded `http://localhost:{PORT}/ehost/...` links with the port of the user who generated the report
- If two users shared the same port, clicking a link in one user's browser could control the other user's eHOST
- If users used different ports, shared report links only worked for the user whose port was embedded at generation time

## Solution

### 1. Serve Reports via HTTP
A new `ReportController` serves report files through eHOST's own HTTP server at `/reports/**`. When a user opens a report, eHOST sets its report directory and launches the browser at `http://127.0.0.1:{port}/reports/index.html`. Since the report is served from the user's own eHOST instance, all relative URLs in the report automatically resolve to that user's port.

### 2. Relative URLs in Reports
Report link generation now uses relative paths (`/ehost/{project}/{file}`) instead of absolute URLs with hardcoded ports (`http://localhost:8001/ehost/{project}/{file}`). When the browser resolves a relative URL, it uses `window.location.origin`, which is the user's own eHOST port.

### 3. Open Report Folder Button
A new "Open Report Folder..." button allows users to browse for any report directory on the file system (not just the current project's reports), serve it via HTTP, and open it in the browser.

## User-Facing Changes

### Reports Panel (4 buttons now)
| Button | Description |
|--------|-------------|
| Graph Reports of Position Indicators | Existing — position indicator reports |
| Annotator Performance | Existing — generates IAA reports |
| Open Existing Reports in Browser | **Changed** — now opens via HTTP instead of file:// |
| Open Report Folder... | **New** — browse for any report folder to open |

### How It Works for End Users
1. Click "Open Existing Reports in Browser" — the report opens in the browser via `http://127.0.0.1:{your_port}/reports/index.html`
2. Click any navigation link in the report — it routes to your eHOST instance automatically
3. Another user viewing the same report through their eHOST will have links route to their instance instead
4. Use "Open Report Folder..." to open reports from any location on disk

## Technical Details

### New File: `ReportController.java`
- `GET /reports/**` — serves files from a configurable base directory
- `setReportBaseDir(File dir)` — static method to change which directory is served
- Path traversal protection — validates resolved paths stay within the base directory
- MIME type handling for HTML, CSS, JS, images, and other common types
- No-cache headers to ensure folder changes take effect immediately
- URL decoding for paths with special characters

### Modified Files
- **`Manager.java`** — changed "Open Existing Reports" to serve via HTTP; added "Open Report Folder..." button with JFileChooser
- **`GenHtmlForNonMatches.java`** / **`GenHtmlForNonMatches2.java`** — changed `data-url` from absolute `http://localhost:{port}/ehost/...` to relative `/ehost/...`
- **`EhostController.java`** — help text now shows actual dynamic port instead of hardcoded values
- **`WebConfig.java`** — added `127.0.0.1` origins to CORS config alongside `localhost`

## Version
1.39b2 (Unreleased)
