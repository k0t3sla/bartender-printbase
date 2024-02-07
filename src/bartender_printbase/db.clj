(ns bartender-printbase.db
  (:require [config.core :refer [env]]
            [pg.core :as pg]
            [clojure.string :as str]
            [pg.honey :as pgh]))


(def config
  {:host (:pg-host env)
   :port (:pg-port env)
   :user (:pg-user env)
   :password (:pg-password env)
   :database (:pg-database env)})

(def conn
  (pg/connect config))

(defn select-first-ten []
  (pgh/execute conn
               {:select [:*]
                :from :customers
                :limit 10}))

(defn get-customer [id]
  (first (pgh/execute conn {:select [:*]
                            :from :customers
                            :where [:= :id (Integer/parseInt id)]})))

(defn insert-query [{:keys [name customer requisites address phone sender]}]
  (pgh/execute conn {:insert-into :customers
                     :values [{:name (str/trim name) :customer (str/trim customer) :requisites (str/trim requisites)
                               :address (str/trim address) :phone phone :sender sender}]}))

(defn delete-query [id]
  (pgh/execute conn {:delete-from :customers
                     :where [:= :id id]}))

(defn update-query [{:keys [name customer requisites address phone sender id]}]
  (pgh/execute conn {:update :customers
                     :set {:name (str/trim name) :customer (str/trim customer) :requisites (str/trim requisites)
                           :address (str/trim address) :phone phone :sender sender}
                     :where [:= :id (Integer/parseInt id)]}))

(defn name-not-unique? [{:keys [name]}]
  (let [name-exists-query {:select [:*]
                           :from :customers
                           :where [:= :name name]}]
    (-> (pgh/execute conn name-exists-query)
        first
        boolean)))

(defn requisites-not-unique? [{:keys [requisites]}]
  (let [requisites-exists-query {:select [:*]
                           :from :customers
                           :where [:= :requisites requisites]}]
    (-> (pgh/execute conn requisites-exists-query)
        first
        boolean)))