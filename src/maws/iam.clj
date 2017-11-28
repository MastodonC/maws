(ns maws.iam
  (:refer-clojure :exclude [read])
  (:require [uuid :refer [uuid]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [amazonica.aws.identitymanagement :as iam]
            [amazonica.aws.securitytoken :as sts]
            [amazonica.core :refer [with-client-config with-credential ex->map]]
            [cljstache.core :refer [render-resource]]
            [aero.core :refer (read-config)]
            [clojure.java.browse :refer (browse-url)]
            [cheshire.core :as json]
            [clj-http.client :as http]
            [clojure.string :as string]))

;;
;; Account setup based functionality
;;

(defn config []
  (let [home (System/getProperty "user.home")
        config-url (str home "/.aws/etc/config.edn")]
    (read-config config-url)))

(defn nick->user-name [config nick]
  (-> config :users nick :name))

(defn account->admin-profile [config account]
  (-> config :accounts account :admin-profile))

(defn account->admin-id [config account]
  (-> config :accounts account :account-id))

(defn attach-group-policy [group-name managed-policy-name]
  (let [arn (str "arn:aws:iam::aws:policy/" managed-policy-name)]
    (println "Adding policy " arn " to " group-name)
    (iam/attach-group-policy :group-name group-name :policy-arn arn)))

(defn add-user-to-group [group-name user-name]
  (do
    (println (str "Adding user " user-name " to " group-name))
    (iam/add-user-to-group :group-name group-name :user-name user-name)))

(defn create-group [config group]
  (let [{:keys [name users managed-policy-names]} group]
    (println (str "Creating group:" name))
    (try
      (iam/create-group :group-name name)
      (catch Exception e
        (if (= (:error-code (ex->map e)) "EntityAlreadyExists")
          (println (str "Group:" name " already exists, skipping"))
          (throw e))))
    (run! (partial attach-group-policy name) managed-policy-names)
    (run! (partial add-user-to-group name) (map (partial nick->user-name config) (flatten users)))))

(defn create-account-groups [config account-group]
  (let [profile (account->admin-profile config (key account-group))
        groups (val account-group)]
    (with-credential {:profile profile}
                     (run! (partial create-group config) groups))))

(defn create-user [user password]
  "Use this in the REPL to create a user that doesn't already exist"
  (let [profile (account->admin-profile (config) :mastodonc)]
    (with-credential {:profile profile}
                     (iam/create-user :user-name user)
                     (pprint/pprint (iam/create-access-key :user-name user))
                     (iam/create-login-profile :user-name user
                                               :password password
                                               :password-reset-required true)
                     (println (str "Console URL:"
                                   "  https://mastodonc.signin.aws.amazon.com/console"
                                   "Ask the user to create an MFA login then point them at MAWS"
                                   "  https://github.com/MastodonC/maws#client-installation"
                                   "  https://github.com/MastodonC/maws-etc/blob/master/client.edn")))))

(defn create-groups []
  "Use this in the REPL to create or update the groups to which users belong"
  (let [config (config)]
    (let [account-groups (dissoc (config :groups) :global-groups)]
      (run! (partial create-account-groups config) account-groups))))

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
                        (println (str "https://signin.aws.amazon.com/switchrole?account="
                                      account "&roleName=" role-name))))
                    roles))) account-roles)))

;;
;; Client functionality
;;

(defn federated-config []
  (-> (System/getProperty "user.home")
      (str "/.aws/etc/client.edn")
      (read-config)))

(defn get-credentials [config account type & mfa]
  (let [{:keys [user trusted-profile trusted-account-id
                trusted-role-readonly trusted-role-admin account-ids]} config
        trusted-role (case type
                       "ro" trusted-role-readonly
                       "admin" trusted-role-admin
                       :else (println (str "Unknown role type: " type)))
        account-id (account-ids (keyword account))
        role-arn (str "arn:aws:iam::" account-id ":role/" trusted-role)
        mfa-device-serial-number (str "arn:aws:iam::" trusted-account-id ":mfa/" user)
        mfa-token (first mfa)
        ar (with-credential {:profile trusted-profile}
                            (if mfa-token
                              (sts/assume-role :role-arn role-arn :role-session-name account
                                               :serial-number mfa-device-serial-number :token-code mfa-token)
                              (sts/assume-role :role-arn role-arn :role-session-name account)))
        credentials (ar :credentials)]
    credentials))

(defn generate-console-url [credentials]
  (let [{:keys [access-key secret-key session-token expiration]} credentials
        session {:sessionId    access-key
                 :sessionKey   secret-key
                 :sessionToken session-token}
        json-session (json/generate-string session)
        signin-query-params {:Action          "getSigninToken"
                             :SessionDuration 43200         ;; 12 hours for console
                             :Session         json-session}
        signin-url "https://signin.aws.amazon.com/federation"
        signin-response (http/get signin-url {:query-params signin-query-params})
        signin-token ((json/parse-string (signin-response :body) true) :SigninToken)
        request-query-params {:Action      "login"
                              :Issuer      ""
                              :Destination "https://console.aws.amazon.com/"
                              :SigninToken signin-token}
        request-query-string (http/generate-query-string request-query-params)
        request-url (str "https://signin.aws.amazon.com/federation?" request-query-string)]
    request-url))

(defn open-browser [account type & mfa]
  (-> (federated-config)
      (get-credentials account type (first mfa))
      (generate-console-url)
      (browse-url)))

(defn display-console-url [account type & mfa]
  (-> (federated-config)
      (get-credentials account type (first mfa))
      (generate-console-url)
      (println)))

(defn generate-env-values
  "The output of this will be wrapped by eval on the shell: eval `maws env -a witan-prod -t admin`"
  [account type & mfa]
  (-> (federated-config)
      (get-credentials account type (first mfa))
      (as-> credentials
            (let [{:keys [access-key secret-key session-token expiration]} credentials]
              (str "export AWS_ACCESS_KEY_ID=" access-key ";\n"
                   "export AWS_SECRET_ACCESS_KEY=" secret-key ";\n"
                   "export AWS_SESSION_TOKEN=" session-token ";\n"
                   "export AWS_SECURITY_TOKEN=" session-token ";\n")))
      (println)))

(defn generate-alias-values
  "The output can be pasted into your profile to provide handy shortcuts and you
  should be able to use tab completion on aliases note: probably a bit too
  expensive to eval within a .bashrc script on startup"
  []
  (let [accounts (-> (federated-config)
                     (:account-ids)
                     (keys))]
    (-> (map (comp #(str "alias console-" % "='maws console -a " % "';\n"
                         "alias admin-console-" % "='maws console-admin -a " % " -m ';\n"
                         "alias env-" % "='maws env -a " % "';\n"
                         "alias admin-env-" % "='maws env-admin -a " % " -m ';\n")
                   name
                   ) accounts)
        (string/join)
        (println))))
