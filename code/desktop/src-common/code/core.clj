(ns code.core
  (:require [code.entities :as e]
            [play-clj.core :refer :all]
            [play-clj.ui :refer :all]
            [play-clj.g2d :refer :all]
            [play-clj.g2d-physics :as physics]))


(declare thehunted-game main-screen)

(def movement-keys [(key-code :dpad-left)
                    (key-code :dpad-right)
                    (key-code :dpad-up)
                    (key-code :dpad-down)])

(defn movement-key?
  [key]
  (pos? (count (filter #(= % key) movement-keys))))


(defn update-camera!
  [screen entities]
  (doseq [{:keys [x y player?]} entities]
    (when player?
      (position! screen x y)))
  entities)

(defscreen main-screen
  :on-show
  (fn [screen entities]
    (let [renderer (orthogonal-tiled-map "level1.tmx" (/ 1 8))
          screen (update! screen
                          :camera (orthographic)
                          :renderer renderer
                          :world (physics/box-2d 0 0))]
      [(e/create-player screen)
       (e/create-enemy screen)]))
 
  :on-render
  (fn [screen entities]
    (clear!)
    (->> entities
         (map (fn [entity]
                (->> entity
                     (e/update-patrol-locations!)
                     (e/move screen)
                     (e/animate screen))))
         (render! screen)
         (physics/step! screen)
         (update-camera! screen)))

  :on-resize
  (fn [screen entities]
    (height! screen 30))


  :on-key-up
  (fn [screen entitites]
    (cond
      (movement-key? (:key screen))
      (->> entitites
           (map e/remove-direction!)
           (map e/set-direction!))
      :else entitites))
  
  :on-key-down
  (fn [screen entities]
    (cond
      (= (:key screen) (key-code :r))
         ((on-gl (set-screen! thehunted-game main-screen))
          entities)
      (movement-key? (:key screen))
         (map e/set-direction! entities)
      :else entities)))


(defgame thehunted-game
  :on-create
  (fn [this]
    (set-screen! this main-screen)))

(defscreen blank-screen
  :on-render
  (fn [screen entities]
    (clear!)))

(set-screen-wrapper! (fn [screen screen-fn]
                       (try (screen-fn)
                         (catch Exception e
                           (.printStackTrace e)              
                           (set-screen! thehunted-game blank-screen)))))
