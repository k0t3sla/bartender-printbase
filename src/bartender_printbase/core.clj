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
   [bartender-printbase.components :refer [form]]
   [clojure.edn :as edn]
   [clojure.java.io :as io]

   [clojure.pprint :as pp])
  (:gen-class))

(defn load-edn
  "Load edn from an io/reader source (filename or io/resource)."
  [source]
  (try
    (with-open [r (io/reader source)]
      (edn/read (java.io.PushbackReader. r)))

    (catch java.io.IOException e
      (printf "Couldn't open '%s': %s\n" source (.getMessage e)))
    (catch RuntimeException e
      (printf "Error parsing edn file '%s': %s\n" source (.getMessage e)))))

(def demo-db (atom (load-edn "resources/fake-db.edn")))

(defn search-list [data]
  (map #(h/html
         [:li [:a {:href (str "?id=" (:id %))}
               (str (:name %))]]) data))

(defn get-customer [id]
  (first (filter #(= (Integer/parseInt id) (:id %)) @demo-db)))

(defn search-customer [s]
  (search-list (filter #(str/includes? (:name %) s) @demo-db)))

(def search-section
  [:section
   {:class "min-h-96"}
   [:ul {:class "menu bg-base-200 rounded-box overflow-y-scroll" :id "search-results"}
    (search-list (take 10 @demo-db))]])

(def search-block
  (list
   [:input {:type "checkbox", :id "search_modal", :class "modal-toggle"}]
   [:div
    {:class "modal", :role "dialog"}
    [:div
     {:class "modal-box md:min-w-[750px] max-h-[490px]"}
     [:input
      {:class "input input-bordered w-full mb-2",
       :type "search",
       :name "search",
       :placeholder "Печатать чтобы искать по имени",
       :hx-post "/search",
       :hx-trigger "input changed delay:200ms, search",
       :hx-target "#search-results",
       :hx-indicator ".htmx-indicator"}]
     search-section]
    [:label {:class "modal-backdrop", :for "search_modal"} "Close"]]))


(defn home-page [req]
  (let [id (:id (:params req))
        form-data (when id
                    (get-customer id))]
    (hiccup/html5
     [:body
      [:head (hiccup/include-css "styles.css")]
      [:head (hiccup/include-js "htmx.js")]
      [:head [:meta
              {:name "viewport", :content "width=device-width, initial-scale=1"}]]
      [:main {:class "container mx-auto sm:px-6 md:py-24"}
       [:div {:class "flex flex-col items-center justify-center"}
        [:div {:class "py-6"} [:label {:for "search_modal" :class "btn btn-outline btn-wide btn-info"} "Поиск"]]]
       [:div {:class "flex flex-col items-center justify-center"}
        (if id 
          (form {:data form-data :action :to-edit :disabled? true})
          (form {:data form-data :action :add :disabled? false}))]]
      search-block
      [:input {:type "checkbox", :id "del_modal", :class "modal-toggle"}]
      [:div
       {:class "modal", :role "dialog"}
       [:div
        {:class "modal-box"}
        [:div {:class "flex flex-col items-center justify-center"}
         [:form {:action "/delete", :method "POST"}
          [:input {:name "id", :type "hidden", :value id}]
          [:button {:class "btn btn-outline btn-wide btn-error"} "Удалить запись"]]]]
       [:label {:class "modal-backdrop", :for "del_modal"} "Close"]]])))

(defn edit-handler [req]
  (try (when req
         (h/html (form {:data (get-customer (:id (:params req))) :action :edit :disabled? false})))
       (catch Exception e
         (h/html [:h2 "Ошибка"]
                 [:h3 e]))))

(defn update-handler [req]
  (try (h/html
        (form {:data (get-customer (:id (:params req))) :action :upd :disabled? false}))
       (catch Exception e
         (h/html [:h2 "Ошибка"]
                 [:h3 e]))))

(defn add-handler [req]
  (try (pp/pprint req)
       (h/html
        (form {:data (get-customer (:id (:params req))) :action :add :disabled? false}))
       (catch Exception e
         (h/html [:h2 "Ошибка"]
                 [:h3 e]))))

(defn delete-handler [req]
  (-> req
      :params
      :id
      println)
  (response/redirect "/"))

(defn copy-handler [req]
  (try (h/html
        (form {:data (get-customer (:id (:params req))) :action :copy :disabled? false}))
       (catch Exception e
         (h/html [:h2 "Ошибка"]
                 [:h3 e]))))

(defn search-handler [req]
  (try (when req
         (search-customer (:search (:params req))))
       (catch Exception e
         (h/html [:h2 "Ошибка"]
                 [:h3 e]))))

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
     ["/edit"
      {:get (fn [request]
              (-> (edit-handler request)
                  (response/response)
                  (response/header "content-type" "text/html")))}]
     ["/add"
      {:post (fn [request]
               (-> (add-handler request)
                   (response/response)
                   (response/header "content-type" "text/html")))}]
     ["/copy"
      {:post (fn [request]
               (-> (copy-handler request)
                   (response/response)
                   (response/header "content-type" "text/html")))}]
     ["/update"
      {:put (fn [request]
              (-> (update-handler request)
                  (response/response)
                  (response/header "content-type" "text/html")))}]
     ["/delete"
      {:post (fn [request]
               (delete-handler request))}]])))


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
                  {:port 8080})))

(defn -main
  []
  (start-server))

(comment
  (start-server)
  (stop-server))



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
    name text not null,
    customer text not null,
    requisites text not null,
    email text not null,
    phone text not null,
    address text not null,
    sender text not null,
  )")
  ;; {:command "CREATE TABLE"}

  ;; Insert three rows returning all the columns.
  ;; Pay attention that PG2 uses not question marks (?)
  ;; but numered dollars for parameters:
  (pg/execute conn
              "insert into demo (title) values ($1), ($2), ($3)
               returning *"
              {:params ["test1" "test2" "test3"]}))
