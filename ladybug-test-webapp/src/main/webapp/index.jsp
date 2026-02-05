<%@ page import="org.wearefrank.ladybug.Report"%>
<%@ page import="org.wearefrank.ladybug.TestTool"%>
<%@ page import="org.wearefrank.ladybug.MessageEncoderImpl"%>
<%@ page import="org.wearefrank.ladybug.storage.CrudStorage"%>
<%@ page import="org.wearefrank.ladybug.storage.LogStorage"%>
<%@ page import="org.wearefrank.ladybug.storage.Storage"%>
<%@ page import="org.wearefrank.ladybug.test.webapp.test.webapp.ComplexReports"%>
<%@ page import="org.springframework.web.context.WebApplicationContext"%>
<%@ page import="org.springframework.web.context.support.WebApplicationContextUtils"%>
<%@ page import="java.util.ArrayList"%>
<%@ page import="java.util.Arrays"%>
<%@ page import="java.util.List"%>
<%@ page import="java.util.UUID"%>
<%@ page import="java.io.ByteArrayInputStream"%>
<%@ page import="java.io.Writer" %>
<%@ page import="java.io.StringWriter" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.HashMap" %>
<%
	ServletContext servletContext = request.getSession().getServletContext();
	WebApplicationContext webApplicationContext = WebApplicationContextUtils.getWebApplicationContext(servletContext);
	TestTool testTool = (TestTool)webApplicationContext.getBean("testTool");
	String correlationId = UUID.randomUUID().toString();
	String otherCorrelationId = UUID.randomUUID().toString();
	String reportName;
	List<String> reportNames = new ArrayList<String>();
	String userName = null;

	if (request.getUserPrincipal() != null) {
		userName = request.getUserPrincipal().getName();
	}

	// Create report links
	String createReportAction = request.getParameter("createReport");
	reportNames.add(reportName = "Simple report");
	if (reportName.equals(createReportAction)) {
		testTool.startpoint(correlationId, null, reportName, "Hello World!");
		testTool.endpoint(correlationId, null, reportName, "Goodbye World!");
	}
	reportNames.add(reportName = "Another simple report");
	if (reportName.equals(createReportAction)) {
		testTool.startpoint(otherCorrelationId, null, reportName, "Hello World!");
		testTool.endpoint(otherCorrelationId, null, reportName, "Goodbye World!");
	}
	reportNames.add(reportName = "Report with empty string as name");
	if (reportName.equals(createReportAction)) {
		testTool.startpoint(otherCorrelationId, null, "", "Hello World!");
		testTool.endpoint(otherCorrelationId, null, "", "Goodbye World!");
	}
	reportNames.add(reportName = "Report with null as name");
	if (reportName.equals(createReportAction)) {
		testTool.startpoint(otherCorrelationId, null, null, "Hello World!");
		testTool.endpoint(otherCorrelationId, null, null, "Goodbye World!");
	}
	reportNames.add(reportName = "Message is captured asynchronously from a character stream");
	if (reportName.equals(createReportAction)) {
		testTool.setCloseMessageCapturers(true);
		testTool.setCloseThreads(true);
		testTool.startpoint(correlationId, null, reportName, "Hello World!");
		Writer writerMessage = testTool.inputpoint(correlationId, null, "writer", new StringWriter());
		writerMessage.write("Passing by the world!");
		testTool.endpoint(correlationId, null, reportName, "Goodbye World!");
		testTool.close(correlationId);
		writerMessage.close();
	}
	reportNames.add(reportName = "Message is null");
	if (reportName.equals(createReportAction)) {
		testTool.startpoint(correlationId, null, reportName, "Hello World!");
		testTool.infopoint(correlationId, null, "Null String", null);
		testTool.setMessageEncoder(testTool.getMessageEncoder());
		testTool.endpoint(correlationId, null, reportName, "Goodbye World!");
	}
	reportNames.add(reportName = "Message is an empty string");
	if (reportName.equals(createReportAction)) {
		testTool.startpoint(correlationId, null, reportName, "Hello World!");
		testTool.infopoint(correlationId, null, "Empty String", "");
		testTool.setMessageEncoder(testTool.getMessageEncoder());
		testTool.endpoint(correlationId, null, reportName, "Goodbye World!");
	}
	reportNames.add(reportName = "Hide a checkpoint in blackbox view");
	if (reportName.equals(createReportAction)) {
		testTool.startpoint(correlationId, null, reportName, "Hello World!");
		testTool.infopoint(correlationId, null, "Hide this checkpoint", "");
		testTool.setMessageEncoder(testTool.getMessageEncoder());
		testTool.endpoint(correlationId, null, reportName, "Goodbye World!");
	}
	reportNames.add(reportName = "Json checkpoint");
	if (reportName.equals(createReportAction)) {
		testTool.startpoint(correlationId, null, reportName, "{ \"employees\": [ { \"id\": 1, \"name\": \"John Doe\", \"department\": \"Engineering\", \"position\": \"Software Engineer\", \"salary\": 80000 }, { \"id\": 2, \"name\": \"Jane Smith\", \"department\": \"Marketing\", \"position\": \"Marketing Manager\", \"salary\": 70000 }, { \"id\": 3, \"name\": \"Michael Johnson\", \"department\": \"Finance\", \"position\": \"Financial Analyst\", \"salary\": 75000 }, { \"id\": 4, \"name\": \"Emily Brown\", \"department\": \"Human Resources\", \"position\": \"HR Specialist\", \"salary\": 65000 }, { \"id\": 5, \"name\": \"David Lee\", \"department\": \"Sales\", \"position\": \"Sales Representative\", \"salary\": 60000 }, { \"id\": 6, \"name\": \"Sarah Wilson\", \"department\": \"Engineering\", \"position\": \"Software Engineer\", \"salary\": 85000 }, { \"id\": 7, \"name\": \"Alex Garcia\", \"department\": \"Marketing\", \"position\": \"Marketing Specialist\", \"salary\": 60000 }, { \"id\": 8, \"name\": \"Olivia Martinez\", \"department\": \"Finance\", \"position\": \"Financial Advisor\", \"salary\": 80000 }, { \"id\": 9, \"name\": \"William Clark\", \"department\": \"Human Resources\", \"position\": \"HR Manager\", \"salary\": 90000 }, { \"id\": 10, \"name\": \"Emma Rodriguez\", \"department\": \"Sales\", \"position\": \"Sales Manager\", \"salary\": 100000 }, { \"id\": 11, \"name\": \"James Anderson\", \"department\": \"Engineering\", \"position\": \"Lead Software Engineer\", \"salary\": 95000 }, { \"id\": 12, \"name\": \"Sophia Wright\", \"department\": \"Marketing\", \"position\": \"Marketing Director\", \"salary\": 110000 }, { \"id\": 13, \"name\": \"Ethan Thomas\", \"department\": \"Finance\", \"position\": \"Chief Financial Officer\", \"salary\": 150000 }, { \"id\": 14, \"name\": \"Ava Hall\", \"department\": \"Human Resources\", \"position\": \"HR Director\", \"salary\": 120000 }, { \"id\": 15, \"name\": \"Mia Lewis\", \"department\": \"Sales\", \"position\": \"Sales Director\", \"salary\": 130000 } ] }");
		testTool.endpoint(correlationId, null, reportName, "Goodbye World!");
	}
	reportNames.add(reportName = "Message encoded using Base64");
	if (reportName.equals(createReportAction)) {
		byte[] message = new byte[6];
		// Two bytes for ë in UTF-8
		message[0] = (byte)195;
		message[1] = (byte)171;
		// Two bytes for © in UTF-8
		message[2] = (byte)194;
		message[3] = (byte)169;
		// One byte for ë in ISO-8859-1
		message[4] = (byte)235;
		// One byte for © in ISO-8859-1
		message[5] = (byte)169;
		// The last two bytes cannot be encoded in UTF-8 so Ladybug will use Base64 instead
		testTool.startpoint(correlationId, null, reportName, message);
		// Remove last two bytes so message can be encoded using UTF-8 by Ladybug
		message = Arrays.copyOf(message, 4);
		testTool.infopoint(correlationId, null, reportName, message);
		// Test Unicode supplementary characters with a smiley :)
		message[0] = (byte)240;
		message[1] = (byte)159;
		message[2] = (byte)152;
		message[3] = (byte)138;
		testTool.endpoint(correlationId, null, reportName, message);
	}
	reportNames.add(reportName = "Waiting for thread to start");
	if (reportName.equals(createReportAction)) {
		testTool.startpoint(correlationId, null, reportName, "message");
		testTool.threadCreatepoint(correlationId, "123");
	}
	reportNames.add(reportName = "Waiting for message to be captured");
	if (reportName.equals(createReportAction)) {
		testTool.startpoint(correlationId, null, reportName, new ByteArrayInputStream(new byte[0]));
	}
	reportNames.add(reportName = "Multiple startpoints");
	if (reportName.equals(createReportAction)) {
		testTool.startpoint(correlationId, null, reportName, "Hello World!");
		testTool.startpoint(correlationId, null, "startpoint 2", "Hello World!");
		testTool.infopoint(correlationId, null, "Hello infopoint", "Hello World!");
		testTool.endpoint(correlationId, null, "endpoint 2", "Goodbye World!");
		testTool.endpoint(correlationId, null, reportName, "Goodbye World!");
	}
	reportNames.add(reportName = "Complex success report");
	if (reportName.equals(createReportAction)) {
		ComplexReports.fillComplexSuccessReport(correlationId, reportName, testTool);
	}
	reportNames.add(reportName = "Complex error report");
	if (reportName.equals(createReportAction)) {
		ComplexReports.fillComplexErrorReport(correlationId, reportName, testTool);
	}
	reportNames.add(reportName = "Add report to database storage");
	if (reportName.equals(createReportAction)) {
		Report report = new Report();
		report.setName("Report for database storage");
		((CrudStorage)testTool.getStorage("databaseStorage")).store(report);
	}
	reportNames.add(reportName = "Report with message context");
	if (reportName.equals(createReportAction)) {
		testTool.startpoint(correlationId, null, reportName, "Hello World!");
		Map<String, Object> messageContext = new HashMap<String, Object>();
		messageContext.put("messageContextKey", "messageContextValue");
		testTool.endpoint(correlationId, null, reportName, "Goodbye World!", messageContext);
	}
	// Other actions
	if ("true".equals(request.getParameter("clearDebugStorage"))) {
		LogStorage debugStorage = (LogStorage)webApplicationContext.getBean("debugStorage");
		debugStorage.clear();
	}
	if ("true".equals(request.getParameter("clearDatabaseStorage"))) {
		Storage databaseStorage = (Storage)webApplicationContext.getBean("databaseStorage");
		databaseStorage.clear();
	}
	if (request.getParameter("changeDebugStorage") != null) {
		testTool.setDebugStorage((LogStorage)testTool.getStorage(request.getParameter("changeDebugStorage")));
	}
	if (request.getParameter("resetDebugStorage") != null) {
		testTool.setDebugStorage((LogStorage) webApplicationContext.getBean("debugStorage"));
	}
	if (request.getParameter("removeReportsInProgress") != null) {
		while (testTool.getNumberOfReportsInProgress() > 0) {
			testTool.removeReportInProgress(0);
		}
	}
	if (request.getParameter("removeReportInProgress") != null) {
		int nr = Integer.valueOf(request.getParameter("removeReportInProgress"));
		testTool.removeReportInProgress(nr -1);
	}
	if(request.getParameter("setReportInProgressThreshold") != null) {
		int time = Integer.valueOf(request.getParameter("setReportInProgressThreshold"));
		testTool.setReportsInProgressThreshold(time);
	}
%>
<html>

  <h1>Browse</h1>

  <a href="testtool">Old Echo2 GUI</a><br/>

  <br/>

  <a href="ladybug">New Angular GUI</a><br/>
  <a href="http://localhost:4200">New Angular GUI using Node.js</a><br/>

  <br/>

  <a href="ladybug/api/testtool">TestTool API</a><br/>
  <a href="http://localhost:4200/api/testtool">TestTool API proxied by Node.js</a><br/>

  <br/>

  <a href="ladybug/api/metadata">Metadata API</a><br/>
  <a href="http://localhost:4200/api/metadata">Metadata API proxied by Node.js</a><br/>

  <br/>

  <h1>Create report</h1>

  <% for (String name : reportNames) { %>
  <a href="index.jsp?createReport=<%=name%>"><%=name%></a><br/>
  <% } %>


  <h1>Other actions</h1>

  <a href="index.jsp?clearDebugStorage=true">Clear debug storage</a><br/>
  <a href="index.jsp?clearDatabaseStorage=true">Clear database storage</a><br/> 
  <a href="index.jsp?changeDebugStorage=databaseStorage">Change debug storage to database storage</a><br/>
  <a href="index.jsp?resetDebugStorage=debugStorage">Reset debug storage to default memory storage</a><br/>
  <a href="index.jsp?removeReportsInProgress">Remove reports in progress</a><br/>
  <a href="index.jsp?removeReportInProgress=1">Remove report in progress number 1</a><br/>
  <a href="h2">Manage H2 database</a> with JDBC URL set to jdbc:h2:../ladybug/data/database-storage/ladybug (like in springTestToolTestWebapp.xml) and User Name and Password empty<br/>
  <a href="index.jsp?setReportInProgressThreshold=1000">Set the report in progress threshold to 1 second</a><br/>
  <a href="index.jsp?setReportInProgressThreshold=300000">Reset the report in progress threshold to 5 min</a><br/>


  <h1>Debug info</h1>

  Logged in user: <%= userName %><br/>

  <br/>

  Name: <%= testTool.getName() %><br/>
  Version: <%= testTool.getVersion() %><br/>
  SpecificationVersion: <%= testTool.getSpecificationVersion() %><br/>
  ImplementationVersion: <%= testTool.getImplementationVersion() %><br/>
  ConfigName: <%= testTool.getConfigName() %><br/>
  ConfigVersion: <%= testTool.getConfigVersion() %><br/>

  <br/>

  ReportGeneratorEnabled: <%= testTool.isReportGeneratorEnabled() %><br/>
  RegexFilter: <%= testTool.getRegexFilter() %><br/>

  <br/>

  NumberOfReportsInProgress: <%= testTool.getNumberOfReportsInProgress() %><br/>
  ReportsInProgressEstimatedMemoryUsage: <%= testTool.getReportsInProgressEstimatedMemoryUsage() %><br/>

  <br/>

  DebugStorage: <%= testTool.getDebugStorage() %><br/>
  DebugStorage size: <%= testTool.getDebugStorage().getSize() %><br/>
  TestStorage: <%= testTool.getTestStorage() %><br/>
  TestStorage size: <%= testTool.getTestStorage().getSize() %><br/>

  <br/>

  Debugger: <%= testTool.getDebugger() %><br/>
  Rerunner: <%= testTool.getRerunner() %><br/>

  <br/>

  Views: <%= testTool.getViews() %><br/>
  StubStrategies: <%= testTool.getStubStrategies() %><br/>
  DefaultStubStrategy: <%= testTool.getDefaultStubStrategy() %><br/>
  MatchingStubStrategiesForExternalConnectionCode: <%= testTool.getMatchingStubStrategiesForExternalConnectionCode() %><br/>

  <br/>

  MaxCheckpoints: <%= testTool.getMaxCheckpoints() %><br/>
  MaxMemoryUsage: <%= testTool.getMaxMemoryUsage() %><br/>
  MaxMessageLength: <%= testTool.getMaxMessageLength() %><br/>

  <br/>

  MessageTransformer: <%= testTool.getMessageTransformer() %><br/>
  MessageEncoder: <%= testTool.getMessageEncoder() %><br/>
  MessageCapturer: <%= testTool.getMessageCapturer() %><br/>
  CloseMessageCapturers: <%= testTool.isCloseMessageCapturers() %><br/>
  CloseThreads: <%= testTool.isCloseThreads() %><br/>

  <br/>

  SecurityLog: <%= testTool.getSecurityLog() %><br/>

  <br/>

  Default charset: <%= java.nio.charset.Charset.defaultCharset() %><br/>
  File encoding: <%= System.getProperty("file.encoding") %><br/>

</html>
