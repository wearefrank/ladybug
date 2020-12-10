Ladybug Test Tool release notes
===============================



Upcoming
--------

- Make default update behaviour overwrite on XmlStorage
- Fix unaccepted filename characters on XmlStorage
- Remove wss4j
- Remove dom4j
- Execute stubable code when report generator not enabled
- Improve some log statements to print stracktrace
- Show label when message is null or empty string
- Update truncated and encoding label in GUI also when switching to checkpoint with null value
- Upgrade dom4j from 1.6.1 to 2.1.3
- Add easier way to integrate with Ladybug using subable code, matching stub strategies, default stub strategy "Stub all external connection code" and rerunner interface
- Add StatusExtractor
- Show encoding (object to string conversion) applied on message
- Encode/decode bean messages to/from xml (with XMLEncoder and XMLDecoder) 
- Encode/decode byte array messages to/from base64
- Add Null="true" to report xml for null messages
- Fix numbers in checkpoint path, not larger than 0 in some cases casing the wrong counterpart checkpoint to be selected in compare
- Display correct checkpoint path (from Checkpoint instead of TreePath)
- Exclude xml-apis (part of JDK nowadays, prevent conflicts)
- Rename internal objects from run to test and from log to debug
- Fix report filter for positive matching (report almost empty)
- Add tool tip to report filter text field
- Only log once when max checkpoints or max memory usage is exceeded
- Make free space minimum configurable
- Fix file system free space check using different limit then warning
- Check file system free space (show warning and don't store report to prevent corrupt storage files)
- Clone transformation and variableCsv too
- Add XmlStorage implementation
- Display number of stubbed checkpoints in Test tab
- Fix error on run in Test tab not being displayed
- Simplify syntax of cross-report variables
- Add toggle options for showing storage and checkpoint IDs in the UI
- Add timestamp to filename when downloading zipped reports



2.0.14
---

- Fix bug where pressing Replace on a report would cause an application error
- Add a text field to the Clone window for editing the input message to clone
- Make all of the Edit window's UI elements fit in one screen



2.0.13
---

- Add functionality to declare and use ${variables} for reports
- Add functionality to generate clone reports based on a CSV-table of variables
- Add functionality to refer to data in another report's checkpoints
- Clarify delete message (delete selected reports? -> delete X selected reports?)
- Clarify truncate message (X characters remaining -> X characters removed)



2.0.11
---

- Increase width of name/path textfields when editing a report
- Fix checkpoint showing wrong truncate message
- Make checkpoint truncating happen while generating the report, instead of afterwards
- Prevent checkpoints from being created when the report occupies too much memory
- Make compare tabs equally wide



2.0.10
---

- Remove single-report delete button (button to delete selected reports remains)



2.0.9
---

- Add refresh after test tab delete action (#2)
- Add configurable checkpoint message truncating
- Avoid IndexOutOfBoundException when starting an unknown thread (#5)



2.0.8
---

- Make ReportRunner public (make it callable for LadybugPipe)
- Bugfix broken searching/filtering on EndTime
- Replace Download all with Download table and Download tree
- Add Open all
- Move error label from above to below buttons in debug tab



2.0.7
---

- Make it possible to specify a transformation per report
- Fix selecting root node on refresh at some parts of tree of Test tab
- Return to previous active tab after closing tab
- Show Compare button after run in Test tab
- Display run result error (if any) on run in Test tab
- Fix error on selecting checkpoint with null message
- Fix error on selecting different stub strategy



2.0.6
---

- Prevent error on reselect node after Delete and Replace
- Refresh after upload
- Limit the use of special chars in normalized path
- Normalize path on save in report component
- Don't show null in path label
- Make report xml read-only in Edit mode
- Show line numbers on report description too
- Don't use TextArea for description in Read-only mode
- Don't log all error messages to log file
- Fix ClassCastException in Test tab for reports with description
- Prevent losing typed data in edit mode on close or select node in tree
- Copy report name from original report on Replace



2.0.5
---

- Refactor code for errorLabel, okayLabel and getReport
- Fix NPE on Open report (from Test tab), Edit, Save 
- Show reports in child folders too in Test tab
- Run reports in Test tab in background
- Add (de)select all to Test tab
- Make it possible to search case sensitive
- Add ProgressBar to Test tab



2.0.4
---

- Fix text color and errors with actions after Run, Replace in Test tab
- Improve Open after Run (select report and don't compare and sort all reports in Compare tab)
- Refactor TreePane in Test tab
- Upgrade Xerces to latest version without ElementTraversal



2.0.3
---

...