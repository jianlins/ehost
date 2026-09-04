# Sync Assignments — What It Is, Why It Is Hidden, and What To Do With It

**Date**: 2026-09-04
**Tag**: 1.39
**Status**: 🟡 Step 1 implemented — the toolbar button is hidden unless explicitly enabled; steps 2–3 open
**Scope**: the `Sync Assignments` toolbar button, [`webservices/`](../../src/main/java/webservices), and
`lib/annotationadmin-integration-1.0.13.jar`

---

## 1. What the button was supposed to do

`Sync Assignments` is the eHOST client side of **VA VINCI Annotation Admin** — a server-side web
application used inside the VA around 2011–2012 to hand annotation work out to annotators. eHOST's
own header still records this: *"Annotation Admin Team added the Sync functions, 2011-2012"*
([`main/eHOST.java`](../../src/main/java/main/eHOST.java)).

The intended round trip was:

1. A study coordinator creates **assignments** in Annotation Admin. An assignment is a bundle of
   *analytes* (documents, or patient records) plus the annotation **schema** to use.
2. The annotator clicks **Sync Assignments** in eHOST, types their Annotation Admin username and
   password, and clicks **Sync**.
3. eHOST authenticates, uploads any annotations it already has, then downloads the assignments. Each
   assignment is materialised as **a normal eHOST project** in the current workspace:

   ```
   <workspace>/<userId>_<assignmentName>/
       config/schema.xml     <- schema pushed by the server
       corpus/*.txt          <- analyte documents pushed by the server
       saved/*.knowtator.xml <- the annotator's work, uploaded on the next sync
       adjudication/         <- optional, if the project was adjudicated
   ```

4. The three tables on the screen list the annotator's analytes by state — **In Progress**, **Done**,
   **On Hold**. Double-clicking a row opens that document in the Result Editor; dragging a row between
   tables changes its status, which is pushed back on the next sync.
5. A local catalog file, `annotationadmin.xml`, is written in the workspace root to remember the server
   address, the user, and the timestamp of the last sync.

So: it is **not** a file-sync or cloud-backup feature. It is a work-assignment client for one specific
VA server product.

### Where the code lives

| Piece | Location |
|---|---|
| Toolbar button `jToggle_AssignmentsScreen` | [`userInterface/GUI.java`](../../src/main/java/userInterface/GUI.java) (created ~line 326, configured ~line 918) |
| Click handler → tab switch | `jToggleButton_AssignmentScreenActionPerformed` → `tabDoorman(TabGuard.tabs.assignmentsScreen)` → `enterTab_assignmentsScreen()` → `getAssignmentsScreen()` |
| The screen itself | [`webservices/AssignmentsScreen.java`](../../src/main/java/webservices/AssignmentsScreen.java) |
| Table models / drag-drop | [`webservices/view/`](../../src/main/java/webservices/view) |
| Login prompt reused for sync | [`resultEditor/annotator/ChangeAnnotator.java`](../../src/main/java/resultEditor/annotator/ChangeAnnotator.java) (calls `assignmentScreen.sync()` after credentials are entered) |
| Protocol, models, Knowtator translation | `lib/annotationadmin-integration-1.0.13.jar` (`gov.va.vinci.annotationAdmin.integration.*`, installed into the local Maven repo by `script/mvn_install_jar.*`) |

---

## 2. Why it does not work today

The whole `webservices/` package is unchanged since the initial import of this fork (commit `4d06899`,
2018). It has no tests, no wiki page, and the following concrete defects:

| # | Problem | Evidence |
|---|---|---|
| 1 | **No reachable server.** The server drop-down is hard-coded to a single entry, `localhost`, default port `8080`. Annotation Admin is VA-internal and is not distributed with eHOST, so out of the box the sync can only ever fail with a connection error. | `String [] options = {"localhost" };` in `AssignmentsScreen.init()` |
| 2 | **The adjudication question is ignored.** When the project has an `adjudication/` folder, eHOST asks *"Submit adjudications (adjudicated annotations)?"* — then calls the same one-argument `doSync(true)` in **both** branches, which hard-codes `isSubmittingAdjudications = false`. The two-argument overload that actually honours the answer is never called, so adjudicated annotations are never uploaded. | `AssignmentsScreen.sync()`; `AnnotationAdminComMgr.doSync(Boolean)` vs `doSync(Boolean, boolean)` |
| 3 | **The user identity plumbing is half-wired.** `setUserId(String id, String username)` silently drops `username`, and `getAssignmentsScreen("")` always passes an empty id, so the local catalog is read for the empty user until a login happens. | `AssignmentsScreen.setUserId`, `GUI.getAssignmentsScreen` |
| 4 | **Pre-annotation fetch would crash.** `sync()` always calls `setFetchPreAnnotations(true)`. The library's FLAP path needs `gov.va.vinci.flap.Client` and `gov.va.vinci.nlp.framework.pipeline.listeners.KnowtatorListener`; neither is bundled in the jar nor declared in `pom.xml`, so that path would throw `NoClassDefFoundError`. It is only avoided because eHOST never sets an NLP service name. | jar contains `AnnotationAdminComMgr$FLAPClientWorker` but no `gov/va/vinci/flap/**`; no FLAP dependency in `pom.xml` |
| 5 | **Insecure transport.** Login is `http://` only, and the credentials are placed in the **query string** (`?userName=…&password=<unsalted SHA-256>`). Nothing supports HTTPS or a proxy. | `AuthenticationManager.authenticateUserWithAnnotationAdmin` |
| 6 | **Side effect without any user action.** Constructing the screen creates `annotationadmin.xml` in the user's workspace root — and `NavigationManager.selectProject()` constructed it on *every* project open, discarding the result, so the file appeared even for users who never touched the button. | `Repository.getCatalog()`, `NavigationManager.selectProject()` |
| 7 | **JDK-8-only dependency.** The library imports `com.sun.corba.se.spi.activation.Server`, removed in JDK 11+. Any future JDK upgrade has to deal with this jar. | source shipped inside the jar |
| 8 | **Visible leftovers of unfinished work.** The username field is commented out, an earlier `SyncManager` is commented out, and a table listener that would gate the Sync button is commented out. | `AssignmentsScreen` |

**Conclusion:** the feature is not merely unconfigured, it is an unfinished integration with a server
that users of this fork do not have. For everyone outside the original VA deployment the button is a
dead end that can silently write files into the workspace.

---

## 3. Decision

**Hide it by default, keep it behind an explicit opt-in, and do not delete it yet.**

Deleting it now would also delete the only working example of eHOST's project-import-from-server flow,
and would drop the jar that contains the Knowtator ⇄ Annotation Admin translation. Hiding costs almost
nothing and is reversible in one checkbox.

### Step 1 — implemented

* New `eHOST.sys` parameter **`[SYNC_ASSIGNMENTS]`**, default `false`
  (read in [`config/system/ParameterGather.java`](../../src/main/java/config/system/ParameterGather.java),
  written in [`config/system/SaveConf.java`](../../src/main/java/config/system/SaveConf.java),
  held in `env.Parameters.SyncAssignments`).
* `GUI.enableFunctionsByMask()` shows the `Sync Assignments` toolbar button only when that flag is true.
  `ContentRenderer.setComponentsVisibleForConsensusMode()`, which used to re-show the button whenever the
  review mode changed, now honours the flag too.
* `NavigationManager.selectProject()` used to construct the Annotation Admin screen — and therefore write
  `annotationadmin.xml` into the workspace — every time a project was opened, even though the result was
  discarded. That call is now made only when the feature is enabled. With the default configuration none
  of the `webservices/` code is constructed and no `annotationadmin.xml` appears.
* **System Config → Feature Visibility → Server** gained a checkbox,
  *"Show Sync Assignments (legacy VA Annotation Admin, unsupported)"*, so a VA site that still runs an
  Annotation Admin server can re-enable the button without editing files or rebuilding.
* No behaviour of the sync itself was changed; defect #2 above is marked with a comment in the source
  pointing at this document, because it cannot be verified without a live server.

### Step 2 — decide the long-term disposition (open)

Pick one, ideally after asking on the issue tracker whether *anyone* still has an Annotation Admin
server:

| Option | What it means | When to choose it |
|---|---|---|
| **2a. Remove** | Delete `webservices/`, the toolbar button, `AssignmentsScreen` wiring in `GUI`/`ChangeAnnotator`, the `gov.va.vinci:annotation-admin` dependency, the jar, and its `script/mvn_install_jar.*` lines. Roughly −1,500 LOC, one fewer JDK-8-only blocker. | No user reports needing it — the likely outcome. |
| **2b. Revive** | Fix defects #1–#6, add HTTPS, add a configurable server list, add integration tests against a stub server. Only worthwhile with a real server to test against. | A VA site confirms it still uses Annotation Admin. |
| **2c. Replace** | Reimplement "fetch my assignments" against eHOST's own [RESTful server](../RESTful-Server-Guide.md), which already serves projects and documents, and drop the VA-specific protocol. | Multi-annotator coordination is wanted as a general eHOST feature. |

Suggested trigger: if no one asks for it within two release cycles, do **2a**.

### Step 3 — if 2a is chosen (open)

1. Remove the toolbar button, `enterTab_assignmentsScreen`, `getAssignmentsScreen`, `assignmentsScreen`
   field, and the `TabGuard.tabs.assignmentsScreen` enum entry.
2. Remove the `AssignmentsScreen` branch from `ChangeAnnotator` (it must keep working as the plain
   "change annotator" dialog — that is its main job).
3. Delete `src/main/java/webservices/**` and `src/main/java/env/…` references, the
   `gov.va.vinci:annotation-admin` dependency, `lib/annotationadmin-integration-1.0.13.jar`, and the
   matching lines in `script/mvn_install_jar.sh` / `.bat`.
4. Keep `[SYNC_ASSIGNMENTS]` parsing for one release so old `eHOST.sys` files do not warn, then drop it.
5. `mvn clean package` and confirm the toolbar still lays out correctly.

---

## 4. How to turn it back on

For a site that does have an Annotation Admin server:

1. **System Config → Feature Visibility → Server → Show Sync Assignments**, then **Save**; or
2. edit `eHOST.sys` in the configuration home (`-c=` or `USER_HOME/.ehost/`):

   ```
   [SYNC_ASSIGNMENTS]
   true
   ```

Then restart or reopen System Config — the toolbar refreshes on save. In the Sync Assignments screen,
type `host:port` into the editable **Server** box (the drop-down only pre-fills `localhost`), and expect
the limitations listed in §2 to still apply.
