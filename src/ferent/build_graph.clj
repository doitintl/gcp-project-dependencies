(ns ferent.build-graph
  (:require [ferent.utils :refer [invert-invertible-map
                                  invert-multimap
                                  remove-keys-with-empty-val
                                  ]]
            [sc.api :refer :all]))


(defn proj-for-service-accounts [service-accounts proj-to-serviceaccounts]
  "Get the projects associated with theseservice-accounts, given a mapping from proj-to-serviceaccounts."
  (let [sa-to-proj (invert-invertible-map proj-to-serviceaccounts)]
    (map #(sa-to-proj %) service-accounts))
  )



(defn build-graph [project-to-sas-granted-role proj-to-its-sas]
  (let [

        x (-> project-to-sas-granted-role
              (update-vals (fn [service-accounts]           ;get proj of the SA that was granted a role
                             (proj-for-service-accounts service-accounts proj-to-its-sas)))
              (update-vals (fn [deps] (remove nil? deps)))  ;remove nil: Those where the project of the SA was not found

              )
        y (map (fn [[proj deps]] [proj (set (remove #(= proj %) deps))])) ;remove self-dependency
        ;(comment (map (comp
        ;        (fn [[proj deps]] [proj (set (remove #(= proj %) deps))]) ;remove self-dependency
        ;        (fn [[proj deps]] [proj (remove nil? deps)]) ;remove nil: Those where the project of the SA was not found
        ;        (fn [[proj service-accounts]] ;get proj of the SA that was granted a role
        ;          [proj (proj-for-service-accounts service-accounts proj-to-its-sas)]))
        ;      ))
        arrow-in (remove-keys-with-empty-val y)
        ]
    {:arrow-in  arrow-in
     :arrow-out (invert-multimap arrow-in)})
  )




