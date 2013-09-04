(ns wlmmap.map
  (:require [mrhyde.extend-js]
            [blade :refer [L]]
            [cljs.core.async :refer [chan <! >! timeout]]
            [shoreleave.remotes.http-rpc :refer [remote-callback]])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [shoreleave.remotes.macros :as macros]))

(blade/bootstrap)

(def mymap (-> L .-mapbox (.map "map" "examples.map-uci7ul8p")
               (.setView [45 3.215] 5)))

(declare setdb addmarkers)

(def layers ())

(macros/rpc (get-lang-list (.-language js/navigator)) [p] (def db0 p))

(defn setmap []
  (do (setdb)
      ;; (addmarkers db0)
      ))

(defn setdb0 [ldb]
  (do (def db0 (list ldb))
      (addmarkers db0)))

(defn removelastlayer []
  (do (.removeLayer mymap (last layers))
      (set! layers (butlast layers))))

(defn setdb []
  (let [db (.getElementById js/document "db")
        yo (.getElementById js/document "go")]
    (set! (.-onclick yo)
          #(do (setdb0 (clojure.string/replace
                        (.-value db) #"/" ""))
               ; (removelastlayer)
               ))))

(defn addmarkers [dbb]
  (let [ch (chan)
        markers (L/MarkerClusterGroup.)]
    (set! layers (conj layers markers))
    (go (while true
          (let [a (<! ch)]
            (macros/rpc
             (get-marker a dbb) [p]
             (let [lat (first (first p))
                   lng (last (first p))
                   img (nth p 1)
                   title (last p)
                   icon ((get-in L [:mapbox :marker :icon])
                         {:marker-symbol ""
                          :marker-color (if img "FF0000" "0044FF")})
                   marker (-> L (.marker (L/LatLng. lat lng)
                                         {:icon icon}))]
               (.bindPopup marker title)
               (.addLayer markers marker))))))
    (remote-callback :get-markers [dbb]
                     #(go (doseq [a %]
                            (<! (timeout 1000))
                            (>! ch a))))
    (.addLayer mymap markers)
    (remote-callback
     :get-center [dbb]
     #(.setView mymap (vector (first %) (last %)) 5))))

;; initialize the HTML page in unobtrusive way
(set! (.-onload js/window) setmap)
