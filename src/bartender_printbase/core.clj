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
   [pg.honey :as pgh]
   #_[clojure.pprint :as pp]
   [bartender-printbase.db :refer [conn
                                   select-first-ten
                                   get-customer
                                   insert-query
                                   delete-query
                                   update-query
                                   name-not-unique?
                                   requisites-not-unique?]]
   [bartender-printbase.components :refer [form]])
  (:gen-class))

(defn search-list [data]
  (if (zero? (count data))
    (h/html [:h1 "Ничего не найдено"])
    (map #(h/html
           [:li [:a {:href (str "?id=" (:id %))}
                 (str (:name %))]]) data)))

(defn search-customer [s]
  (search-list (pgh/execute conn {:select [:*]
                                  :from :customers
                                  :where [:like :name (str "%" s "%")]})))

(defn search-section []
  [:section
   {:class "min-h-96"}
   [:ul {:class "menu bg-base-200 rounded-box overflow-y-scroll" :id "search-results"}
    (search-list (select-first-ten))]])

(defn search-block []
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
     (search-section)]
    [:label {:class "modal-backdrop", :for "search_modal"} "Close"]]))


(defn home-page [req]
  (let [id (:id (:params req))
        form-data (when id
                    (get-customer id))]
    (hiccup/html5
     [:body
      [:head (hiccup/include-css "styles.css")]
      [:head (hiccup/include-js "htmx.js")]
      [:head (hiccup/include-js "inputmask.min.js")]
      [:head [:meta
              {:name "viewport", :content "width=device-width, initial-scale=1"}]]
      [:main {:class "container mx-auto sm:px-6 md:py-24"}
       [:div {:class "flex flex-col items-center justify-center"}
        [:div {:class "flex gap-x-12 py-6"}
         [:label {:for "search_modal" :class "btn btn-outline btn-wide btn-info"} "Поиск"]
         (when id [:a {:href "/" :class "btn btn-outline"} "Добавить новый элемент"])]]
       [:div {:class "flex flex-col items-center justify-center"}
        (if id
          (form {:data form-data :action :to-edit :disabled? true})
          (form {:data form-data :action :add :disabled? false}))]]
      (search-block)
      [:input {:type "checkbox", :id "del_modal", :class "modal-toggle"}]
      [:div
       {:class "modal", :role "dialog"}
       [:div
        {:class "modal-box"}
        [:div {:class "flex flex-col items-center justify-center"}
         [:form {:action "/delete", :method "POST"}
          [:input {:name "id", :type "hidden", :value id}]
          [:button {:class "btn btn-outline btn-wide btn-error"} "Удалить запись"]]]]
       [:label {:class "modal-backdrop", :for "del_modal"} "Close"]]
      (hiccup/include-js "scripts.js")])))

(defn edit-handler [req]
  (try (when req
         (h/html (form {:data (get-customer (:id (:params req))) :action :edit :disabled? false})))
       (catch Exception e
         (h/html [:h2 "Ошибка"]
                 [:h3 e]))))

(defn update-handler [req]
  (try (update-query (:params req))
       (h/html (form {:data (get-customer (:id (:params req))) :action :to-edit :disabled? true}))
       (catch Exception e
         (h/html [:h2 "Ошибка"]
                 [:h3 e]))))

(defn add-handler [req]
  (try (let [params (:params req)
             uniq-name? (name-not-unique? params)
             uniq-requisites? (requisites-not-unique? params)]
         (if (some true? [uniq-name?
                          uniq-requisites?])
           (h/html (form {:data params :action :to-edit
                          :disabled? true
                          :name-not-unuque uniq-name? :requisites-not-unique uniq-requisites?}))
           (do (insert-query (:params req))
               (response/redirect "/"))))
       (catch Exception e
         (h/html [:h2 "Ошибка"]
                 [:h3 e]))))

(defn copy-handler [req]
  (try (when req
         (h/html (form {:data (dissoc (get-customer (:id (:params req))) :id) :action :copy :disabled? false})))
       (catch Exception e
         (h/html [:h2 "Ошибка"]
                 [:h3 e]))))

(defn delete-handler [req]
  (-> req
      :params
      :id
      Integer/parseInt
      delete-query)
  (response/redirect "/"))

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
               (-> (add-handler request)))}]
     ["/update"
      {:put (fn [request]
              (-> (update-handler request)))}]
     ["/copy"
      {:post (fn [request]
               (-> (copy-handler request)
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

  ;; let's create a table
  (pg/query conn "
  CREATE TABLE customers (
    id serial PRIMARY KEY,
    name text NOT NULL,
    customer text NOT NULL,
    requisites text NOT NULL,
    address text NOT NULL,
    phone text NOT NULL,
    sender text NOT NULL
);")

  (pgh/execute conn {:select [:*]
                     :from :customers})
  )
