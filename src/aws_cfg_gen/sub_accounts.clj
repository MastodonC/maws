(ns aws-cfg-gen.sub-accounts
  (:refer-clojure :exclude [read])
  (:require [uuid :refer [uuid]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [amazonica.aws.identitymanagement :as iam]
            [amazonica.core :refer [with-client-config]]
            [cljstache.core :refer [render-resource]]))

(def home
  (System/getProperty "user.home"))

(def config-path (str home "/.aws/"))
(def config-url (str config-path "sub-accounts.edn"))

(defn read
  "Read sub-account data into a sorted map"
  [url]
  (-> url
      slurp
      edn/read-string
      (->> (into (sorted-map)))))

(defn display
  [url]
  (-> url
      read
      pprint/pprint))

(defn write
  "Writes sub-account info to sa-config-url"
  [sa]
  (-> sa
      (pprint/write :stream nil)
      (->> (spit config-url))))

(defn new
  "Create a new sa hash"
  [name id]
  (pprint/pprint
   {(keyword name) {:external_id_ro (uuid)
                    :external_id_rw (uuid)
                    :id id}}))

;; (defmacro with-aws-config
;;   "Bug in amazonica.core/with-client-config define our own here."
;;   [config & body]
;;   `(binding [amazonica.core/*client-config* ~config]
;;      (do ~@body)))

(defn create-internal-federated-role
  [aws-profile role-name trusted-account-id]
  (let [assume-role-policy-document (render-resource "templates/assume-role-policy" {:trusted-account-id trusted-account-id}) ]
    (iam/create-role {:profile aws-profile} :role-name role-name
                     :assume-role-policy-document assume-role-policy-document)
    (iam/attach-role-policy {:profile aws-profile} :role-name role-name
                            :policy-arn "arn:aws:iam::aws:policy/ReadOnlyAccess")))

;;(create-internal-federated-role "mc-ops-sandpit" "MastodoncReadOnly" "165664414043")
