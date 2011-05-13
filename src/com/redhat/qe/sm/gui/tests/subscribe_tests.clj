(ns com.redhat.qe.sm.gui.tests.subscribe-tests
  (:use [test-clj.testng :only (gen-class-testng data-driven)]
	      [com.redhat.qe.sm.gui.tasks.test-config :only (config)]
        [com.redhat.qe.verify :only (verify)]
        [error.handler :only (with-handlers handle ignore recover)]
	       gnome.ldtp)
  (:require [com.redhat.qe.sm.gui.tasks.tasks :as tasks]
             com.redhat.qe.sm.gui.tasks.ui)
  (:import [org.testng.annotations BeforeClass BeforeGroups Test]))

(defn ^{BeforeClass {:groups ["setup"]}}
  register [_]
  (with-handlers [(handle :already-registered [e]
                               (recover e :unregister-first))]
    (tasks/register (@config :username) (@config :password))))

(comment 
(defn subscribe_each [subscription]
  (tasks/search {})
  (with-handlers [(ignore :subsription-not-available)
                  (handle :wrong-consumer-type [e]
                  (recover e :log-warning))]
    (tasks/subscribe subscription)))
)

(defn ^{Test {:groups ["subscribe"]}}
  subscribe_all [_]
  (tasks/search {})
  (tasks/do-to-all-rows-in :all-subscriptions-view 0
                  (fn [subscription]
                    (with-handlers [(ignore :subscription-not-available)
                                    (handle :wrong-consumer-type [e]
                                                 (recover e :log-warning))]
                      (tasks/subscribe subscription)))))

(defn ^{Test {:groups ["subscribe"]
              :dependsOnMethods [ "subscribe_all"]}}
  unsubscribe_all [_]
  (tasks/ui selecttab :my-subscriptions)
  (tasks/do-to-all-rows-in :my-subscriptions-view 0
                     (fn [subscription] (with-handlers [(ignore :not-subscribed)]
                                         (tasks/unsubscribe subscription)
                                         (verify (= (tasks/ui rowexist? :my-subscriptions-view subscription) false))))))



(comment
;; https://bugzilla.redhat.com/show_bug.cgi?id=679961                                         
(defn ^{Test {:groups ["subscribe"]}}
  check_unsubscribe_clear [_]
  (tasks/search {})
  (let [subscripton (tasks/ui getcellvalue :all-subscriptions-view 0 0)]
    ;;add error handler?
    (tasks/subscribe subscritpion))
  (tasks/ui selecttab :my-subscriptions)
  (let [subscripton (tasks/ui getcellvalue :my-subscriptions-view 0 0)]
    ;;verify that info field populated
    (tasks/unsubscribe subscription)
    ;;verify that info field cleared
    )
)

)
;; TODO https://bugzilla.redhat.com/show_bug.cgi?id=683550
;; TODO https://bugzilla.redhat.com/show_bug.cgi?id=691784
;; TODO https://bugzilla.redhat.com/show_bug.cgi?id=691788
;; TODO https://bugzilla.redhat.com/show_bug.cgi?id=703920


(gen-class-testng)
