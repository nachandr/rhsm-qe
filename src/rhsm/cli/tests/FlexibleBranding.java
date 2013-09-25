package rhsm.cli.tests;

import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;

import java.math.BigInteger;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import com.redhat.qe.Assert;
import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import com.redhat.qe.auto.testng.TestNGUtils;

import rhsm.base.CandlepinType;
import rhsm.base.ConsumerType;
import rhsm.base.SubscriptionManagerBaseTestScript;
import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.CandlepinTasks;
import rhsm.data.ConsumerCert;
import rhsm.data.ContentNamespace;
import rhsm.data.EntitlementCert;
import rhsm.data.InstalledProduct;
import rhsm.data.OrderNamespace;
import rhsm.data.ProductCert;
import rhsm.data.ProductSubscription;
import rhsm.data.Repo;
import rhsm.data.RevokedCert;
import rhsm.data.SubscriptionPool;
import rhsm.data.YumRepo;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;
import com.redhat.qe.tools.SSHCommandRunner;

/**
 * @author skallesh
 * 
 * 
 */
@Test(groups = { "FlexibleBranding" })
public class FlexibleBranding extends SubscriptionManagerCLITestScript {
	protected  static String BrandType = null;
	protected String ownerKey="";
	protected String randomAvailableProductId;
	protected EntitlementCert expiringCert = null;
	protected String EndingDate;
	protected final String importCertificatesDir = "/tmp/sm-importExpiredCertificatesDir"
			.toLowerCase();
	protected final String myEmptyCaCertFile = "/etc/rhsm/ca/myemptycert.pem";
	protected Integer configuredHealFrequency = null;
	protected Integer configuredCertFrequency = null;
	protected String configuredHostname=null;
	protected String factname="system.entitlements_valid";
	protected String RemoteServerError="Remote server error. Please check the connection details, or see /var/log/rhsm/rhsm.log for more information.";
	protected String SystemDateOnClient=null;
	protected String SystemDateOnServer=null;
	List<String> providedProducts = new ArrayList<String>();
	protected List<File> entitlementCertFiles = new ArrayList<File>();
	protected final String Brand_Name = "/var/lib/rhsm/branded_name".toLowerCase();


	
		
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@Test(	description="verify if brandname file is created",
			groups={"VerifyBrandFileCreation"},
			enabled=true)
	public void VerifyBrand_NameFileCreation() throws Exception {
		String productname=null;
		clienttasks.register(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, null, null, null,
				(String) null, null, null, null, true, null, null, null, null);
		client.runCommandAndWait("cp /root/temp1/32060.pem "+clienttasks.productCertDir);
		for (InstalledProduct installed : clienttasks.getCurrentlyInstalledProducts()) {
			
				productname=installed.productName;
			
		
		}
		clienttasks.subscribe(true, null,(String)null, null, null, null, null, null, null, null, null);
		String result=client.runCommandAndWait("cat "+Brand_Name).getStdout();
		Assert.assertEquals(result.trim(), productname.trim());
	}
	
	
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@Test(	description="verify if brandname file is created",
			groups={"VerifyBrandFileDeletion"},
			enabled=true)
	public void VerifyBrand_NameFileNotDeletedAfterUnsubscribing() throws Exception {
		String productname=null;
		clienttasks.register(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, null, null, null,
				(String) null, null, null, null, true, null, null, null, null);
		client.runCommandAndWait("cp /root/temp1/32060.pem "+clienttasks.productCertDir);
		for (InstalledProduct installed : clienttasks.getCurrentlyInstalledProducts()) {
			productname=installed.productName;
			
		}
		clienttasks.subscribe(true, null,(String)null, null, null, null, null, null, null, null, null);
		String result=client.runCommandAndWait("cat "+Brand_Name).getStdout();
		Assert.assertEquals(result.trim(), productname.trim());
		clienttasks.unsubscribe(true,(BigInteger)null, null, null, null);
		Assert.assertEquals(result.trim(), productname.trim());
	}
	
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@Test(	description="verify if brandname file are replaced",
			groups={"VerifyBrandFileContents"},
			enabled=true)
	public void VerifyBrand_nameContentsAreReplaced() throws Exception {
		String productname=null;
		clienttasks.register(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, null, null, null,
				(String) null, null, null, null, true, null, null, null, null);
		client.runCommandAndWait("cp /root/temp1/32060.pem "+clienttasks.productCertDir);
		for (InstalledProduct installed : clienttasks.getCurrentlyInstalledProducts()) {
			productname=installed.productName;
			
		}
		clienttasks.subscribe(true, null,(String)null, null, null, null, null, null, null, null, null);
		String result=client.runCommandAndWait("cat "+Brand_Name).getStdout();
		Assert.assertEquals(result.trim(), productname.trim());
		clienttasks.unsubscribe(true,(BigInteger)null, null, null, null);
		Assert.assertEquals(result.trim(), productname.trim());
		moveProductCertFiles();
		client.runCommandAndWait("cp /root/temp1/37060.pem "+clienttasks.productCertDir);
		for (InstalledProduct installed : clienttasks.getCurrentlyInstalledProducts()) {
			productname=installed.productName;
			
		}
		clienttasks.subscribe(true, null,(String)null, null, null, null, null, null, null, null, null);
		result=client.runCommandAndWait("cat "+Brand_Name).getStdout();
		Assert.assertEquals(result.trim(), productname.trim());
		clienttasks.unsubscribe(true,(BigInteger)null, null, null, null);
		Assert.assertEquals(result.trim(), productname.trim());
	
		
	}
	
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@Test(	description="verify if brandname file are replaced",
			groups={"VerifyBrand_TypeValue"},
			enabled=true) //yet to work on
	public void VerifyBrand_TypeValue() throws Exception {
		String productname=null;
		clienttasks.register(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, null, null, null,
				(String) null, null, null, null, true, null, null, null, null);
		client.runCommandAndWait("cp /root/temp1/32060.pem "+clienttasks.productCertDir);
		for (InstalledProduct installed : clienttasks.getCurrentlyInstalledProducts()) {
			productname=installed.productName;
			
		}
		clienttasks.subscribe(true, null,(String)null, null, null, null, null, null, null, null, null);
		client.runCommandAndWaitWithoutLogging("find "+clienttasks.entitlementCertDir+" -regex \"/.+/[0-9]+.pem\" -exec rct cat-cert {} \\;");
		String certificates = client.getStdout();
		String BrandType=parseInfo(certificates);
			Assert.assertEquals(BrandType, "OS");
		}
	
	
	

	
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@Test(	description="verify if brandname file are replaced",
			groups={"CreationWithImport"},
			enabled=true) 
	public void VerifyBrand_NameFileCreationWithImportedCert() throws Exception {
		String productname=null;
		clienttasks.register(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, null, null, null,
				(String) null, null, null, null, true, null, null, null, null);
		client.runCommandAndWait("cp /root/temp1/32060.pem "+clienttasks.productCertDir);
		for (InstalledProduct installed : clienttasks.getCurrentlyInstalledProducts()) {
			productname=installed.productName;
			
		}
		clienttasks.subscribe(true, null,(String)null, null, null, null, null, null, null, null, null);
		client.runCommand("mkdir /root/importedcertDir");
		client.runCommand("cat "+clienttasks.entitlementCertDir+"/* >> /root/importedcertDir/importedcert.pem");
		client.runCommand("rm -rf "+Brand_Name);
		clienttasks.clean(null, null, null);
		clienttasks.importCertificate("/root/importedcertDir/importedcert.pem");
		String result=client.runCommandAndWait("cat "+Brand_Name).getStdout();
		client.runCommand("rm -rf /root/importedcertDir/importedcert.pem");
		Assert.assertEquals(result.trim(), productname.trim());
		clienttasks.unsubscribe(true,(BigInteger)null, null, null, null);
		Assert.assertEquals(result.trim(), productname.trim());
	}
	
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@Test(	description="verify if brandname file are replaced",
			groups={"CreationWithRHSMCERTD"},
			enabled=true) 
	public void VerifyBrand_NameFileCreationWithRHSMCERTD() throws Exception {
		String productname=null;
		clienttasks.register(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, null, null, null,
				(String) null, null, null, null, true, null, null, null, null);
		client.runCommandAndWait("cp /root/temp1/32060.pem "+clienttasks.productCertDir);
		for (InstalledProduct installed : clienttasks.getCurrentlyInstalledProducts()) {
			productname=installed.productName;
			
		}
		clienttasks.restart_rhsmcertd(null, null, false, null);
		sleep(2*60*1000);
		String result=client.runCommandAndWait("cat "+Brand_Name).getStdout();
		Assert.assertEquals(result.trim(), productname.trim());
		clienttasks.unsubscribe(true,(BigInteger)null, null, null, null);
		Assert.assertEquals(result.trim(), productname.trim());
	}
	
	
	@BeforeClass(groups = { "setup" })
	protected void moveProductCertFiles() throws IOException {
		client = new SSHCommandRunner(sm_clientHostname, sm_sshUser, sm_sshKeyPrivate,sm_sshkeyPassphrase,null);
		if(!(RemoteFileTasks.testExists(client, "/root/temp1/"))){
			client.runCommandAndWait("mkdir " + "/root/temp1/");
		}
			client.runCommandAndWait("mv " + clienttasks.productCertDir + "/"+ "*" + " " + "/root/temp1/");
		
		}
	
	@AfterGroups(groups = { "setup" }, value = {"FlexibleBranding"})
			@AfterClass(groups = "setup")
			public void restoreProductCerts() throws IOException {
				client = new SSHCommandRunner(sm_clientHostname, sm_sshUser, sm_sshKeyPrivate,sm_sshkeyPassphrase,null);
				client.runCommandAndWait("mv " + "/root/temp1/*.pem" + " "
						+ clienttasks.productCertDir);
				client.runCommandAndWait("rm -rf " + "/root/temp1");
	}
	
	static public String parseInfo(String rawCertificates) {
		Map<String,String> regexes = new HashMap<String,String>();
		List certData = new ArrayList();
		regexes.put("BrandType",			"Product:(?:(?:\\n.+)+)Brand Type: (.+)");
		// split the rawCertificates process each individual rawCertificate
		String rawCertificateRegex = "\\+-+\\+\\n\\s+Entitlement Certificate\\n\\+-+\\+";
		for (String rawCertificate : rawCertificates.split(rawCertificateRegex)) {
			
			// strip leading and trailing blank lines and skip blank rawCertificates
			rawCertificate = rawCertificate.replaceAll("^\\n*","").replaceAll("\\n*$", "");
			if (rawCertificate.length()==0) continue;
			List<Map<String,String>> certDataList = new ArrayList<Map<String,String>>();
			for(String field : regexes.keySet()){
				Pattern pat = Pattern.compile(regexes.get(field), Pattern.MULTILINE);
			//	BrandTypeValue=regexes.get(field);
				addRegexMatchesToList(pat, rawCertificate, certDataList, field);
			}
			
			// assert that there is only one group of certData found in the list
			List<String> BrandTypeValue = new ArrayList<String>();
			for(Map<String,String> CertMap : certDataList) {
				// normalize newlines from productName when it spans multiple lines (introduced by bug 864177)
				String key = "BrandType",Brandtype = CertMap.get(key);
				if (Brandtype!=null) {
					CertMap.remove(key);
					BrandType = Brandtype.replaceAll("\\s*\\n\\s*", " ");
				}
				//BrandTypeValue.add(BrandType);
			}}
			return BrandType;
			
	}
		
		
	static protected boolean addRegexMatchesToList(Pattern regex, String to_parse, List<Map<String,String>> matchList, String sub_key) {
		boolean foundMatches = false;
		Matcher matcher = regex.matcher(to_parse);
		int currListElem=0;
		while (matcher.find()){
			if (matchList.size() < currListElem + 1) matchList.add(new HashMap<String,String>());
			Map<String,String> matchMap = matchList.get(currListElem);
			matchMap.put(sub_key, matcher.group(1).trim());
			matchList.set(currListElem, matchMap);
			currListElem++;
			foundMatches = true;
		}
        if (!foundMatches) {
        	//log.warning("Could not find regex '"+regex+"' match for field '"+sub_key+"' while parsing: "+to_parse );
        	log.finer("Could not find regex '"+regex+"' match for field '"+sub_key+"' while parsing: "+to_parse );
        }
		return foundMatches;
	}
	}

