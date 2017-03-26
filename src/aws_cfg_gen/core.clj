(ns aws-cfg-gen.core
  (:gen-class)
  (:require [uuid :refer [uuid]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [aws-cfg-gen.cli-plus :refer [create-cli-handler parse-opts+]]))

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

(defn display-sa
  [url]
  (-> url
      read-sa
      pprint/pprint))

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

;; Initial CLI option config (extended by cli-plus to include actions and mandatory options)
;;
;; generate action i.e, build the config file
(def generate-cli-options
  [["-h" "--help"]])

(def generate-cli-options+
  {:required-options #{}
   :actions {}})

(defn generate-handler
  [options]
  (print options))

(def generate (create-cli-handler generate-cli-options generate-cli-options+ generate-handler))

;; add-acccount action
(def add-account-cli-options
  [["-n" "--name NAME" "Name of AWS account"]
   ["-i" "--id ID" "ID of AWS account"]
   ["-h" "--help"]])

(def add-account-cli-options+
  {:required-options #{:name :id}
   :actions {}})

(defn add-account-handler
  [options]
  (do (print "Adding this:\n")
      new-sa (:name options) (:id options)))

(def add-account (create-cli-handler add-account-cli-options add-account-cli-options+ add-account-handler))

;; display-acccount action
(def display-cli-options
  [["-h" "--help"]])

(def display-cli-options+
  {:required-options #{}
   :actions {}})

(defn display-handler
  [options]
  (display-sa sa-config-url))

(def display (create-cli-handler display-cli-options display-cli-options+ display-handler))

;; Top-level config or main
(def main-cli-options
  [["-h" "--help"]])

(def main-cli-options+
  {:required-options #{}
   :actions {:generate generate
             :add-account add-account
             :display display}})

(def -main (create-cli-handler main-cli-options main-cli-options+))
