(ns aws-cfg-gen.core
  (:gen-class)
  (:require [uuid :refer [uuid]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [aws-cfg-gen.cli-plus :refer [create-cli-handler]]))

;; Initial CLI option config (extended by cli-plus to include actions and mandatory options)
;;

;; Top-level config or main
(def main-cli-options
  [["-h" "--help"]])

(def main-cli-options+
  {:required-options #{}
   :valid-actions #{"generate" "add-account"}})

;; generate action i.e, build the config file
(def generate-cli-options
  [["-h" "--help"]])

(def generate-cli-options+
  {:required-options #{}
   :valid-actions #{}})

;; add-acccount action
(def add-account-cli-options
  [["-n" "--name NAME" "Name of AWS account"]
   ["-i" "--id ID" "ID of AWS account"]
   ["-h" "--help"]])

(def add-account-cli-options+
  {:required-options #{:name :id}
   :valid-actions #{}})

(defn generate-handler
  [options]
  (print options))

(defn add-account-handler
  [options]
  (print options))

(def generate (create-cli-handler generate-cli-options generate-cli-options+ generate-handler))
(def add-account (create-cli-handler add-account-cli-options add-account-cli-options+ add-account-handler))
(def -main (create-cli-handler main-cli-options main-cli-options+))


;; Sub Account functions
;; sa - sub-accounts
;;

(def sa-config-path "resources")
(def sa-config-url (str sa-config-path "/sub-accounts.edn"))

(defn read-sa
  "Read sub-account data into a sorted map"
  [url]
  (-> url
      slurp
      edn/read-string
      (->> (into (sorted-map)))))

(defn write-sa
  "Writes sub-account info to sa-config-url"
  [sa]
  (-> sa
      (pprint/write :stream nil)
      (->> (spit sa-config-url))))

(defn new-sa
  "Create a new sa hash"
  [name id]
  {(keyword name) {
                   :external_id_ro (uuid)
                   :external_id_rw (uuid)
                   :id id}})
