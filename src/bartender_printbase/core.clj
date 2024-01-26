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


(defn home-page [req]
  (let [id (:params req)]
    (hiccup/html5
     [:body
      [:head (hiccup/include-css "styles.css")]
      [:head (hiccup/include-js "htmx.js")]
      [:main {:class "container mx-auto"}
       [:form
        [:input {:type "text" :placeholder "Поиск"}]]
       [:form
        [:input {:type "text" :placeholder "Имя записи"}]
        [:input {:type "text" :placeholder "Контрагент"}]
        [:input {:type "text" :placeholder "Реквизиты"}]
        [:textaria {:type "Адресс"}]
        [:input {:type "Телефон"}]
        [:input {:type "Отправитель"}]
        [:input {:type "hidden"}]]]])))

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
        [:h2 "Успешно добавлен товар"])
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
                  (if (:host-port env)
                    {:port (:host-port 4000)}
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
              {:params ["test1" "test2" "test3"]}))
