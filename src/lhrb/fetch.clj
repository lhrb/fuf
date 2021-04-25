(ns lhrb.fetch
  (:gen-class)
  (:import (org.jsoup Jsoup))
  (:require [clojure.string :as str]))

(defn get-page [url]
  (let [time (+ 5000 (rand-int 5000))]
   (do
     (println (str "sleep for " time "ms"))
     (Thread/sleep time)
     (.get (Jsoup/connect url)))))

(defn gen-urls
  "all fuf urls so far, maybe some won't work let's see"
  []
  (let [f "https://sanft3.rssing.com/chan-9291954/article"
        e "-live.html"]
    (for [x (range 1 270)]
      (str f x e))))

(defn get-iframe-url
  "since the actual content is wrapped we need
  to get the inner iframe url"
  [url]
  (str "http:"
   (-> (get-page url)
       (.select "iframe")
       (.attr "src"))))

(defn get-relevant-content
  [url]
  (-> (get-page url)
      (.select "ul")
      (.first)
      (.select "li")
      (.toString)
      (str/split-lines)))

(defn parse-txt
  "example
  in: <li><strong>Title:</strong> Zwei alte Hasen und das Verbrechen in Berlin</li>
  out: Zwei alte Hasen und das Verbrechen in Berlin"
  [elem]
  (-> elem
      (str/split #"</strong>")
      second
      (str/split #"</li>")
      first
      (str/trim)))

(defn parse-url [elem]
  (-> elem
      (str/split #"href=\"")
      second
      (str/split #"\">")
      first
      (str/trim)))

(defn create-entry [content]
  (let [parse (fn [x] (parse-txt (nth content x)))]
    {:episode/title (parse 0)
     :episode/date (parse 1)
     :episode/url (parse-url (nth content 3))}))

(defn entry->file [entry filename]
  (spit filename entry :append true))

(defn url->entry [url filename]
  (try
    (-> url
        (get-iframe-url)
        (get-relevant-content)
        (create-entry)
        (entry->file filename))
    (catch Exception ex (.printStackTrace ex))))

(comment

  (def filename "resources/fetchedEntries")
  ;; parse all sites
  (doall
   (->> (gen-urls)
        (map (fn [url] (url->entry url filename)))))

  (def url "https://sanft3.rssing.com/chan-9291954/article1-live.html")

  (def fuf (clojure.edn/read-string (str "["(slurp filename) "]")))

  (def cnt ["<li><strong>Title:</strong> Zwei alte Hasen und das Verbrechen in Berlin</li>"
  "<li><strong>Publish Date:</strong> Sun, 25 Mar 2012 16:00:00 0200</li>"
  "<li><strong>Author:</strong> Not Available</li>"
  "<li><strong>URL:</strong> <a href=\"http://download.radioeins.de/podcast/zwei_alte_hasen/zh_20120325_160000.mp3\">http://download.radioeins.de/podcast/zwei_alte_hasen/zh_20120325_160000.mp3</a></li>"
            "<li><strong>Article Unique Identifier:</strong> Not Available</li>"])

  (create-entry cnt)
  ,)
