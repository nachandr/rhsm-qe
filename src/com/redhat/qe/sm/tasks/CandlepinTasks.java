package com.redhat.qe.sm.tasks;

import java.util.logging.Logger;

import com.redhat.qe.auto.testopia.Assert;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandRunner;

/**
 * @author jsefler
 *
 */
public class CandlepinTasks {

	protected static Logger log = Logger.getLogger(SubscriptionManagerTasks.class.getName());
	protected /*NOT static*/ SSHCommandRunner sshCommandRunner = null;
	

	public CandlepinTasks() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	public CandlepinTasks(SSHCommandRunner runner) {
		super();
		setSSHCommandRunner(runner);
	}
	
	public void setSSHCommandRunner(SSHCommandRunner runner) {
		sshCommandRunner = runner;
	}
	
	
	
	/**
	 * @param serverInstallDir
	 * @param serverImportDir
	 * @param branch - git branch (or tag) to deploy.  The most common values are "master" and "candlepin-latest-tag" (which is a special case)
	 */
	public void deploy(String serverInstallDir, String serverImportDir, String branch) {

		log.info("Upgrading the server to the latest git tag...");
		Assert.assertEquals(RemoteFileTasks.testFileExists(sshCommandRunner, serverInstallDir),1,"Found the server install directory "+serverInstallDir);

		RemoteFileTasks.searchReplaceFile(sshCommandRunner, "/etc/sudoers", "\\(^Defaults[[:space:]]\\+requiretty\\)", "#\\1");	// Needed to prevent error:  sudo: sorry, you must have a tty to run sudo
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "cd "+serverInstallDir+"; git checkout master; git pull", Integer.valueOf(0), null, "(Already on|Switched to branch) 'master'");
		if (branch.equals("candlepin-latest-tag")) {
			RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "cd "+serverInstallDir+"; git tag | sort -t . -k 3 -n | tail -1", Integer.valueOf(0), "^candlepin", null);
			branch = sshCommandRunner.getStdout().trim();
		}
		if (branch.startsWith("candlepin-")) {
			RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "cd "+serverInstallDir+"; git checkout "+branch, Integer.valueOf(0), null, "HEAD is now at .* package \\[candlepin\\] release \\["+branch.substring(branch.indexOf("-")+1)+"\\]."); //HEAD is now at 560b098... Automatic commit of package [candlepin] release [0.0.26-1].
	
		} else {
			RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "cd "+serverInstallDir+"; git checkout "+branch, Integer.valueOf(0), null, "(Already on|Switched to branch) '"+branch+"'");	// Switched to branch 'master' // Already on 'master'
		}
//		RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "cd "+serverInstallDir+"; git checkout "+latestGitTag, Integer.valueOf(0), null, "HEAD is now at .* package \\[candlepin\\] release \\["+latestGitTag.substring(latestGitTag.indexOf("-")+1)+"\\]."); //HEAD is now at 560b098... Automatic commit of package [candlepin] release [0.0.26-1].
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "service postgresql restart", Integer.valueOf(0), "Starting postgresql service:\\s+\\[  OK  \\]", null);
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "unset FORCECERT; export GENDB=true; export IMPORTDIR="+serverImportDir+"; cd "+serverInstallDir+"/proxy; buildconf/scripts/deploy", Integer.valueOf(0), "Initialized!", null);
		/* attempt to use live logging
		SSHCommandResult sshCommandResult = sshCommandRunner.runCommandAndWait("cd "+serverInstallDir+"/proxy; buildconf/scripts/deploy", true);
			Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(0));
			Assert.assertContainsMatch(sshCommandResult.getStdout(), "Initialized!");
		*/
	}
	
	public void refreshSubscriptionPools(String server, String port, String owner, String password) {
		log.info("Refreshing the subscription pools for owner '"+owner+"' on candlepin server '"+server+"'...");
		// /usr/bin/curl -u admin:admin -k --header 'Content-type: application/json' --header 'Accept: application/json' --request PUT https://localhost:8443/candlepin/owners/admin/subscriptions
		// /usr/bin/curl -u candlepin_system_admin:admin -k --header 'Content-type: application/json' --header 'Accept: application/json' --request PUT https://candlepin1.devlab.phx1.redhat.com:443/candlepin/owners/1616678/subscriptions
		//                                                                                                                                                                                                             ^orgid of the user for whom you are refreshing pools can be found by connecting to the database -> see https://engineering.redhat.com/trac/IntegratedMgmtQE/wiki/entitlement_qe_get_products

		String command = "/usr/bin/curl -u "+owner+":"+password+" -k --header 'Content-type: application/json' --header 'Accept: application/json' --request PUT https://"+server+":"+port+"/candlepin/owners/"+owner+"/subscriptions";
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner, command, 0);
	}
	
	public void restartTomcat() {
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner,"service tomcat6 restart",Integer.valueOf(0),"^Starting tomcat6: +\\[  OK  \\]$",null);
	}
}
