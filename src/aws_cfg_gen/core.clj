(ns aws-cfg-gen.core
  (:gen-class)
  (:require [aws-cfg-gen.sub-accounts :as sa]
            [aws-cfg-gen.cli-plus :refer [create-cli-handler parse-opts+]]))

;; generate action
;;
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
;;
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
      sa/new (:name options) (:id options)))

(def add-account (create-cli-handler add-account-cli-options add-account-cli-options+ add-account-handler))

;; display-acccount action
;;
(def display-cli-options
  [["-h" "--help"]])

(def display-cli-options+
  {:required-options #{}
   :actions {}})

(defn display-handler
  [options]
  (sa/display sa/config-url))

(def display (create-cli-handler display-cli-options display-cli-options+ display-handler))

;; Top-level config or main
;;
(def main-cli-options
  [["-h" "--help"]])

(def main-cli-options+
  {:required-options #{}
   :actions {:generate generate
             :add-account add-account
             :display display}})

(def -main (create-cli-handler main-cli-options main-cli-options+))
