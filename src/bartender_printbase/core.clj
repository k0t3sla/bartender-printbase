(ns bartender-printbase.core
  (:require
   [config.core :refer [env]]
   [clojure.string :as str]
   [org.httpkit.server :as http]
   [hiccup.page :as hiccup]
   [hiccup.core :as h]
   [reitit.ring :as ring]
   [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
   [ring.util.response :as response]
   [pg.core :as pg]
   
   [clojure.pprint :as pp])
  (:gen-class))

(defn text-input [label val disabled? id]
  [:label {:class "form-control w-full max-w-xs"}
   [:div {:class "label"}
    [:span {:class "label-text px-6"} label]
    (if disabled?
      [:input {:type "text" :disabled true :value val :placeholder label :id id :name id :class "input input-bordered w-full max-w-xs"}]
      [:input {:type "text" :value val :placeholder label :id id :name id :class "input input-bordered w-full max-w-xs"}])]])

(defn textaria-input [label val disabled? id]
  [:label {:class "form-control w-full max-w-xs"}
   [:div {:class "label"}
    [:span {:class "label-text px-6"} label]
    (if disabled?
      [:textarea {:placeholder label :disabled true :id id :name id :class "textarea textarea-bordered h-24"} val]
      [:textarea {:placeholder label :id id :name id :class "textarea textarea-bordered h-24"} val])]])

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

(defn search-customer [s]
  (filter #(str/includes? (:name %) s) demo-db))

(defn form [data disabled?]
  [:form
   (text-input "Имя записи" (:name data) disabled? "name")
   (text-input "Контрагент" (:customer data) disabled? "customer")
   (text-input "Реквизиты" (:requisites data) disabled? "requisites")
   (textaria-input "Адресс" (:address data) disabled? "address")
   (text-input "Телефон" (:phone data) disabled? "phone")
   (text-input "Отправитель" (:sender data) disabled? "sender")
   (when-not disabled? [:input {:type "hidden" :name (:id data) :id (:id data)}])])


(defn home-page [req]
  (let [id (:id (:params req))
        data (when id
               (get-customer id))]
    (hiccup/html5
     [:body
      [:head (hiccup/include-css "styles.css")]
      [:head (hiccup/include-js "htmx.js")]
      [:main {:class "container mx-auto py-24"}
       [:div {:class "flex flex-col items-center justify-center"}
        [:label {:for "search_modal", :class "btn"} "Поиск"]
        (form data false)]
       [:button {}]]
      [:input {:type "checkbox", :id "search_modal", :class "modal-toggle"}]
      [:div
       {:class "modal", :role "dialog"}
       [:div
        {:class "modal-box"}
        [:h3 {:class "text-lg font-bold"} "Поиск по пользователям"]
        [:p {:class "py-4"} "This modal works with a hidden checkbox!"]]
       [:label {:class "modal-backdrop", :for "search_modal"} "Close"]]])))


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

(defn search-handler [req]
  (try (when req
         (search-customer (:search (:params req))))
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
     ["/search"
      {:post (fn [request]
              (-> (search-handler request)
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
