(ns me.lomin.alive.selectors
  (:require [com.rpl.specter :as specter]
            [clojure.string :as string]))

(def walker
  (specter/recursive-path [path]
                          p
                          (specter/cond-path sequential?
                                             (specter/if-path path
                                                              (specter/continue-then-stay specter/ALL p)
                                                              [specter/ALL p])
                                             map?
                                             (specter/if-path path
                                                              (specter/continue-then-stay specter/MAP-VALS p)
                                                              [specter/MAP-VALS p]))))

(defmulti keyword-selector (fn [k] (keyword (namespace k))) :default ::none)

(defmethod keyword-selector ::none [k] [identity k])
(defmethod keyword-selector :must [k] [walker (specter/must (keyword (name k)))])

(defn tag= [t]
  (specter/comp-paths seqable? (specter/selected? [specter/FIRST (specter/pred= t)])))

(defmethod keyword-selector nil [k]
  [walker
   (tag= k)])

(defmethod keyword-selector :. [k]
  [walker
   [seqable? #(if-let [class-str (:class (second %))]
                (some #{(name k)}
                      (string/split class-str #" ")))]])

(defmethod keyword-selector :# [k]
  [walker
   [seqable? #(= (name k) (:id (second %)))]])

(defmethod keyword-selector :> [k]
  [identity
   (tag= (keyword (name k)))])

(defmethod keyword-selector :>. [k]
  [identity
   [seqable? #(if-let [class-str (:class (second %))]
                (some #{(name k)}
                      (string/split class-str #" ")))]])

(defmethod keyword-selector :># [k]
  [identity
   [seqable? #(= (name k) (:id (second %)))]])

(defmethod keyword-selector :key [k]
  [walker
   (specter/must (keyword (name k)))])