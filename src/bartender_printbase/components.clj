(ns bartender-printbase.components)

(defn text-input [label val disabled? id]
  [:label {:class "form-control w-full"}
   [:div {:class "label"}
    [:span {:class "label-text md:px-6"} label]
    [:input {:type "text" :disabled disabled? :value val :placeholder label :id id :name id :class "input input-bordered md:w-full md:max-w-xs"}]]])

(defn textaria-input [label val disabled? id]
  [:label {:class "form-control w-full"}
   [:div {:class "label"}
    [:span {:class "label-text px-6"} label]
    [:textarea {:placeholder label :disabled disabled? :id id :name id :class "textarea textarea-bordered h-24 md:w-[324px]"} val]]])

(defn input-button-block [data action disabled?]
  (list
   [:div {:class "pb-8"}
    (text-input "Имя записи" (:name data) disabled? "name")
    (text-input "Контрагент" (:customer data) disabled? "customer")
    (text-input "Реквизиты" (:requisites data) disabled? "requisites")
    (textaria-input "Адресс" (:address data) disabled? "address")
    (text-input "Телефон" (:phone data) disabled? "phone")
    (text-input "Отправитель" (:sender data) disabled? "sender")]
   [:input {:type "hidden" :name (:id data) :id (:id data)}]
   (cond
     (= action :copy) [:div {:class "flex gap-x-12 justify-center max-w-[750px] mx-auto"}
                      [:button {:class "btn btn-outline btn-success"} "Добавить"]]
     (= action :add) [:div {:class "flex gap-x-12 justify-center max-w-[750px] mx-auto"}
                      [:button {:class "btn btn-outline btn-success"} "Добавить"]]
     (= action :edit) [:div {:class "flex gap-x-12 justify-center max-w-[750px] mx-auto"}
                       [:button {:class "btn btn-outline btn-accent"} "Обновить"]
                       [:a {:href (str "/?id=" (:id data)) :class "btn btn-outline btn-secondary"} "Отмена"]]
     (= action :to-edit) [:div {:class "flex gap-x-12 justify-center max-w-[750px] mx-auto"}
                          [:button {:class "btn btn-outline btn-primary" :hx-get (str "/edit?id=" (:id data))} "Изменить"]
                          [:button {:class "btn btn-outline btn-warning" :hx-post (str "/copy?id=" (:id data))} "Добавить копированием"]
                          [:label {:for "del_modal" :class "btn btn-outline btn-error"} "Удалить"]])))


(defn form [{:keys [data action disabled?]}]
  (cond
    (= action :copy) [:form {:class "md:w-[32rem]" :action "/add" :method "POST" :hx-target "this" :hx-swap "outerHTML"}
                      (input-button-block data action disabled?)]
    (= action :to-edit) [:form {:class "md:w-[32rem]" :hx-target "this" :hx-swap "outerHTML"}
                         (input-button-block data action disabled?)]
    (= action :add) [:form {:class "md:w-[32rem]" :action "/add", :method "POST" :hx-target "this" :hx-swap "outerHTML"}
                     (input-button-block data action disabled?)]
    (= action :edit) [:form {:class "md:w-[32rem]" :hx-put "/update" :hx-target "this" :hx-swap "outerHTML"}
                      (input-button-block data action disabled?)]))