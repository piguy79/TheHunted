(ns code.core
  (:require [code.entities :as e]
            [play-clj.core :refer :all]
            [play-clj.ui :refer :all]
            [play-clj.g2d :refer :all]
            [play-clj.g2d-physics :as physics]))


(declare code-game main-screen)


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
                     (e/move screen entities))))
         (render! screen)
         (physics/step! screen)
         (update-camera! screen)))

  :on-resize
  (fn [screen entities]
    (height! screen 30))

  :on-key-down
  (fn [screen entities]
    (cond
      (= (:key screen) (key-code :r))
      (on-gl (set-screen! code-game main-screen)))
    entities))


(defgame code-game
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
                           (set-screen! code-game blank-screen)))))
