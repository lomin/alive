(ns me.lomin.alive.selectors
  (:require [com.rpl.specter :as specter]
            [clojure.string :as string]))

(def TAG 0)
(def ATTRS 1)
(def CONTENT 2)
(def SECOND (specter/nthpath 1))
(def MAYBE-ALL (specter/comp-paths seqable? specter/ALL))

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

(defmulti keyword-selector (fn [k] (keyword (namespace k))) :default ::tag)

;; tags
(defn tag= [tag]
  (specter/comp-paths indexed?
                      (specter/selected? [specter/FIRST
                                          (specter/pred= (keyword (name tag)))])))

(defmethod keyword-selector ::tag [k]
  [walker (tag= k)])

(defmethod keyword-selector :> [k]
  [(partial conj [MAYBE-ALL])
   (tag= k)])

; classes
(defn class= [class]
  (specter/comp-paths indexed?
                      (specter/selected? [1
                                          (specter/must :class)
                                          #(some #{(name class)}
                                                 (string/split % #" "))])))

(defmethod keyword-selector :. [k]
  [walker (class= k)])

(defmethod keyword-selector :>. [k]
  [(partial conj [MAYBE-ALL])
   (class= k)])

(defmethod keyword-selector :&. [k]
  [identity (class= k)])

; ids
(defn id= [id]
  (specter/comp-paths indexed?
                      (specter/selected? [1
                                          (specter/must :id)
                                          (specter/pred= (name id))])))

(defmethod keyword-selector :# [k]
  [walker (id= k)])

(defmethod keyword-selector :># [k]
  [(partial conj [MAYBE-ALL])
   (id= k)])

(defmethod keyword-selector :&# [k]
  [identity (class= k)])

; map-key
(defn must [k]
  (specter/must k))

(defmethod keyword-selector :must [k]
  [identity (must (keyword (name k)))])

(defn with-attr= [attr]
  (walker (must attr)))