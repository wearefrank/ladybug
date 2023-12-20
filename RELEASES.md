Ladybug Test Tool release notes
===============================



Upcoming (2.3)
--------------

- Add Liquibase xml files (remove experimental version from test jar)
- Add alternative destination to proxy storage
- Implement database test storage
- Optionally store report xml in database
- Fix performance impact of max memory usage check
- Add maxStorageSize to DatabaseStorage
- Add HideMessageTransformer and LinkingMessageTransformer
- Add run result info to Debug tab
- Improve run result info
- Fix IndexOutOfBoundsException when max checkpoints or memory usage reached and a thread is closed
- Fix new GUI not working on WebSphere
- Add CustomReportAction
- Fix open report on sorted table
- Fix removeThreadCreatepoint() causing incorrect order of checkpoints and IndexOutOfBoundsException
- Log thread info in CloseReportsTask
- Prevent IndexOutOfBoundsException and add warning to report
- Add CONVERSATION ID to Proof of migration view
- Don't close new threads while other threads are active
- Disable javax.inject.Inject (change pom.xml to enable)
- Improve (database) storage search
- Improve proof of migration
  - Show first endpoint in overview / metadata table
  - Add separate errors view
  - Make sure the record for old component and the record for the new component (that have the same correlation id) are displayed directly below each other
- Add level label to checkpoint component (old GUI)
- Add type label to checkpoint component (old GUI)
- Whitelist api resources (instead of blacklisting)
- Support Quarkus
- Handle invalid checkpoints with level < 0
- Add closeNewThreadsOnly



2.2
---

- Add ProofOfMigrationStorage
- Add ProxyStorage
- Add CloseReportsTask
- Add database storage implementation
- Bump Saxon from 9.4.0.7 to 10.8
- Support Saxon 10 (keep 9 working too)
- Fix concurrency issue
- Show byte[] as UTF-8 with toggle button for Base64
- Upgrade jackson from 1.1.1 to 2.13.0
- Fix incompatible types for field endTime
- Fix ArrayIndexOutOfBoundsException on concurrent last endpoint for report and a startpoint
- Change integration method of new GUI with Frank!Framework
- Visualize waiting for threads and streams and add closeThreads and closeMessageCapturers
- Add REST API for new frontend
- Add checkpoint after stubable code has been executed
- Clean storage java files
- Fix concurrency issues in file storage when running load test with both reads and writes
- Change log level of xml storage from info to debug
- Fix typo in "in progress for more than 5 minutes" message
- Fix Echo2 session reset on unexpected throwable during import
- Fix error message on unexpected throwable during import
- Use exception class name for message class name in case of stream with exception
- Fix NPE on report upload
- Fix report not closed for synchronous stream (a stream that is closed immediately)
- Fix reports in progress and Waiting for message capturers message shown too early
- Fix estimated memory usage incorrect for stream
- Enable application to notify Ladybug of an exception being thrown during processing of a stream
- Highlight in tree when message is an exception
- Stream Reader and InputStream
- Fix: toObject is using UTF-8 instead of value from charsetNotifier
- Add method to close report or thread in report



2.1
---

- Fix IndexOutOfBoundsException on abort when ignoring report
- Enable application to notify the charset for binary streams
- Show warning for reports in progress and message capturers waiting for close
- Use generics so no casting is needed for checkpoint methods
- Use UTF-8 encoding for byte array when possible
- Return stream object as stub when original message was a stream object
- Set messageClassName only when message object is not a String object
- Add messageClassName to toXML()
- Show message on upload when no ttr files in zip
- Add support for streams
- Upgrade ibis-echo2 from 2.0.2 to 2.0.3
- Refactor XmlStorage's MetadataHandler in order to minimize rewrites.
- Fix change in report name after replacement for XmlStorage
- Add thread with warning when threadCreatepoint() (and threadStartpoint()) not used
- Fix metadata file format for XmlStorage
- Improve storage id handling of XmlStorage while reading from a filesystem.
- Fix report discovery for XmlStorage
- Make security log configurable
- Upgrade junit from 4.12 to 4.13.1
- Replace log4j with slf4j
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