package com.redhat.qe.sm.base;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;

import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.sm.cli.tasks.CandlepinTasks;
import com.redhat.qe.sm.cli.tasks.SubscriptionManagerTasks;
import com.redhat.qe.sm.data.SubscriptionPool;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;
import com.redhat.qe.tools.SSHCommandRunner;

/**
 * @author ssalevan
 * @author jsefler
 *
 */
public class SubscriptionManagerTestScript extends com.redhat.qe.auto.testng.TestScript{
//	protected static final String defaultAutomationPropertiesFile=System.getenv("HOME")+"/sm-tests.properties";
//	public static final String RHSM_LOC = "/usr/sbin/subscription-manager-cli ";
	
	protected String serverHostname			= System.getProperty("rhsm.server.hostname");
	protected String serverPort 			= System.getProperty("rhsm.server.port");
	protected String serverPrefix 			= System.getProperty("rhsm.server.prefix");
	protected String serverBaseUrl			= System.getProperty("rhsm.server.baseurl");
	protected String serverInstallDir		= System.getProperty("rhsm.server.installdir");
	protected String serverImportDir		= System.getProperty("rhsm.server.importdir");
	protected String serverBranch			= System.getProperty("rhsm.server.branch");
	protected Boolean isServerOnPremises	= Boolean.valueOf(System.getProperty("rhsm.server.onpremises","false"));
	protected Boolean deployServerOnPremises= Boolean.valueOf(System.getProperty("rhsm.server.deploy","true"));

	protected String client1hostname		= System.getProperty("rhsm.client1.hostname");
	protected String client1username		= System.getProperty("rhsm.client1.username");
	protected String client1password		= System.getProperty("rhsm.client1.password");

	protected String client2hostname		= System.getProperty("rhsm.client2.hostname");
	protected String client2username		= System.getProperty("rhsm.client2.username");
	protected String client2password		= System.getProperty("rhsm.client2.password");

	protected String clienthostname			= client1hostname;
	protected String clientusername			= client1username;
	protected String clientpassword			= client1password;
	
	protected String clientOwnerUsername	= System.getProperty("rhsm.client.owner.username");
	protected String clientOwnerPassword	= System.getProperty("rhsm.client.owner.password");
	protected String clientUsernames		= System.getProperty("rhsm.client.usernames");
	protected String clientPasswords		= System.getProperty("rhsm.client.passwords");

	protected String tcUnacceptedUsername	= System.getProperty("rhsm.client.username.tcunaccepted");
	protected String tcUnacceptedPassword	= System.getProperty("rhsm.client.password.tcunaccepted");
	protected String regtoken				= System.getProperty("rhsm.client.regtoken");
	protected int certFrequency				= Integer.valueOf(System.getProperty("rhsm.client.certfrequency"));
	protected String enablerepofordeps		= System.getProperty("rhsm.client.enablerepofordeps");

	protected String prodCertLocation		= System.getProperty("rhsm.prodcert.url");
	protected String prodCertProduct		= System.getProperty("rhsm.prodcert.product");
	
	protected String sshUser				= System.getProperty("rhsm.ssh.user","root");
	protected String sshKeyPrivate			= System.getProperty("rhsm.sshkey.private",".ssh/id_auto_dsa");
	protected String sshkeyPassphrase		= System.getProperty("rhsm.sshkey.passphrase","");

//	protected String itDBSQLDriver			= System.getProperty("rhsm.it.db.sqldriver", "oracle.jdbc.driver.OracleDriver");
//	protected String itDBHostname			= System.getProperty("rhsm.it.db.hostname");
//	protected String itDBDatabase			= System.getProperty("rhsm.it.db.database");
//	protected String itDBPort				= System.getProperty("rhsm.it.db.port", "1521");
//	protected String itDBUsername			= System.getProperty("rhsm.it.db.username");
//	protected String itDBPassword			= System.getProperty("rhsm.it.db.password");
	
	protected String dbHostname				= System.getProperty("rhsm.server.db.hostname");
	protected String dbSqlDriver			= System.getProperty("rhsm.server.db.sqldriver");
	protected String dbPort					= System.getProperty("rhsm.server.db.port");
	protected String dbName					= System.getProperty("rhsm.server.db.name");
	protected String dbUsername				= System.getProperty("rhsm.server.db.username");
	protected String dbPassword				= System.getProperty("rhsm.server.db.password");

	
	
	protected String[] rpmUrls				= System.getProperty("rhsm.rpm.urls").split(",");
	protected Boolean installRPMs			= Boolean.valueOf(System.getProperty("rhsm.rpm.install","true"));


//DELETEME
//MOVED TO TASKS CLASSES
//	protected String defaultConfigFile		= "/etc/rhsm/rhsm.conf";
//	protected String rhsmcertdLogFile		= "/var/log/rhsm/rhsmcertd.log";
//	protected String rhsmYumRepoFile		= "/etc/yum/pluginconf.d/rhsmplugin.conf";
	
//	public static Connection itDBConnection = null;
	public static Connection dbConnection = null;
	
	public static SSHCommandRunner server	= null;
	public static SSHCommandRunner client	= null;
	public static SSHCommandRunner client1	= null;	// client1
	public static SSHCommandRunner client2	= null;	// client2
	
	protected static CandlepinTasks servertasks	= null;
	protected static SubscriptionManagerTasks clienttasks	= null;
	protected static SubscriptionManagerTasks client1tasks	= null;	// client1 subscription manager tasks
	protected static SubscriptionManagerTasks client2tasks	= null;	// client2 subscription manager tasks
	
	protected Random randomGenerator = new Random(System.currentTimeMillis());
	
	public SubscriptionManagerTestScript() {
		super();
		// TODO Auto-generated constructor stub
	}


	
	
	// Configuration Methods ***********************************************************************
	
	@BeforeSuite(groups={"setup"},description="subscription manager set up")
	public void setupBeforeSuite() throws ParseException, IOException{
	
		client = new SSHCommandRunner(clienthostname, sshUser, sshKeyPrivate, sshkeyPassphrase, null);
		clienttasks = new com.redhat.qe.sm.cli.tasks.SubscriptionManagerTasks(client);
		
		// will we be connecting to the candlepin server?
		if (!(	serverHostname.equals("") || serverHostname.startsWith("$") ||
				serverInstallDir.equals("") || serverInstallDir.startsWith("$") )) {
			server = new SSHCommandRunner(serverHostname, sshUser, sshKeyPrivate, sshkeyPassphrase, null);
			servertasks = new com.redhat.qe.sm.cli.tasks.CandlepinTasks(server,serverInstallDir);

		} else {
			log.info("Assuming the server is already setup and running.");
		}
		
		// will we be testing multiple clients?
		if (!(	client2hostname.equals("") || client2hostname.startsWith("$") ||
				client2username.equals("") || client2username.startsWith("$") ||
				client2password.equals("") || client2password.startsWith("$") )) {
			client1 = client;
			client1tasks = clienttasks;
			client2 = new SSHCommandRunner(client2hostname, sshUser, sshKeyPrivate, sshkeyPassphrase, null);
			client2tasks = new com.redhat.qe.sm.cli.tasks.SubscriptionManagerTasks(client2);
		} else {
			log.info("Multi-client testing will be skipped.");
		}
		
		// setup the server
		if (server!=null && isServerOnPremises) {
			servertasks.updateConfigFileParameter("pinsetter.org.fedoraproject.candlepin.pinsetter.tasks.CertificateRevocationListTask.schedule","0 0\\/2 * * * ?");
			servertasks.cleanOutCRL();
			if (deployServerOnPremises)
				servertasks.deploy(serverHostname, serverImportDir,serverBranch);
			
			// also connect to the candlepin server database
			connectToDatabase();  // do this after the call to deploy since it will restart postgresql
		}
		
		// in the event that the clients are already registered from a prior run, unregister them
		unregisterClientsAfterSuite();
		
		// setup the client(s)
		if (installRPMs) client1tasks.installSubscriptionManagerRPMs(rpmUrls,enablerepofordeps);
		client1tasks.consumerCertDir	= client1tasks.getConfigFileParameter("consumerCertDir");
		client1tasks.entitlementCertDir	= client1tasks.getConfigFileParameter("entitlementCertDir");
		client1tasks.productCertDir		= client1tasks.getConfigFileParameter("productCertDir");
		client1tasks.consumerCertFile	= client1tasks.consumerCertDir+"/cert.pem";
		client1tasks.consumerKeyFile	= client1tasks.consumerCertDir+"/key.pem";
		client1tasks.updateConfigFileParameter("hostname", serverHostname);
		client1tasks.updateConfigFileParameter("port", serverPort);
		client1tasks.updateConfigFileParameter("prefix", serverPrefix);
		client1tasks.updateConfigFileParameter("insecure", "1");
		client1tasks.changeCertFrequency(certFrequency,false);
		client1tasks.cleanOutAllCerts();
		if (client2tasks!=null) if (installRPMs) client2tasks.installSubscriptionManagerRPMs(rpmUrls,enablerepofordeps);
		if (client2tasks!=null) client2tasks.consumerCertDir	= client2tasks.getConfigFileParameter("consumerCertDir");
		if (client2tasks!=null) client2tasks.entitlementCertDir	= client2tasks.getConfigFileParameter("entitlementCertDir");
		if (client2tasks!=null) client2tasks.productCertDir		= client2tasks.getConfigFileParameter("productCertDir");
		if (client2tasks!=null) client2tasks.consumerCertFile	= client2tasks.consumerCertDir+"/cert.pem";
		if (client2tasks!=null) client2tasks.consumerKeyFile	= client2tasks.consumerCertDir+"/key.pem";
		if (client2tasks!=null) client2tasks.updateConfigFileParameter("hostname", serverHostname);
		if (client2tasks!=null) client2tasks.updateConfigFileParameter("port", serverPort);
		if (client2tasks!=null) client2tasks.updateConfigFileParameter("prefix", serverPrefix);
		if (client2tasks!=null) client2tasks.updateConfigFileParameter("insecure", "1");
		if (client2tasks!=null) client2tasks.changeCertFrequency(certFrequency,false);
		if (client2tasks!=null) client2tasks.cleanOutAllCerts();
		// transfer a copy of the CA Cert from the candlepin server to the client
		// TEMPORARY WORK AROUND TO AVOID ISSUES:
		// https://bugzilla.redhat.com/show_bug.cgi?id=617703 
		// https://bugzilla.redhat.com/show_bug.cgi?id=617303
		/*
		if (server!=null && isServerOnPremises) {
			log.warning("TEMPORARY WORKAROUND...");
			RemoteFileTasks.getFile(server.getConnection(), "/tmp","/etc/candlepin/certs/candlepin-ca.crt");
			RemoteFileTasks.putFile(commandRunner.getConnection(), "/tmp/candlepin-ca.crt", "/tmp/", "0644");
		}
		*/
	}
	
	@AfterSuite(groups={"setup"},description="subscription manager tear down")
	public void unregisterClientsAfterSuite() {
		if (client2tasks!=null) client2tasks.unregister_();	// release the entitlements consumed by the current registration
		if (client1tasks!=null) client1tasks.unregister_();	// release the entitlements consumed by the current registration
	}
	
	@AfterSuite(groups={"setup"},description="subscription manager tear down")
	public void disconnectDatabaseAfterSuite() {
		
		// close the candlepin database connection
		if (dbConnection!=null) {
			try {
				dbConnection.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	// close the connection to the database
		}
	}


	
	// Protected Methods ***********************************************************************
	
	protected void connectToDatabase() {
		try { 
			// Load the JDBC driver 
			Class.forName(dbSqlDriver);	//	"org.postgresql.Driver" or "oracle.jdbc.driver.OracleDriver"
			
			// Create a connection to the database
			String url = dbSqlDriver.contains("postgres")? 
					"jdbc:postgresql://" + dbHostname + ":" + dbPort + "/" + dbName :
					"jdbc:oracle:thin:@" + dbHostname + ":" + dbPort + ":" + dbName ;
			log.info(String.format("Attempting to connect to database with url and credentials: url=%s username=%s password=%s",url,dbUsername,dbPassword));
			dbConnection = DriverManager.getConnection(url, dbUsername, dbPassword); 
			DatabaseMetaData dbmd = dbConnection.getMetaData(); //get MetaData to confirm connection
		    log.fine("Connection to "+dbmd.getDatabaseProductName()+" "+dbmd.getDatabaseProductVersion()+" successful.\n");

		} 
		catch (ClassNotFoundException e) { 
			log.warning("JDBC driver not found!:\n" + e.getMessage());
		} 
		catch (SQLException e) {
			log.warning("Could not connect to backend database:\n" + e.getMessage());
		}
	}

	/* DELETEME  OLD CODE FROM ssalevan
	
	public void getSalesToEngineeringProductBindings(){
		try {
			String products = itDBConnection.nativeSQL("select * from butt;");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			log.info("Database query for Sales-to-Engineering product bindings failed!  Traceback:\n"+e.getMessage());
		}
	}
	*/
	

	public static void sleep(long milliseconds) {
		log.info("Sleeping for "+milliseconds+" milliseconds...");
		try {
			Thread.sleep(milliseconds);
		} catch (InterruptedException e) {
			log.info("Sleep interrupted!");
		}
	}
	
	protected int getRandInt(){
		return Math.abs(randomGenerator.nextInt());
	}
	
	
//	public void runRHSMCallAsLang(SSHCommandRunner sshCommandRunner, String lang,String rhsmCall){
//		sshCommandRunner.runCommandAndWait("export LANG="+lang+"; " + rhsmCall);
//	}
//	
//	public void setLanguage(SSHCommandRunner sshCommandRunner, String lang){
//		sshCommandRunner.runCommandAndWait("export LANG="+lang);
//	}
	

	// Protected Inner Data Class ***********************************************************************
	
	protected class RegistrationData {
		public String username=null;
		public String password=null;
		public JSONObject jsonOwner=null;
		public SSHCommandResult registerResult=null;
		public List<SubscriptionPool> allAvailableSubscriptionPools=null;/*new ArrayList<SubscriptionPool>();*/
		public RegistrationData() {
			super();
		}
		public RegistrationData(String username, String password, JSONObject jsonOwner,	SSHCommandResult registerResult, List<SubscriptionPool> allAvailableSubscriptionPools) {
			super();
			this.username = username;
			this.password = password;
			this.jsonOwner = jsonOwner;
			this.registerResult = registerResult;
			this.allAvailableSubscriptionPools = allAvailableSubscriptionPools;
		}
	}
	
	// this list will be populated by subclass ResisterTests.RegisterWithUsernameAndPassword_Test
	protected List<RegistrationData> registrationDataList = new ArrayList<RegistrationData>();	

	
	
	// Data Providers ***********************************************************************

	
	@DataProvider(name="getGoodRegistrationData")
	public Object[][] getGoodRegistrationDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getGoodRegistrationDataAsListOfLists());
	}
	protected List<List<Object>> getGoodRegistrationDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		
//		for (List<Object> registrationDataList : getBogusRegistrationDataAsListOfLists()) {
//			// pull out all of the valid registration data (indicated by an Integer exitCode of 0)
//			if (registrationDataList.contains(Integer.valueOf(0))) {
//				// String username, String password, String type, String consumerId
//				ll.add(registrationDataList.subList(0, 4));
//			}
//			
//		}
// changing to registrationDataList to get all the valid registeredConsumer
		
		for (RegistrationData registeredConsumer : registrationDataList) {
			if (registeredConsumer.registerResult.getExitCode().intValue()==0) {
				ll.add(Arrays.asList(new Object[]{registeredConsumer.username, registeredConsumer.password}));
			}
		}
		
		return ll;
	}
	
	
	@DataProvider(name="getAvailableSubscriptionPoolsData")
	public Object[][] getAvailableSubscriptionPoolsDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getAvailableSubscriptionPoolsDataAsListOfLists());
	}
	protected List<List<Object>> getAvailableSubscriptionPoolsDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		if (clienttasks==null) return ll;
		
		// assure we are registered
		clienttasks.unregister();
		clienttasks.register(clientusername, clientpassword, null, null, null, null);
		if (client2tasks!=null)	{
			client2tasks.unregister();
			client2tasks.register(client2username, client2password, null, null, null, null);
		}
		
		// unsubscribe from all consumed product subscriptions and then assemble a list of all SubscriptionPools
		clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		if (client2tasks!=null)	{
			client2tasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		}

		// populate a list of all available SubscriptionPools
		for (SubscriptionPool pool : clienttasks.getCurrentlyAvailableSubscriptionPools()) {
			ll.add(Arrays.asList(new Object[]{pool}));		
		}
		
		return ll;
	}
	

}
