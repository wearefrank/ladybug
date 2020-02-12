Ladybug Test Tool release notes
===============================



Upcoming
--------




2.0.12
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