(ns rhsm.gui.tests.firstboot_tests
  (:use [test-clj.testng :only (gen-class-testng
                                data-driven)]
        [rhsm.gui.tasks.test-config :only (config
                                           clientcmd)]
        [com.redhat.qe.verify :only (verify)]
        [slingshot.slingshot :only (try+
                                    throw+)]
        rhsm.gui.tasks.tools
        gnome.ldtp)
  (:require [rhsm.gui.tasks.tasks :as tasks]
            [rhsm.gui.tests.base :as base]
             rhsm.gui.tasks.ui)
  (:import [org.testng.annotations
            AfterClass
            BeforeClass
            BeforeGroups
            Test]
            org.testng.SkipException
            [com.redhat.qe.auto.bugzilla BzChecker]))

(def window-name "Choose Service")

(defn start_firstboot []
  (tasks/start-firstboot)
  (tasks/ui click :firstboot-forward)
  (tasks/ui click :license-yes)
  (tasks/ui click :firstboot-forward)
  ;; RHEL5 has a different firstboot order than RHEL6
  (if (tasks/fbshowing? :firstboot-window "Firewall")
    (do
      (tasks/ui click :firstboot-forward)
      (tasks/ui click :firstboot-forward)
      (tasks/ui click :firstboot-forward)
      (tasks/ui click :firstboot-forward)
      (sleep 3000) ;; FIXME find a better way than a hard wait...
      (verify (tasks/fbshowing? :register-now))))
  (tasks/ui click :register-now)
  (tasks/ui click :firstboot-forward)
  (assert ( bool (tasks/ui guiexist :firstboot-window window-name))))

(defn kill_firstboot []
  (run-command "killall -9 firstboot")
  (sleep 5000))

(defn zero-proxy-values []
  (tasks/set-conf-file-value "proxy_hostname" "")
  (tasks/set-conf-file-value "proxy_port" "")
  (tasks/set-conf-file-value "proxy_user" "")
  (tasks/set-conf-file-value "proxy_password" ""))

(defn reset_firstboot []
  (kill_firstboot)
  (run-command "subscription-manager clean")
  (zero-proxy-values)
  (start_firstboot))

(defn ^{BeforeClass {:groups ["setup"]}}
  firstboot_init [_]
  (try
    (if (= "RHEL7" (get-release)) (base/startup nil))
    (if (= "5.7" (:version (get-release :true)))
      (throw (SkipException. (str "Skipping firstboot tests on RHEL 5.7 as the tool is not updated"))))
    (skip-if-bz-open "922806")
    (skip-if-bz-open "1016643" (= "RHEL7" (get-release)))
    ;; new rhsm and classic have to be totally clean for this to run
    (run-command "subscription-manager clean")
    (let [sysidpath "/etc/sysconfig/rhn/systemid"]
      (run-command (str "[ -f " sysidpath " ] && rm " sysidpath )))
    (catch Exception e
      (reset! (skip-groups :firstboot) true)
      (throw e))))

(defn ^{AfterClass {:groups ["setup"]
                    :alwaysRun true}}
  firstboot_cleanup [_]
  (assert-valid-testing-arch)
  (kill_firstboot)
  (run-command "subscription-manager clean")
  (zero-proxy-values))

(defn ^{Test {:groups ["firstboot"
                       "blockedByBug-973269"
                       "blockedByBug-988411"]}}
  firstboot_enable_proxy_auth
  "Checks whether the proxy and authentication is enabled in rhsm-conf file"
  [_]
  (try
    (reset_firstboot)
    (tasks/ui click :register-rhsm)
    (let [hostname (@config :basicauth-proxy-hostname)
          port (@config :basicauth-proxy-port)
          username (@config :basicauth-proxy-username)
          password (@config :basicauth-proxy-password)]
      (tasks/enableproxy hostname :port port :user username :pass password :firstboot? true)
      (tasks/ui click :firstboot-forward)
      (tasks/checkforerror)
      (tasks/firstboot-register (@config :username) (@config :password))
      (tasks/verify-conf-proxies hostname port username password))
    (finally
     (reset_firstboot)
     (tasks/disableproxy true)
     (kill_firstboot))))

(defn ^{Test {:groups ["firstboot"
                       "blockedByBug-973269"
                       "blockedByBug-988411"]}}
  firstboot_enable_proxy_noauth
  "Checks whether the proxy is enabled and authentication is disabled in rhsm-conf file"
  [_]
  (try
    (reset_firstboot)
    (tasks/ui click :register-rhsm)
    (let [hostname (@config :noauth-proxy-hostname)
          port (@config :noauth-proxy-port)]
      (tasks/enableproxy hostname :port port :firstboot? true)
      (tasks/ui click :firstboot-forward)
      (tasks/checkforerror)
      (tasks/firstboot-register (@config :username) (@config :password))
      (tasks/verify-conf-proxies hostname port "" ""))
    (finally
     (reset_firstboot)
     (tasks/disableproxy true)
     (kill_firstboot))))

(defn ^{Test {:groups ["firstboot"]}}
  firstboot_disable_proxy
  "Checks whether the proxy and authentication is disabled in rhsm-conf file"
  [_]
  (reset_firstboot)
  (tasks/ui click :register-rhsm)
  (tasks/disableproxy true)
  (tasks/ui click :firstboot-forward)
  (tasks/checkforerror)
  (tasks/firstboot-register (@config :username) (@config :password))
  (tasks/verify-conf-proxies "" "" "" ""))

(defn firstboot_register_invalid_user
  "Register with invalid user credentials at firstboot"
  [user pass recovery]
  (reset_firstboot)
  (tasks/ui click :register-rhsm)
  (tasks/ui click :firstboot-forward)
  (let [test-fn (fn [username password expected-error-type]
                  (try+
                   (tasks/firstboot-register username password)
                   (catch [:type expected-error-type]
                       {:keys [type]}
                     type)))]
    (let [thrown-error (apply test-fn [user pass recovery])
          expected-error recovery]
     (verify (= thrown-error expected-error))
     ;; https://bugzilla.redhat.com/show_bug.cgi?id=703491
     (verify (tasks/fbshowing? :firstboot-user)))))

(defn ^{Test {:groups ["firstboot"
                       "blockedByBug-642660"
                       "blockedByBug-863572"]}}
  firstboot_check_back_button_state
  "Checks the state of back and forward button (whether disabled) when firstboot is
   regestering (when progress-bar is displayed) to to a server. This check is performed
   only in RHEL5 for RHEL6 and above this would be a part of firstboot-register"
  [_]
  (reset_firstboot)
  (tasks/ui click :register-rhsm)
  (tasks/ui click :firstboot-forward)
  (if (= "RHEL5" (get-release))
    (do
      (tasks/firstboot-register (@config :username) (@config :password))
      (verify (not (bool (tasks/ui hasstate :firstboot-back "Sensitive"))))
      (verify (not (bool (tasks/ui hasstate :firstboot-forward "Sensitive")))))
    (tasks/firstboot-register (@config :username) (@config :password) :back-button? true)))

(defn ^{Test {:groups ["firstboot"
                       "blockedByBug-872727"
                       "blockedByBug-973317"]
              :dependsOnMethods ["firstboot_check_back_button_state"]}}
  firstboot_check_back_button
  "Checks the functionality of the back button during firstboot"
  [_]
  (reset_firstboot)
  (tasks/ui click :register-rhsm)
  (tasks/ui click :firstboot-forward)
  (tasks/firstboot-register (@config :username) (@config :password))
  (tasks/ui click :firstboot-back)
  (verify (tasks/fbshowing? :firstboot-window window-name))
  (let [output (:stdout (run-command "subscription-manager identity"))]
    ;; Functionality of back-button is limited to RHEL5. In RHEL6 its
    ;; behavior is different
    (verify (substring? "Current identity is" output))))

;; https://tcms.engineering.redhat.com/case/72669/?from_plan=2806
(defn ^{Test {:groups ["firstboot" "blockedByBug-642660"]}}
  firstboot_skip_register
  "Checks whether firstboot skips register if subscription manger is already registered"
  [_]
  (kill_firstboot)
  (run-command "subscription-manager unregister")
  (run-command "subscritption-manager clean")
  (run-command (str "subscription-manager register"
                                      " --username " (@config :username)
                                      " --password " (@config :password)
                                      " --org " (@config :owner-key)))
  (tasks/start-firstboot)
  (tasks/ui click :firstboot-forward)
  (tasks/ui click :license-yes)
  (tasks/ui click :firstboot-forward)
  (if (tasks/fbshowing? :firstboot-window "Firewall")
    (do
      (tasks/ui click :firstboot-forward)
      (tasks/ui click :firstboot-forward)
      (tasks/ui click :firstboot-forward)
      (tasks/ui click :firstboot-forward)
      (sleep 3000) ;; FIXME find a better way than a hard wait...
      (if (tasks/fbshowing? :firstboot-window "Set Up Software Updates")
        (do
          (tasks/ui click :firstboot-forward)
          (verify (tasks/fbshowing? :firstboot-window "Create User")))))))

;; https://tcms.engineering.redhat.com/case/72670/?from_plan=2806
(defn ^{Test {:groups ["firstboot"]
              :dependsOnMethods ["firstboot_skip_register"]}}
  firstboot_check_register_sm_unregistered
  "Checks whether firstboot navigates to register screen when subscription manager is unregistered"
  [_]
  (run-command "subscription-manager unregister")
  (kill_firstboot)
  (run-command "subscription-manager clean")
  (zero-proxy-values)
  (tasks/start-firstboot)
  (tasks/ui click :firstboot-forward)
  (tasks/ui click :license-yes)
  (tasks/ui click :firstboot-forward)
  (if (tasks/fbshowing? :firstboot-window "Firewall")
    (do
      (tasks/ui click :firstboot-forward)
      (tasks/ui click :firstboot-forward)
      (tasks/ui click :firstboot-forward)
      (tasks/ui click :firstboot-forward)
      (sleep 3000) ;; FIXME find a better way than a hard wait...
      (verify (tasks/fbshowing? :register-now)))))

(defn ^{Test {:groups ["firstboot"
                       "blockedByBug-973317"]}}
  firstboot_back_button_after_register
  "Verifies that on clicking backbutton after registering from Create User menu should navigte to Choose Service menu"
  [_]
  (if-not (= "RHEL5" (get-release))
    (do
      (reset_firstboot)
      (tasks/ui click :register-rhsm)
      (tasks/ui click :firstboot-forward)
      (tasks/firstboot-register (@config :username) (@config :password))
      (tasks/ui click :firstboot-forward)
      (verify (tasks/fbshowing? :firstboot-window "Create User"))
      (tasks/ui click :firstboot-back)
      (verify (not (tasks/fbshowing? :firstboot-window window-name))))))


(data-driven firstboot_register_invalid_user {Test {:groups ["firstboot"]}}
  [^{Test {:groups ["blockedByBug-703491"]}}
   (if-not (assert-skip :firstboot)
     (do
       ["sdf" "sdf" :invalid-credentials]
       ["" "" :no-username]
       ["" "password" :no-username]
       ["sdf" "" :no-password])
     (to-array-2d []))])

;; TODO: https://bugzilla.redhat.com/show_bug.cgi?id=700601

(gen-class-testng)
