<%@page import="java.util.Locale"%>
<%@page import="org.apache.commons.lang.StringUtils"%>
<%@page import="org.pentaho.platform.util.messages.LocaleHelper"%>
<%@page import="java.net.URLClassLoader"%>
<%@page import="java.net.URL"%>
<%@page import="java.util.ResourceBundle"%>
<%@page import="java.io.File"%>
<%@page import="org.owasp.esapi.ESAPI"%>

<%
  Locale effectiveLocale = request.getLocale();
  if (!StringUtils.isEmpty(request.getParameter("locale"))) {
    effectiveLocale = new Locale(request.getParameter("locale"));
    request.getSession().setAttribute("locale_override", request.getParameter("locale"));
    LocaleHelper.setLocaleOverride(effectiveLocale);
  } else {
    request.getSession().setAttribute("locale_override", null);
    LocaleHelper.setLocaleOverride(null);
  }
  
  URLClassLoader loader = new URLClassLoader(new URL[] {application.getResource("/mantle/messages/")});
  ResourceBundle properties = ResourceBundle.getBundle("mantleMessages", request.getLocale(), loader);

%>

<html>
	<head>
		<title>Pentaho User Console</title>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
		<meta name="gwt:property" content="locale=<%=ESAPI.encoder().encodeForHTMLAttribute(effectiveLocale.toString())%>">
		<link rel="shortcut icon" href="/pentaho-style/favicon.ico" />
		<link rel='stylesheet' href='mantle/MantleStyle.css'/>
    <link rel="stylesheet" href="content/data-access/resources/gwt/datasourceEditorDialog.css"/>
    <link rel="stylesheet" href="mantle/Widgets.css" />

    <script language="javascript" type="text/javascript" src="webcontext.js?context=mantle"></script>
        
    <script type="text/javascript" src="mantle/nativeScripts.js"></script>
    <script type="text/javascript">
      if(window.opener && window.opener.reportWindowOpened != undefined){
        window.opener.reportWindowOpened();
      }

  	var dataAccessAvailable = false; //Used by child iframes to tell if data access is available.
    /* this function is called by the gwt code when initing, if the user has permission */
    function initDataAccess(hasAccess) {
      dataAccessAvailable = hasAccess;
      if(!hasAccess){
        return;
      }
      if(typeof(addMenuItem) == "undefined"){
        setTimeout("initDataAccess("+hasAccess+")", 1000);
        return;
      } else {
        addMenuItem("manageDatasourcesEllipsis","manage_content_menu", "ManageDatasourcesCommand");
        addMenuItem("newDatasource","new_menu", "AddDatasourceCommand");
      }
    }

    var datasourceEditorCallback = {
      onFinish : function(val, transport) {
      },
      onError : function(val) {
        alert('error:' + val);
      },
      onCancel : function() {
      },
      onReady : function() {
      }
    }

    // This allows content panels to have PUC create new datasources. The iframe requesting
    // the new datasource must have a function "openDatasourceEditorCallback" on it's window scope
    // to be notified of the successful creation of the datasource.
    function openDatasourceEditorIFrameProxy(windowReference){
    	var callbackHelper = function(bool, transport){
    		windowReference.openDatasourceEditorCallback(bool, transport);
    	}
    	pho.openDatasourceEditor(new function(){
        this.onError = function(err){
          alert(err);
        }
        this.onCancel = function(){
        }
        this.onReady = function(){
        }
        this.onFinish = function(bool, transport){
          callbackHelper(bool, transport);
        }
      });
    }

    </script>
	</head>

	<body oncontextmenu="return false;" class="pentaho-page-background">

  <script type="text/javascript" src="http://www.java.com/js/deployJava.js"></script>
  <script type="text/javascript">
    var attributes = { id:'printApplet',
        code:'org.pentaho.Printer.class', 
                      archive:'mantle/puc-printer-applet-1.0-SNAPSHOT.jar', width:1, height:1} ;
    var parameters = {} ;
    
    deployJava.runApplet(attributes, parameters, '1.6');

    function setRobotTrackerVisible(flag){
      (flag) ? $("#robotHelper").show() : $("#robotHelper").hide();
    }
    function doPrint(){
      setRobotTrackerVisible(true);
      var offset = $(".pentaho-tab-deck-panel").offset();
      document.getElementById('printApplet').focus();
      document.getElementById('printApplet').print(offset.left+5,offset.top+5,$(".pentaho-tab-deck-panel").width()+30,$(".pentaho-tab-deck-panel").height());
      
    }
  </script>

	<div id="loading">
    		<div class="loading-indicator">
    			<img src="mantle/large-loading.gif" width="32" height="32"/><%= properties.getString("loadingConsole") %><a href="http://www.pentaho.com"></a><br/>
    			<span id="loading-msg"><%= properties.getString("pleaseWait") %></span>
    		</div>
	</div>

  <div id="robotHelper" style="position:absolute; top:1px; left: 1px; height: 3px; width: 3px; background: url('mantle/robotHelper.png');"></div>

	<!-- OPTIONAL: include this if you want history support -->
	<iframe id="__gwt_historyFrame" style="width:0px;height:0px;border:0;display:none"></iframe>

	</body>

	<script language='javascript' src='mantle/mantle.nocache.js'></script>
	<script language='javascript' src='content/data-access/resources/gwt/DatasourceEditor.nocache.js'></script>

</html>
