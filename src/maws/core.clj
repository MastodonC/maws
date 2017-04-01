(ns maws.core
  (:gen-class)
  (:require [maws.iam :as iam]
            [maws.cli-plus :refer [create-cli-parser parse-opts+]]))

;; console options
(def console-cli-options
  [["-a" "--account ACCOUNT"]
   ["-d" "--display-url"]
   ["-h" "--help"]])

(defn console-cli-handler [options]
  (let [{:keys [account display-url]} options]
    (if display-url
      (iam/display-console-url account "ro")
      (iam/open-browser account "ro"))))

(def console-cli-options+
  {:required-options #{:account}
   :actions {}
   :options-fn console-cli-handler})

(def console-parser (create-cli-parser console-cli-options console-cli-options+))

;; console-admin options
(def console-admin-cli-options
  [["-a" "--account ACCOUNT"]
   ["-m" "--mfa KEY"]
   ["-d" "--display-url"]
   ["-h" "--help"]])

(defn console-admin-cli-handler [options]
  (let [{:keys [account display-url mfa]} options]
    (if display-url
      (iam/display-console-url account "admin" mfa)
      (iam/open-browser account "admin" mfa))))

(def console-admin-cli-options+
  {:required-options #{:account :mfa}
   :actions {}
   :options-fn console-admin-cli-handler})

(def console-admin-parser (create-cli-parser console-admin-cli-options console-admin-cli-options+))

;; env options
(def env-cli-options
  [["-a" "--account ACCOUNT"]
   ["-h" "--help"]])

(defn env-cli-handler [options]
  (let [{:keys [account]} options]
    (iam/generate-env-values account "ro")))

(def env-cli-options+
  {:required-options #{:account}
   :actions {}
   :options-fn env-cli-handler})

(def env-parser (create-cli-parser env-cli-options env-cli-options+))

;; env-admin options
(def env-admin-cli-options
  [["-a" "--account ACCOUNT"]
   ["-m" "--mfa KEY"]
   ["-h" "--help"]])

(defn env-admin-cli-handler [options]
  (let [{:keys [account type mfa]} options]
    (iam/generate-env-values account "admin" mfa)))

(def env-admin-cli-options+
  {:required-options #{:account :mfa}
   :actions {}
   :options-fn env-admin-cli-handler})

(def env-admin-parser (create-cli-parser env-admin-cli-options env-admin-cli-options+))

;; aliases options
(def aliases-cli-options
  [])

(defn aliases-cli-handler [options]
  (iam/generate-alias-values))

(def aliases-cli-options+
  {:required-options #{}
   :actions {}
   :options-fn aliases-cli-handler})

(def aliases-parser (create-cli-parser aliases-cli-options aliases-cli-options+))

;; Top-level config or main
;;

(def main-cli-options
  [["-h" "--help"]])

(defn main-cli-handler [options]
  options)

(def main-cli-options+
  {:required-options #{}
   :actions {:console console-parser
             :console-admin console-admin-parser
             :env env-parser
             :env-admin env-admin-parser
             :aliases aliases-parser
             }
   :options-fn main-cli-handler})

(def main-parser (create-cli-parser main-cli-options main-cli-options+))

(defn -main [ & args ]
  (apply main-parser {} args))
