(ns lhrb.fuf
  (:gen-class)
  (:import (org.jsoup Jsoup))
  (:require [clojure.string :as str]))

(defn greet
  "Callable entry point to the application."
  [data]
  (println (str "Hello, " (or (:name data) "World") "!")))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (greet {:name (first args)}))
