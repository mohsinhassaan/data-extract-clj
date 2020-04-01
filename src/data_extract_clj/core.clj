(ns data-extract-clj.core
  (:gen-class)
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.tools.cli :as cli]
            [cheshire.core :as cheshire]
            [clojure.string :as str]
            [dk.ative.docjure.spreadsheet :as dk]
            [clojure.core.async :as async :refer [>!! <!! chan thread]]
            [clojure.java.io :as io]))

(defn load-queries
  [file]
  (->> (str/split (slurp file) #"\n\n")
       (map #(str/split % #"\n"))
       (map (fn [[query title lim]] [query title (Integer/parseInt lim)]))
       (remove empty?)))

(defn write-result
  [result title lim out-folder]
  (loop [heading (first result)
         result (rest result)
         n 1]
    (if (empty? result) nil
                        (let [wb (dk/create-workbook title (cons heading (take lim result)))]
                         (dk/save-workbook! (str out-folder "/" title "-" n ".xlsx") wb)
                         (recur heading (drop lim result) (inc n))))))

(def cli-opts
  [["-n" "--max-calls MAX" "Max number of parallel db calls"
    :default 3]
   ["-o" "--output FOLDER" "Output Folder"
    :default "output"]
   ["-h" "--help"]])

(defn usage [options-summary]
  (->> ["Takes SQL queries from file and writes result as xlsx files"
        ""
        "Usage: java -jar data-extract-clj-1.0.0-standalone.jar [options] [input-file] [credentials-file]"
        ""
        "Options:"
        options-summary]
       (str/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

(defn validate-args
  [args]
  (let [{:keys [options arguments errors summary]} (cli/parse-opts args cli-opts)]
    (cond
      (:help options) {:exit-message (usage summary) :ok? true}
      errors {:exit-message (error-msg errors)}
      (= 2 (count arguments)) {:in-file (first arguments) :connection (second arguments) :options options}
      :else {:exit-message (usage summary)})))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn -main
  [& args]
  (let [{:keys [in-file connection options exit-message ok?]} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
    (let [db (cheshire/parse-string (slurp connection) true)
          queries (load-queries in-file)
          n (count queries)
          {:keys [max-calls output]} options
          result-chan (chan max-calls)]
      (doseq [[query title lim] queries]
            (thread (>!! result-chan [(jdbc/query db [query] {:keywordize? false
                                                              :as-arrays?  true
                                                              :result-set-fn (fn [rs]
                                                                               (doall (map #(map str %) rs)))})
                                      title lim output])))
      (if (not (.isDirectory (io/file output))) (.mkdir (io/file output)))
      (dotimes [_ n]
            (apply write-result (<!! result-chan)))))))