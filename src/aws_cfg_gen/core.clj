(ns aws-cfg-gen.core
  (:gen-class)
  (:require [aws-cfg-gen.sub-accounts :as sa]
            [aws-cfg-gen.cli-plus :refer [create-cli-parser parse-opts+]]))

;; generate action
;;
(def generate-cli-options
  [["-h" "--help"]])

;; action option handlers should return any unused or global options
;; so that they may pass through.
(defn generate-cli-handler [options]
  options)

(def generate-cli-options+
  {:required-options #{}
   :actions {}
   :options-fn generate-cli-handler})

(def generate-parser (create-cli-parser generate-cli-options generate-cli-options+))

;; add-acccount action
;;
(def add-account-cli-options
  [["-n" "--name NAME" "Name of AWS account"]
   ["-i" "--id ID" "ID of AWS account"]
   ["-h" "--help"]])

(defn add-account-cli-handler [options]
  (do (print "Adding this:\n")
      (sa/new (:name options) (:id options))))

(def add-account-cli-options+
  {:required-options #{:name :id}
   :actions {}
   :options-fn add-account-cli-handler})

(def add-account-parser (create-cli-parser add-account-cli-options add-account-cli-options+))

;; display-acccount action
;;
(def display-cli-options
  [["-h" "--help"]])

(defn display-cli-handler [options]
  (sa/display sa/config-url))

(def display-cli-options+
  {:required-options #{}
   :actions {}
   :options-fn display-cli-handler})

(def display-parser (create-cli-parser display-cli-options display-cli-options+))

;; Top-level config or main
;;
(def main-cli-options
  [["-h" "--help"]])

(defn main-cli-handler [options]
  options)

(def main-cli-options+
  {:required-options #{}
   :actions {:generate generate-parser
             :add-account add-account-parser
             :display display-parser}
   :options-fn main-cli-handler})

(def main-parser (create-cli-parser main-cli-options main-cli-options+))

(defn -main [ & args ]
  (apply main-parser {} args))
