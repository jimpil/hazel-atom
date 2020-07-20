(ns hazel-atom.core-test
  (:require [clojure.test :refer :all]
            [hazel-atom.core :refer :all])
  (:import (com.hazelcast.core Hazelcast HazelcastInstance)))

(defn- cluster-of [n]
  (repeatedly n #(Hazelcast/newHazelcastInstance)))

(defn- get-atomic-ref
  [ref-name ^HazelcastInstance hz]
  (-> hz .getCPSubsystem (.getAtomicReference ref-name)))

(deftest hazel-atom-tests
  ;; it can be closed/destroyed
  (with-open [db (->> (Hazelcast/newHazelcastInstance)
                      (get-atomic-ref "unit-testing")
                      hz-atom)]

    (testing "resetting"
      (is (= 5 (reset! db 5))))


    (testing "swapping f"
      (testing "with 0-arg"
        (is (= 6 (swap! db inc))))

      (testing "with 1-arg"
        (is (= 8 (swap! db + 2))))

      (testing "with 2-arg"
        (is (= 10 (swap! db + 1 1))))

      (testing "with 3-arg"
        (is (= 13 (swap! db + 1 1 1))))

      (testing "with 4-arg"
        (is (= 17 (swap! db + 1 1 1 1))))

      (is (sequential? (swap! db range))))

    (testing "deref-ing"
      (is (= 17 (count @db))))
    ))

(deftest stress-test
  (testing "serial updates"
    (let [ref-name "serial-test"
          nodes    (cluster-of 3)
          [atom1 atom2 atom3 :as atoms]
          (->> nodes
               (map (partial get-atomic-ref ref-name))
               (map hz-atom))
          expected (range 100)]

      ;(println "Initialising with" (reset! atom2 []))

      (doseq [i expected] (swap! (rand-nth atoms) conj i))
      (is (= expected @atom1 @atom2 @atom3))
      (run! #(.shutdown ^HazelcastInstance %) nodes)))

  (Thread/sleep 200)

  (testing "parallel updates"
    (let [ref-name "parallel-test"
          nodes    (cluster-of 4)
          [atom1 atom2 atom3 atom4 :as atoms]
          (->> nodes
               (map (partial get-atomic-ref ref-name))
               (map hz-atom))
          ;_ (println "Initialising with" (reset! atom1 #{}))
          expected (range 200)
          ranges (partition 50 expected)
          loops (map
                  #(future
                     (doseq [i %2]
                       (Thread/sleep (rand-int 100))
                       (swap! %1 conj i))
                       true)
                  atoms
                  ranges)]
      ;(println "Waiting for nodes to complete their work...")
      (is (every? deref loops)) ;; block the thread until all done
      (is (= (set expected) @atom1 @atom2 @atom3 @atom4))
      (run! #(.shutdown ^HazelcastInstance %) nodes))

    )
  )


