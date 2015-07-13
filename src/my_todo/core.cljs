(ns ^:figwheel-always my-todo.core
    (:require-macros [cljs.core.async.macros :refer [go]])
    (:require [om.core :as om :include-macros true]
              [om.dom :as dom :include-macros true]
              [cljs.core.async :refer [put! chan <!]]
              [ajax.core :refer [GET POST]]))

(enable-console-print!)

(println "Edits to this text should show up in your developer console.")

;; define your app data so that it doesn't get over-written on reload

(defonce app-state
  (atom
    {:comments [] }))

;;ajax

(defn simple-handler [response]
  (.log js/console (str response)))

(defn get-comments [f res]
  (f res))

(defn error-handler [{:keys [status status-text]}]
  (.log js/console (str "something bad happened: " status " " status-text)))

(defn load-data [comm]
  (GET "http://localhost:3000/comments"
    {:response-format :json
     :keywords? true
     :handler (partial get-comments #(put! comm [:init %] ))
     :error-handler error-handler}))

(defn save-commemnt [new-comment]
  (POST "http://localhost:3000/comments"
    {:params new-comment
     :format :json
     :handler simple-handler
     :error-handler error-handler}))

;;event handlers

(defn add-comment [evt owner {:keys [author text comm] :as data}]
  (.preventDefault evt)
  (when-not (or (empty? author) (empty? text))
    (let [new-comment {:author author :text text}]
    (om/set-state! owner :author "")
    (om/set-state! owner :text "")
    (put! comm [:add new-comment])
    (save-commemnt new-comment))))

;;(defn handle-change [e owner {:keys [text]}]
(defn handle-change [e owner key-name]
  (let [value (-> e .-target .-value)] ;; === (.. e .-target .-value)
    (.preventDefault e)
    (om/set-state! owner (keyword key-name) value)))

;; Other components

(defn comment-view [comment owner]
  (reify
    om/IRender
      (render [_]
        (dom/li nil
          (dom/div #js {:className "comment"}
            (dom/h2 #js {:className "commentAuthor"} (:author comment))
            (dom/div nil (:text comment)))))))

(defn comment-form [_ owner]
  (reify
    om/IInitState
    (init-state [_]
      {:author "",
       :text ""})

    om/IRenderState
    (render-state [_ {:keys [comm] :as state}]
      (dom/form #js {:className "commentForm"
                     :onSubmit #(add-comment % owner state)}
        (dom/input #js {:type "text"
                        :placeholder "Your name"
                        :value (:author state)
                        :onChange #(handle-change % owner "author")})
        (dom/input #js {:type "text"
                        :placeholder "Say something"
                        :value (:text state)
                        :onChange #(handle-change % owner "text")})
        (dom/input #js {:type "submit" :value "Post"})))))

;;main component

(defn handle-comm-event [t app val]
  (case t
    :add (om/transact! app :comments #(conj % val))
    :init (om/update! app {:comments val})
    nil))

(defn render-comment-list [{:keys [comments]}]
  (apply dom/ul #js {:className "commentList"}
    (om/build-all comment-view comments)))


(defn comment-box [app owner]
    (reify
    om/IWillMount
    (will-mount [_]
      (let [comm (chan)]
        (om/set-state! owner :comm comm)
          (go
           (load-data comm)
           (while true
            (let [ [t value] (<! comm)]
              (handle-comm-event t app value))))))

    om/IRenderState
    (render-state [_ {:keys [comm]}]
      (dom/div #js {:className "commentBox"}
        (dom/h1 nil "Comments")
        (render-comment-list app)
        (om/build comment-form nil {:init-state {:comm comm}})))))

(om/root
  comment-box
  app-state
  {:target (. js/document (getElementById "app"))})


(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)

