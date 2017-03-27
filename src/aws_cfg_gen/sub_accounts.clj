(ns aws-cfg-gen.sub-accounts
  (:refer-clojure :exclude [read])
  (:require [uuid :refer [uuid]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]))

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
