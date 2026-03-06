# eHOST RESTful Server Guide

This guide explains how eHOST's built-in RESTful server works and how to use it for multi-user deployments and IAA report navigation.

## Enabling the RESTful Server

Add the following to your `eHOST.sys` configuration file:

```
[RESTFUL_SERVER]
true
```

> **Note:** eHOST reads `eHOST.sys` from your configuration home directory (default: `~/.ehost/`). If you specified a custom config home via `-c=/path/to/config`, the file is read from there. Make sure you edit the correct `eHOST.sys` file.

## Port Configuration

The server port is configured in `application.properties`:

```properties
server.port=8010
server.address=127.0.0.1
```

- **Default port**: 8010
- If the configured port is in use, eHOST automatically scans for the next available port
- The selected port is updated in `application.properties` for the current session
- Port range: 8001–8020

When the server starts, you'll see the URL in the terminal:
```
--------------------------------------------------------------
eHOST RESTful Server is running at:
 - Local URL: http://localhost:8010
--------------------------------------------------------------
```

## REST API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/` or `/status` | GET | Server status page |
| `/ehost/{projectName}` | GET | Navigate eHOST to the named project |
| `/ehost/{projectName}/{fileName}` | GET | Navigate to a specific file in a project |
| `/reports/**` | GET | Serve IAA report files (see below) |
| `/shutdown` | GET | Shutdown the server |

### Examples

```
http://127.0.0.1:8010/status
http://127.0.0.1:8010/ehost/myproject
http://127.0.0.1:8010/ehost/myproject/document001
http://127.0.0.1:8010/reports/index.html
http://127.0.0.1:8010/shutdown
```

## Viewing IAA Reports via HTTP

### Why HTTP Instead of File://
When multiple users share the same eHOST installation (e.g., on a Windows Server with RDP), each user's eHOST runs on a different port. IAA reports contain navigation links that tell eHOST which file to open. By serving reports through eHOST's HTTP server, each user's links automatically route to their own eHOST instance — no port conflicts, no cross-user interference.

### Opening Reports

There are two ways to open reports in the **Reports** panel:

1. **"Open Existing Reports in Browser"** — Opens the current project's report (`{project}/reports/index.html`) via HTTP. The report is served at `http://127.0.0.1:{your_port}/reports/index.html`.

2. **"Open Report Folder..."** — Opens a file chooser dialog to select any report folder on disk. The selected folder is served via HTTP and opened in your browser. Use this when you need to view reports from a different location (e.g., a shared network drive or another project's output).

### How Navigation Links Work
When you click a file name in an IAA report (e.g., in the "Unmatched Details" section), the link calls `/ehost/{project}/{file}` on the same origin. Since each user opened the report through their own eHOST port, the link routes to their eHOST instance.

**Example flow:**
- User A's eHOST runs on port 8001 → opens report at `http://127.0.0.1:8001/reports/index.html` → links call `http://127.0.0.1:8001/ehost/...`
- User B's eHOST runs on port 8002 → opens the same report at `http://127.0.0.1:8002/reports/index.html` → links call `http://127.0.0.1:8002/ehost/...`

Both users view the same report content but each user's clicks control only their own eHOST.

## Multi-User Deployment

### Shared Installation Setup
For a Windows Server with multiple RDP users sharing one eHOST installation:

1. Install eHOST in a shared location (e.g., `C:\Apps\eHOST\`)
2. Each user gets their own config home (default: `C:\Users\{username}\.ehost\`)
3. Each user's `eHOST.sys` should have `[RESTFUL_SERVER]` set to `true`
4. Each user's `application.properties` can use the same default port — eHOST will auto-select an available one
5. Each user launches eHOST from their own session — it binds to a unique port automatically

### Sharing Reports Across Users
1. Generate IAA reports from any user's eHOST (reports are saved to the project directory)
2. Any user can open the same reports using either:
   - "Open Existing Reports in Browser" (if the project is loaded)
   - "Open Report Folder..." (to browse to the report directory)
3. Navigation links work for all users because they use relative paths

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Server not starting | Check that `[RESTFUL_SERVER]` is set to `true` in `~/.ehost/eHOST.sys` (not just the project-local copy) |
| "Can't reach this page" in browser | Verify eHOST is running and check the terminal for the actual port number |
| Port conflicts | eHOST auto-scans ports 8001–8020; if all are in use, increase the range or close other instances |
| Report links not working | Make sure you opened the report via eHOST's button (HTTP), not by double-clicking the HTML file |
| Wrong eHOST instance responds | Each user should open the report through their own eHOST instance |
| `localhost` vs `127.0.0.1` | eHOST binds to `127.0.0.1`; on some systems `localhost` may resolve to IPv6 (`::1`), so use `127.0.0.1` |
