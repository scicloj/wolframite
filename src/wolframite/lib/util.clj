(ns wolframite.lib.util
  "This is a 'temporary' repository for useful functions that are still looking for their place in the world, i.e. they seem useful enough to be outside the local namespace but we're still trying to find the right level of abstraction.")

(defn leaf-keys-in
  "The vector of leaf key combinations that lead to leaves.
  "
  ([m] (leaf-keys-in [] m ()))
  ([prev m result]
   (reduce-kv (fn [res k v]
                (if (map? v)
                  (leaf-keys-in (conj prev k) v res)
                  (conj res (conj prev k))))
              result
              m)))

(defn leaf-values
  "The leaf values of a nested map."
  ([m]
   (leaf-values [] m ()))
  ([prev m result]
   (reduce-kv (fn [res k v]
                (if (map? v)
                  (leaf-values (conj prev k) v res)
                  (conj res v)))
              result
              m)))

(defn leave-values--filtered
  "The leaf values for a given final key."
  [m k--leaf]
  (map (fn [ks]
         (get-in m ks))
       (filter (fn [x] (some #(= % k--leaf) x)) (leaf-keys-in m))))
