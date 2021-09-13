(ns core
  (:require [clojure.core.async :refer [>! <! >!! <!! go go-loop chan close! timeout]]
            [org.httpkit.client :as http]
            [clojure.string]))

(defn random-response
  "Write the given response to the output channel after a
   random number of milliseconds (<= 10s)"
  [out resp]
  (let [ms (-> (rand-int 10)
               (inc)
               (* 1000))]
    (go (<! (timeout ms))
        (>! out resp))))

(defn thread-race
  "Start n threads and print the response of whoever is first."
  [n]
  (let [in (chan)]
    (dotimes [thread-id n]
      (random-response in (str "Hi I'm Thread " thread-id)))
    (println (str "Got response: " (<!! in)))
    (close! in)))

(def word-counts (atom {}))

(defn random-lorem-ipsum
  "Write a random length lorem ipsum to the output channel
   using GET request to loripsum.net API."
  [out]
  (let [n (inc (rand-int 100))]
    (http/get (str "https://loripsum.net/api/" n "/long/plaintext")
              (fn [{:keys [body error]}]
                (when-not error
                  (>!! out body))))))

(defn word-frequencies
  [s]
  (->> (clojure.string/split s #"\s+")
       (frequencies)))

(defn multi-threaded-word-frequencies!
  "Start n threads to fetch random lorem ipsums and then merge the 
   results using channels and aggregate them into the word-count atom."
  [n]
  (let [in (chan)]
    (go-loop []
      (let [txt (<! in)]
        (println (str "Aggregator thread got string of length: " (count txt)))
        (swap! word-counts (fn [state]
                             (merge-with + state (word-frequencies txt))))
        (recur)))
    (dotimes [_ n]
      (random-lorem-ipsum in))))

(comment
  (multi-threaded-word-frequencies! 10)
  @word-counts

  (thread-race 10)
  )