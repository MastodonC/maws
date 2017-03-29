(ns aws-cfg-gen.sub-accounts
  (:refer-clojure :exclude [read])
  (:require [uuid :refer [uuid]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [amazonica.aws.identitymanagement :as iam]
            [amazonica.core :refer [with-client-config]]
            [cljstache.core :refer [render-resource]]
            [aero.core :refer (read-config)]))

(defn config []
  (let [home (System/getProperty "user.home")
        config-url (str home "/.aws/etc/config.edn")]
    (read-config config-url)))

(defmacro with-profile
  "Per invocation binding of credentials based on profile"
  [profile & body]
  `(binding [amazonica.core/*credentials* {:profile ~profile}]
     (do ~@body)))

(defn attach-group-policy [group-name managed-policy-name]
  (let [arn (str "arn:aws:iam::aws:policy/" managed-policy-name)]
    (iam/attach-group-policy :group-name group-name :policy-arn arn)))

(defn add-user-to-group [group-name nick]
  (let [user-name ((nick->user nick) :name)]
    (println "Adding " user-name " to " group-name)
    (iam/add-user-to-group :group-name group-name :user-name user-name)
    ))

(defn create-group [group]
  (let [{:keys [name users managed-policy-names]} group]
    ;;(iam/create-group :group-name name)
    (run! (partial attach-group-policy name) managed-policy-names)
    ;;(run! (partial add-user-to-group name) (flatten users))
    ))

(defn create-account-groups [account-groups]
  (let [profile ((account->profile (key account-groups)) :admin-profile)
        groups (val account-groups)]
    (with-profile profile
      (run! create-group groups))))

(defn create-groups [config]
  (let [account-groups (dissoc (config :groups) :global-groups)
        account->profile (config :accounts)
        nick->user (config :users)]
    (run! create-account-groups account-groups)))

(defn create-internal-federated-role
  [aws-admin-profile role-name trusted-account-id]
  (let [assume-role-policy-document (render-resource "templates/assume-role-policy" {:trusted-account-id trusted-account-id}) ]
    (with-profile aws-admin-profile
      (iam/create-role :role-name role-name
                       :assume-role-policy-document assume-role-policy-document)
      (iam/attach-role-policy :role-name role-name
                              :policy-arn "arn:aws:iam::aws:policy/ReadOnlyAccess"))))

;; (defn read
;;   "Read sub-account data into a sorted map"
;;   [url]
;;   (-> url
;;       slurp
;;       edn/read-string
;;       (->> (into (sorted-map)))))

;; (defn display
;;   [url]
;;   (-> url
;;       read
;;       pprint/pprint))

;; (defn write
;;   "Writes sub-account info to sa-config-url"
;;   [sa]
;;   (-> sa
;;       (pprint/write :stream nil)
;;       (->> (spit config-url))))

;; (defn new
;;   "Create a new sa hash"
;;   [name id]
;;   (pprint/pprint
;;    {(keyword name) {:external_id_ro (uuid)
;;                     :external_id_rw (uuid)
;;                     :id id}}))
