(ns aws-cfg-gen.sub-accounts
  (:refer-clojure :exclude [read])
  (:require [uuid :refer [uuid]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [amazonica.aws.identitymanagement :as iam]
            [amazonica.aws.securitytoken :as sts]
            [amazonica.core :refer [with-client-config with-credential]]
            [cljstache.core :refer [render-resource]]
            [aero.core :refer (read-config)]))

(defn config []
  (let [home (System/getProperty "user.home")
        config-url (str home "/.aws/etc/config.edn")]
    (read-config config-url)))

(defn nick->user-name [config nick]
  (((config :users) nick) :name))

(defn account->admin-profile [config account]
  (((config :accounts) account) :admin-profile))

(defn account->admin-id [config account]
  (((config :accounts) account) :account-id))

(defn attach-group-policy [group-name managed-policy-name]
  (let [arn (str "arn:aws:iam::aws:policy/" managed-policy-name)]
    (println "Adding policy " arn " to " group-name)
    (iam/attach-group-policy :group-name group-name :policy-arn arn)))

(defn add-user-to-group [group-name user-name]
  (do
    (println (str "Adding user " user-name " to " group-name))
    (iam/add-user-to-group :group-name group-name :user-name user-name)
    ))

(defn create-group [config group]
  (let [{:keys [name users managed-policy-names]} group]
    (println (str "Creating group:" name))
    (iam/create-group :group-name name)
    (run! (partial attach-group-policy name) managed-policy-names)
    (run! (partial add-user-to-group name) (map (partial nick->user-name config) (flatten users)))
    ))

(defn create-account-groups [config account-group]
  (let [profile (account->admin-profile config (key account-group))
        groups (val account-group)]
    (with-credential {:profile profile}
      (run! (partial create-group config) groups))))

(defn create-groups [config]
  (let [account-groups (dissoc (config :groups) :global-groups)]
    (run! (partial create-account-groups config) account-groups)))

(defn attach-role-policy [role-name managed-policy-name]
  (let [arn (str "arn:aws:iam::aws:policy/" managed-policy-name)]
    (println (str "Adding policy " arn " to " role-name))
    (iam/attach-role-policy :role-name role-name :policy-arn arn)))

(defn create-role [config role]
  (let [{:keys [name trusted-account managed-policy-names assume-role-policy-template]} role
        trusted-account-id (account->admin-id config trusted-account)
        assume-role-policy-document (render-resource (str "templates/" assume-role-policy-template) {:trusted-account-id trusted-account-id})]
    (println (str "Creating role:" name))
    (iam/create-role :role-name name
                     :assume-role-policy-document assume-role-policy-document)
    (run! (partial attach-role-policy name) managed-policy-names)))

(defn create-account-roles [config account-roles]
  (let [profile (account->admin-profile config (key account-roles))
        roles (val account-roles)]
    (println (str "In account: " (name (key account-roles))))
    (with-credential {:profile profile}
      (run! (partial create-role config) roles))))

(defn create-roles [config]
  (let [account-roles (config :roles)]
    (run! (partial create-account-roles config) account-roles)))

(defn construct-signin-links [config]
  ;; For now we assume the account name is also the account alias
  (let [account-roles (config :roles)]
    (run! (fn [account-role]
           (let [account (name (key account-role))
                 roles (val account-role)]
             (run! (fn [role]
                    (let [role-name (role :name)]
                      (println (str "https://signin.aws.amazon.com/switchrole?account=" account "&roleName=" role-name))))
                   roles))) account-roles)))

;; Perhaps split this out as this is the client side of things only
(defn federated-config []
  (let [home (System/getProperty "user.home")
        config-url (str home "/.aws/etc/client.edn")]
    (read-config config-url)))

(defn get-credentials [config account type]
  (let [{:keys [user trusted-profile trusted-account-id trusted-role-readonly trusted-role-admin account-ids]} config
        trusted-role (case type
                       "ro" trusted-role-readonly
                       "admin" trusted-role-admin
                       :else (println (str "Unknown role type: " type)))
        account-id (account-ids (keyword account))
        role-arn (str "arn:aws:iam::" account-id ":role/" trusted-role)]
    (with-credential {:profile trusted-profile}
      (let [ar (sts/assume-role :role-arn role-arn :role-session-name account)
            credentials (ar :credentials)
            ]
        (pprint/pprint credentials)))))
