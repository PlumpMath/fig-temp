(ns ^:figwheel-always fig-temp.core
  (:require-macros [reagent.ratom :refer [reaction]])
    (:require [clojure.browser.repl :as repl]
              [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [goog.events :as events]
              [goog.history.EventType :as EventType]
              [cljsjs.react :as react]
              [re-frame.core :refer [register-handler path register-sub dispatch subscribe]])
  (:import goog.History))

(defonce conn
  (repl/connect "http://localhost:9000/repl"))

;;; this is the initial state
(defonce initial-state
         {:timer (js/Date.)
          :time-color "#f34"})

;; define your app data so that it doesn't get over-written on reload

(defonce app-state (atom {:text "Hello world!"}))



(enable-console-print!)

;;(println "Should show up in your developer console.")
(println "Woot!")

(defn home-page []
  [:div [:h2 "Welcome to the rest of your life"]
   [:div [:a {:href "#/about"} "go to about page"]]])

(defn about-page []
  [:div [:h2 "About recur"]
   [:div [:a {:href "#/"} "go to the home page"]]])


(defonce time-updater (js/setInterval
                        #(dispatch [:timer (js/Date.)]) 1000))


;; Handlers
;-------------------------------------------------------------

;; This handler sets the initial state
(register-handler
  ;; the handler is passed a map (not an atom) and must return the new state
  ;; of the db
  :initialize
  (fn
    [db _]
    (merge db initial-state)))

;; This handler changes the color of the displayed time
(register-handler
  ;;; register-handler can take 3 arguments to allow you to insert middleware
  ;;; see https://github.com/Day8/re-frame/wiki/Handler-Middleware
  :time-color
  (path [:time-color])
  (fn
    ;; the path handler allows you to get directly to items in the database
    ;; return the value you want assoc'd in
    [time-color [_ value]]
    value))

;; This handler changes the value of the time
(register-handler
  :timer
  (fn
    ;; the first item in the second argument is :timer the second is the
    ;; new value
    [db [_ value]]
    (assoc db :timer value)))

;; add subscriptions to :timer and :time-color
(register-sub
  :timer
  (fn
    [db _]
    ;; you need to wrap your subscription code in a reaction
    (reaction (:timer @db))))

(register-sub
  :time-color
  (fn
    [db _]
    ;; you need to wrap your subscription code in a reaction
    (reaction (:time-color @db))))

(dispatch [:initialize])

(defn greeting [message]
  [:h1 message])

(defn clock []
  (let [time-color (subscribe [:time-color])
        timer (subscribe [:timer])]
    ;;; wrap your component in a function to use the suscription
    (fn []
      ;; note that the initialize call will not be dispatched immediately
      ;; as it is an async call
      (when @timer
        (let [time-str (-> @timer .toTimeString (clojure.string/split " ") first)]
          [:div.example-clock
          [:h2
           {:style {:color @time-color}}
           time-str]])))))

(defn color-input []
  (let [time-color (subscribe [:time-color])]
    ;;; wrap your component in a function to use the suscription
    (fn []
      [:div.color-input
       "Time color: "
       [:input {:type "text"
                :value @time-color
                :on-change #(dispatch
                             [:time-color (-> % .-target .-value)])}]])))

(defn simple-example []
  [:div#grid
   [:div#navbar "Navbar"]
   [:div#header "Header"]
   [:div#main "Main"
    [:div
     [greeting "Hello world, it is now"]
     [clock]
     [color-input]]]
   [:div#sidebar "This is the Sidebar!"]
   [:div#footer "Footer"]]
  )

(defn current-page []
   ;[(session/get :current-page)]
   [simple-example])


;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
                    (session/put! :current-page #'home-page))

(secretary/defroute "/about" []
                    (session/put! :current-page #'about-page))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
      EventType/NAVIGATE
      (fn [event]
        (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

;; -------------------------
;; Initialize app

(defn ^:export run []
  (reagent/render [simple-example]
                  (js/document.getElementById "app")))

;(defn mount-root []
;  (reagent/render [current-page] (.getElementById js/document "app")))
;
;(defn init! []
;  (hook-browser-navigation!)
;  (mount-root))

