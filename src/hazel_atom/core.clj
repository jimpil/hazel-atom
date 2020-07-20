(ns hazel-atom.core
  "Defines `HazelcastAtom` - a distributed construct implementing IAtom, IAtom2, IDeref, IReference & IMeta (i.e. atom-like)."
  (:import (clojure.lang IAtom IAtom2 IDeref IReference IMeta)
           (com.hazelcast.cp IAtomicReference)
           (com.hazelcast.core IFunction)
           (java.io Closeable)))

(defn- hz-fn
  "Turns a regular Clojure function into a hazelcast IFunction."
  ^IFunction [f & args]
  (if (fn? f)
    (reify IFunction
      (apply [_ x]
        (apply f x args)))
    f))

(deftype HazelcastAtom
  [^IAtomicReference aref meta-atom]

  IMeta
  (meta [_] @meta-atom)

  IReference
  (alterMeta [_ f args]
    (swap! meta-atom f args))

  (resetMeta [_ m]
    (reset! meta-atom m))

  IDeref
  (deref [_]
    (.get aref))

  IAtom
  (reset [_ nv]
    (.set aref nv)
    nv)

  (swap [_ f]
    (->> (hz-fn f)
         (.alterAndGet aref)))

  (swap [_ f arg1]
    (->> (hz-fn f arg1)
         (.alterAndGet aref)))

  (swap [_ f arg1 arg2]
    (->> (hz-fn f arg1 arg2)
         (.alterAndGet aref)))

  (swap [_ f arg1 arg2 arg-rest]
    (->> (apply hz-fn f arg1 arg2 arg-rest)
         (.alterAndGet aref)))

  (compareAndSet [_ ov nv]
    ;; avoid this method
    (.compareAndSet aref ov nv))

  IAtom2 ;; for completeness - do NOT prefer (unless interested in the 'before' value only)
  (resetVals [_ nv]
    [(.getAndSet aref nv) nv])

  (swapVals [_ f]
    [(.getAndAlter aref (hz-fn f))
     (.get aref)]) ;; not atomic

  (swapVals [_ f arg1]
    [(.getAndAlter aref (hz-fn f arg1))
     (.get aref)]) ;; not atomic

  (swapVals [_ f arg1 arg2]
    [(.getAndAlter aref (hz-fn f arg1 arg2))
     (.get aref)]) ;; not atomic

  (swapVals [_ f arg1 arg2 arg-rest]
    [(.getAndAlter aref (apply hz-fn f arg1 arg2 arg-rest))
     (.get aref)]) ;; not atomic

  Closeable
  (close [_] ;; completely destroys the reference!
    (.destroy aref))
  )

(defn ^HazelcastAtom hz-atom
  "Given a hazelcast `IAtomicReference`, an optional init-value
   and/or meta-map, returns a `HazelcastAtom`.

   Example:

     (-> (Hazelcast/newHazelcastInstance)
         .getCPSubsystem
         (.getAtomicReference \"app-data\")
         (hz-atom {:a 1 :b 2}))"
  ([hz-atomic-ref]
   (hz-atom hz-atomic-ref nil))
  ([hz-atomic-ref init-val]
   (hz-atom hz-atomic-ref init-val nil))
  ([^IAtomicReference hz-atomic-ref init-val meta-map]
   (cond-> (HazelcastAtom. hz-atomic-ref (atom meta-map))
           (some? init-val)
           (doto (reset! init-val)))))


(comment
  ;; example
  (-> (Hazelcast/newHazelcastInstance)
      .getCPSubsystem
      (.getAtomicReference "whatever")
      (hz-atom {:a 1 :b 2}))

  )
