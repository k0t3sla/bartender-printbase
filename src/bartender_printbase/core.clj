(ns bartender-printbase.core
  (:require
   [config.core :refer [env]]
   [org.httpkit.server :as http]
   [hiccup.page :as hiccup]
   [hiccup.core :as h]
   [reitit.ring :as ring]
   [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
   [ring.util.response :as response]
   [pg.core :as pg]
   
   [clojure.pprint :as pp])
  (:gen-class))

(defn text-input [label val id]
  [:label {:class "form-control w-full max-w-xs"}
   [:div {:class "label"}
    [:span {:class "label-text px-6"} label]
    [:input {:type "text" :value val :placeholder label :id id :name id :class "input input-bordered w-full max-w-xs"}]]])

(defn textaria-input [label val id]
  [:label {:class "form-control w-full max-w-xs"}
   [:div {:class "label"}
    [:span {:class "label-text px-6"} label]
    [:textarea {:placeholder label :value val :id id :name id :class "textarea textarea-bordered h-24"}]]])

(def demo-db #{{:id 1
                :name "Foo"
                :customer "bar"
                :requisites "inn 23424234"
                :address "Lenin 1"
                :phone "+792592342904"
                :sender "Ivan"}
               {:id 2
                :name "Example2"
                :customer "bar2"
                :requisites "inn 2323424"
                :address "Lenin 2"
                :phone "+7924232423423"
                :sender "pavel"}
               {:id 3
                :name "Example4"
                :customer "foo4"
                :requisites "inn 2080989"
                :address "tverskaya"
                :phone "+73434343"
                :sender "Alex"}})

(defn get-customer [id]
  (first (filter #(= (Integer/parseInt id) (:id %)) demo-db)))


(defn home-page [req]
  (let [id (:id (:params req))
        data (when id
               (get-customer id))]
    (hiccup/html5
     [:body
      [:head (hiccup/include-css "styles.css")]
      [:head (hiccup/include-js "htmx.js")]
      [:h1 (:id (:params req))]
      [:main {:class "container"}
       [:form
        [:input {:type "text" :placeholder "Поиск"}]]
       [:form
        (text-input "Имя записи" (:name data) "name")
        (text-input "Контрагент" (:customer data) "customer")
        (text-input "Реквизиты" (:requisites data) "requisites")
        (textaria-input "Адресс" (:address data) "address")
        (text-input "Телефон" (:phone data) "phone")
        (text-input "Отправитель" (:sender data) "sender")
        [:p (:params req)]
        [:input {:type "hidden" :name (:id data) :id (:id data)}]]]])))


(defn update-handler [req]
  (try (pp/pprint req)
       (h/html
        [:h2 "Успешно обновлен список"])
       (catch Exception e
         [:h2 "Ошибка"]
         [:h3 e])))

(defn add-handler [req]
  (try (pp/pprint req)
       (h/html
        [:h2 "Успешно добавлен в список"])
       (catch Exception e
         [:h2 "Ошибка"]
         [:h3 e])))

(defn delete-handler [req]
  (try (pp/pprint req)
       (h/html
        [:h2 "Удалена запись"])
       (catch Exception e
         [:h2 "Ошибка"]
         [:h3 e])))

(def handler
  (ring/ring-handler
   (ring/router
    [["/"
      {:get (fn [request]
              (-> (home-page request)
                  (response/response)
                  (response/header "content-type" "text/html")))}]
     ["/add"
      {:post (fn [request]
               (-> (add-handler request)
                   (response/response)
                   (response/header "content-type" "text/html")))}]
     ["/update"
      {:put (fn [request]
              (-> (update-handler request)
                  (response/response)
                  (response/header "content-type" "text/html")))}]
     ["/delete"
      {:post (fn [request]
               (-> (delete-handler request)
                   (response/response)
                   (response/header "content-type" "text/html")))}]])))


(defmethod response/resource-data :resource
  [^java.net.URL url]
  ;; GraalVM resource scheme
  (let [resource (.openConnection url)
        len (#'ring.util.response/connection-content-length resource)]
    (when (pos? len)
      {:content        (.getInputStream resource)
       :content-length len
       :last-modified  (#'ring.util.response/connection-last-modified resource)})))

(defonce server (atom nil))

(defn stop-server []
  (when-not (nil? @server)
    ;; graceful shutdown: wait 100ms for existing requests to be finished
    ;; :timeout is optional, when no timeout, stop immediately
    (@server :timeout 100)
    (reset! server nil)))

(defn start-server []
  (println "starting http.kit server http://localhost:8080/")
  (reset! server (http/run-server
                  (wrap-defaults
                   handler
                   (assoc api-defaults :static {:resources "public"}))
                  (if (nil? (:host-port env))
                    {:port (:host-port env)}
                    {:port 8080}))))


(defn -main
  []
  (start-server))

(comment
  (start-server)
  (stop-server) 
  )



(comment
  (def config
    {:host "127.0.0.1"
     :port 10140
     :user "test"
     :password "test"
     :database "test"})

  ;; connect to the database
  (def conn
    (pg/connect config))

  ;; a trivial query
  (pg/query conn "select 1 as one")
  ;; [{:one 1}]

  ;; let's create a table
  (pg/query conn "
  create table demo (
    id serial primary key,
    title text not null,
    created_at timestamp with time zone default now()
  )")
  ;; {:command "CREATE TABLE"}

  ;; Insert three rows returning all the columns.
  ;; Pay attention that PG2 uses not question marks (?)
  ;; but numered dollars for parameters:
  (pg/execute conn
              "insert into demo (title) values ($1), ($2), ($3)
               returning *"
              {:params ["test1" "test2" "test3"]}) 
)
