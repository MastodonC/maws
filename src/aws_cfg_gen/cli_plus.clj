(ns aws-cfg-gen.cli-plus
  (:require [clojure.set :as set]
            [clojure.string :as string]
            [clojure.tools.cli :refer [parse-opts] :as cli]
            [clojure.test :refer [function?]]))

;; Extend parse-opts to include actions and mandatory options.
;; Also provide helper functions to create the cli parsers.
;; Define options in the normal way:
;;
;;   (def main-cli-options
;;     [["-h" "--help"]])
;;
;; Create a handler to deal with options.  The output handler should
;; be any options that will be passed to the next action
;;
;;   (defn main-cli-handler [options]
;;     ...do stuff...
;;     options
;;     )
;;
;; Create the options+
;;
;; (def main-cli-options+
;;   {:required-options #{config}
;;    :actions {:generate generate ;; futher cli parsers to call
;;              :add-account add-account
;;              :display display}
;;    :options-fn main-cli-handler})
;;
;; Then create the cli parser
;;
;; (def main-parser (create-cli-parser main-cli-options main-cli-options+))
;;
;; The parsers expect as the first argument any pre-set options. Thus
;; if we did:
;;
;; (def generate-parser (create-cli-parser generate-cli-options generate-cli-options+)
;;
;; (defn (-main [ & args]
;;    (apply main-parser {} args)
;;
;; We can call the program with:
;;
;;   program -c generate -i 2344 -n matt push --up  ...
;;

(defn parse-opts+
  "Take an action (may be nil), the set of options from parse-opts,
  and the opts+ spec to determine if the action is a valid one and all required
  options to an action are present. Generate a map for a result similar to the
  parse-opts map."
  [action options options+]
  (let [{:keys [required-options actions options-fn]} options+
        result {:action+ nil
                :action-fn+ nil
                :actions+ (set (map name (keys actions)))
                :missing-options+ nil
                :options-fn+ options-fn
                :errors+ nil
                :summary+ nil}]
    (as-> result x
        (if (not-empty action)
          (if (contains? (:actions+ x) action)
            (assoc x :action+ action)
            (assoc x :errors+
                   (into []
                         (conj (:errors+ x) (str "Invalid action: " action)))))
          x)
        (if (not-empty (x :action+))
          (if (function? (actions (keyword action)))
            (assoc x :action-fn+ (actions (keyword action)))
            (assoc x :errors+
                   (into [] (conj (x :errors+) (str "No action function to dispatch to: " action)))))
          x)
        (assoc x :missing-options+
               (into []
                     (set/difference required-options (keys options))))
        (if (not-empty (:missing-options+ x))
          (assoc x :errors+
                 (into []
                       (conj (x :errors+)
                             (str "Missing required options: "
                                  (string/join ", " (map (comp (partial str "--") name) (:missing-options+ x)))))))
          x)
        (assoc x :summary+
               (->> [(if (not-empty (:actions+ x))
                       (->> ["Actions"
                             (string/join "\n" (map #(str "  " %) (:actions+ x)))]
                            (string/join "\n"))
                       [])
                     (if (not-empty required-options)
                       (->> ["Required Options"
                             (string/join "\n" (map #(str "  --" %) (map name required-options)))
                             ""]
                            (string/join "\n"))
                       [])]
                    (flatten)
                    (string/join "\n"))))))

(defn error-msg [errors]
  (str "The following errors occurred parsing the cli:\n\n"
       (string/join \newline errors)))

(defn exit [status msg]
  (println msg)
  ;;(System/exit status)
)

(defn cli-summary
  "Display CLI summary info"
  [action summary summary+]
  (->> [(str "Usage: aws-cfg-gen [options] " (if (nil? action) "[action]" action))
        ""
        "Options:"
        summary
        ""
        summary+
        ""]
       (string/join \newline)))

(defn validate-args [cli-options cli-options+ args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options :in-order true :strict true)
        action (first arguments)
        {:keys [action+ actions+ action-fn+ options-fn+ errors+ summary+]} (parse-opts+ action options cli-options+)]
    (cond
      (:help options) {:exit-message (cli-summary action summary summary+) :ok? true}
      errors {:exit-message (error-msg errors)}
      errors+ {:exit-message (error-msg errors+)}
      (not options-fn+) {:exit-message (str "No option handler to dispatch to.")}
      (and (not action+) (not-empty actions+)) {:exit-message (cli-summary nil summary summary+)}
      :else {:action action+ :action-fn action-fn+ :options options :options-fn options-fn+ :arguments (rest arguments)})))

(defn create-cli-parser
  ([cli-options cli-options+]
   (fn [options & args]
     (let [{:keys [action-fn options options-fn exit-message ok? arguments]} (validate-args cli-options cli-options+ args)]
       (if exit-message
         (exit (if ok? 0 1) exit-message)
         (if action-fn
           (as-> options x
             (options-fn x)
             (apply action-fn x arguments))
           (options-fn options)))))))
