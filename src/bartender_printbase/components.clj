(ns bartender-printbase.components)

(defn text-input [label val disabled? id]
  [:label {:class "form-control w-full max-w-xs"}
   [:div {:class "label"}
    [:span {:class "label-text px-6"} label]
    [:input {:type "text" :disabled disabled? :value val :placeholder label :id id :name id :class "input input-bordered w-full max-w-xs"}]]])

(defn textaria-input [label val disabled? id]
  [:label {:class "form-control w-full max-w-xs"}
   [:div {:class "label"}
    [:span {:class "label-text px-6"} label]
    [:textarea {:placeholder label :disabled disabled? :id id :name id :class "textarea textarea-bordered h-24"} val]]])

(defn button-block [id]
  (if id
    [:div {:class "flex gap-x-12 justify-center max-w-[750px] mx-auto"}
     [:button {:class "btn btn-outline btn-primary" :hx-get (str "/edit?id=" id)} "Изменить"]
     [:button {:class "btn btn-outline btn-warning"} "Удалить"]]
    [:div {:class "flex gap-x-12 justify-center max-w-[750px] mx-auto"}
     [:button {:class "btn btn-outline btn-success"} "Добавить"]]))

(defn form [data disabled?]
  [:form
   (text-input "Имя записи" (:name data) disabled? "name")
   (text-input "Контрагент" (:customer data) disabled? "customer")
   (text-input "Реквизиты" (:requisites data) disabled? "requisites")
   (textaria-input "Адресс" (:address data) disabled? "address")
   (text-input "Телефон" (:phone data) disabled? "phone")
   (text-input "Отправитель" (:sender data) disabled? "sender")
   (when-not disabled? [:input {:type "hidden" :name (:id data) :id (:id data)}])
   (button-block (:id data))])

