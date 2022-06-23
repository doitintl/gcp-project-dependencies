(ns ferent.utils
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [com.climate.claypoole :as cp])
  (:import (java.io IOException PushbackReader)))

(def thread-count
  "Highly IO-bound processes should get more threads than we have processors."
  (* 3  (.. Runtime getRuntime availableProcessors)))

(defn pairs-to-multimap [seq-of-pairs]
  (let [grouped-pairs-by-key (group-by first (sort seq-of-pairs))
        vec-pairs-of-key-and-valuelist (for [[k v] grouped-pairs-by-key] [k (sort (map second v))])
        as-map (into {} vec-pairs-of-key-and-valuelist)]
    (into {} (for [[k vs] as-map] [k (set vs)]))))

(defn invert-multimap [multimap]
  (let [inverse (for [[k lst] multimap] (for [i lst] [i k]))
        flattened (apply concat inverse)]
    (pairs-to-multimap flattened)))

(defn invert-invertible-map [multimap]
  (let [inverse-multimap (invert-multimap multimap)]
    (assert (every? #(>= 1 (count %)) (vals inverse-multimap))
            (str "Each value should be associated with 1 and only 1 key. Duplicates: "
                 (remove #(>= 1 (count %)) (vals inverse-multimap))))

    (into {} (for [[k v] inverse-multimap] [k (first v)]))))

(defn twolevel-sort [lst-lst]
  (sort (map (comp vec sort) lst-lst)))

(defn rotate-to-lowest [lst]
  "Rotate lst so that the 0th element is (the first appearance of) its minimal element"
  (let [indexed (map-indexed vector lst)
        minimal-element (reduce (fn [a b] (if (>= 0 (compare (second a) (second b))) a b)) indexed)]
    (vec (take (count lst) (drop (first minimal-element) (cycle lst))))))

(defn load-edn
  "Load edn from an io/reader source (filename or io/resource)."
  [source]
  (try
    (with-open [r (io/reader source)]
      (edn/read (PushbackReader. r)))
    (catch IOException e
      (throw (Exception. (str "Couldn't open " (.getMessage e)))))
    (catch RuntimeException e
      (throw (Exception. (str "Error parsing edn file " (.getMessage e)))))))

(defn pfilter [pred coll]
  (map first (filter second (map vector coll (cp/pmap (cp/threadpool thread-count) pred coll)))))

(defn- get-env-int ([key default]
                    (let [val (System/getenv key)]
                      (if (and (= default :required) (nil? val))
                        (throw (AssertionError. (str "Must provide a value for env variable " key)))
                        (let [retval (or val default)]
                          (do
                            (.println *err* (str  key ": " retval))
                            retval)))))
  ([key] (get-env-int key nil)))

;avoid printing each time
(def  get-env (memoize get-env-int))


