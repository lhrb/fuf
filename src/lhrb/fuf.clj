(ns lhrb.fuf
  (:gen-class)
  (:import (org.jsoup Jsoup)
           (java.time LocalDate)
           (java.time LocalDateTime)
           (java.time.format DateTimeFormatter))
  (:require [clojure.string :as str]
            [hiccup.core :refer [html]]))

(defn greet
  "Callable entry point to the application."
  [data]
  (println (str "Hello, " (or (:name data) "World") "!")))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (greet {:name (first args)}))

(defn episode->html [{:keys [episode/title
                             episode/date
                             episode/url
                             episode/nr]}]
  (let [nr-str (str "#" nr " ")
        filename (str "fuf" nr ".mp3")]
   [:div {:id nr}
    [:strong nr-str title]
    [:table {:class "u-full-width"}
     [:tbody
      [:tr
       [:td [:em date]]
       [:td {:align "right"}
        [:a {:href url :download filename} "Download"]]]]]]))

(defn build-page [episodes]
  (html [:html {:lang "de"}
        [:head
         [:meta {:charset "UTF-8"}]
         [:meta {:name "viewport"
                 :content "width=device-width, initial-scale=1"}]
         [:link {:rel "stylesheet" :href "css/normalize.css"}]
         [:link {:rel "stylesheet" :href "css/skeleton.css"}]
         [:link {:rel "stylesheet" :href "css/custom.css"}]
         [:title "Sanft & Sorgfältig - Alle Folgen"]]
         [:body
          [:div {:class "container"}
           [:section {:class "header"}
            [:h2 {:class "title"} "Sanft & Sorgfältig - alle Folgen"]]
           (map episode->html episodes)]]]))


(comment

  (spit "resources/public/index.html" (build-page episodes))
  (count episodes)

  (def episodes
    (clojure.edn/read-string (slurp "resources/episodes-clean.edn")))

  (def e (first episodes))

  ;; get episode link used for data cleaning
  (->> episodes
       (map :episode/url)
       (map #(str/split % #"/"))
       (map drop-last)
       (map #(str/join "/" %))
       distinct)

  (defn podcast-link? [s]
    (or
     (str/starts-with? s "http://media.rbb-online.de/rad/podcast/zwei_alte_hasen")
     (str/starts-with? s "http://download.radioeins.de/podcast/zwei_alte_hasen")))

  (->> episodes
       (filter #(podcast-link? (:episode/url %))))

  (defn format-date
    "sort by date and format"
    [episodes]
    (let [df (DateTimeFormatter/ofPattern "EEE, dd MMM yyyy HH:mm:ss")
          rm-tz (fn [d] (->> d (drop-last 5) (str/join)))
          toDate (fn [d] (-> (rm-tz d) (LocalDateTime/parse df)))
          ;; date back to string
          target-df (DateTimeFormatter/ofPattern "EEE, dd MMM yyyy")
          ]
      (->> episodes
           (map (fn [m] (update m :episode/date toDate)))
           (sort-by :episode/date #(.isBefore %1 %2))
           (map (fn [m] (update m :episode/date #(.format % target-df)))))))

  (defn to-rm [eps]
   (->> (group-by :episode/title eps)
        (filter (fn [[k v]] (> (count v) 1)))
        (map (fn [[k v]] v))
        (flatten)
        (filter (fn [v] (str/starts-with? (:episode/url v) "http://download.radio")))
        #_(into #{})))

  (defn distinct-by [f coll]
    (let [groups (group-by f coll)]
      (map #(first (groups %)) (distinct (map f coll)))))


  (->> (to-rm eps)
       (map :episode/url))
  (def to-rm' (to-rm eps))

  (def eps (clojure.edn/read-string (slurp "resources/episodes.edn")))

  (def s (:episode/url (first eps)))

  (def df (DateTimeFormatter/ofPattern "yyyyMMdd"))
  (def tdf (DateTimeFormatter/ofPattern "EEE, dd MMM yyyy"))

  (defn get-date-from-url [eps df]
    (-> (:episode/url eps)
        (str/split #"/")
        last
        (subs 3 11)
        (LocalDate/parse df)))

  (def eps
   (->> eps
        (filter #(podcast-link? (:episode/url %)))
        (map (fn [e] (assoc e :episode/date (get-date-from-url e df))))
        (distinct-by :episode/date)
        (sort-by :episode/date #(.isBefore %1 %2))
        (map (fn [e] (update e :episode/date #(.format % tdf))))))

  (def eps
    (format-date
     (->> (clojure.edn/read-string (slurp "resources/episodes.edn"))
          (filter #(podcast-link? (:episode/url %)))
          (distinct)
          ;(remove to-rm')
          )))


  (def eps
    ;;add nr
    (map (fn [e n] (assoc e :episode/nr n)) eps (range 1 (+ 1 (count eps)))))

  (with-open [w (clojure.java.io/writer "resources/episodes-clean.edn")]
    (binding [*print-length* false
              *out* w]
      (pr eps)))

  ,)
