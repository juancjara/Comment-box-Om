(ns ^:figwheel-always my-todo.core
    (:require-macros [cljs.core.async.macros :refer [go]])
    (:require [om.core :as om :include-macros true]
              [om.dom :as dom :include-macros true]
              [clojure.data :as data]
              [cljs.core.async :refer [put! chan <!]]))

(enable-console-print!)

(println "Edits to this text should show up in your developer console.")

;; define your app data so that it doesn't get over-written on reload

(defonce app-state
  (atom
    {:comments
     [{:author "aut1" :text "text1"}
      {:author "aut2" :text "text2"}
      {:author "aut3" :text "text3"}] }))

;;event handlers

(defn add-comment [evt owner {:keys [author text add] :as data}]
  (.preventDefault evt)
  (when-not (or (empty? author) (empty? text))
    (om/set-state! owner :author "")
    (om/set-state! owner :text "")
    (put! add {:author author :text text} )))

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
    (render-state [_ {:keys [add] :as state}]
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

(defn render-comment-list [{:keys [comments]}]
  (apply dom/ul #js {:className "commentList"}
    (om/build-all comment-view comments)))

(defn comment-box [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:add (chan)})

    om/IWillMount
    (will-mount [_]
      (let [add (om/get-state owner :add)]
        (go (loop []
          (let [new-comment (<! add)]
            (om/transact! data :comments #(conj % new-comment))
            (recur))))))

    om/IRenderState
    (render-state [_ {:keys [add]}]
      (dom/div #js {:className "commentBox"}
        (dom/h1 nil "Comments")
        (render-comment-list data)
        (om/build comment-form nil {:init-state {:add add}})))))

(om/root
  comment-box
  app-state
  {:target (. js/document (getElementById "app"))})


(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)

