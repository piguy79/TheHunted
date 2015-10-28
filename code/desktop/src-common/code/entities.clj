(ns code.entities
  (:require [play-clj.core :refer :all]
            [play-clj.g2d :refer :all]
            [play-clj.g2d-physics :as physics]))


(defn ^:private create
  ([start-layer img]
   (assoc img
          :x 0
          :y 0
          :height 2
          :width 2
          :max-velocity 0.5
          :start-layer start-layer))
  ([start-layer down up stand-right walk-right]
  (let [down-flip (texture down :flip true false)
        up-flip (texture up :flip true false)
        stand-flip (texture stand-right :flip true false)
        walk-flip (texture walk-right :flip true false)]
    (assoc (create start-layer down)
           :down (animation 0.2 [down down-flip])
           :up (animation 0.2 [up up-flip])
           :right (animation 0.2 [stand-right walk-right])
           :left (animation 0.2 [stand-flip walk-flip]))))) 

;; Need to load a texture and set its left and right
;; Give velocity property and set on the grass layer
;; Also give a width and height
(defn create-person
  []
  (let [sheet (texture "tiles.png")
        tiles (texture! sheet :split 16 16)
        player-images (for [col [0 1 2 3]]
                        (texture (aget tiles 6 col)))
        [down up stand-right walk-right] player-images]
    (assoc (create "grass" down up stand-right walk-right)
           :person? true)))

(defn create-player-body!
  [screen]
  (let [body (physics/add-body! screen (physics/body-def :static))]
    (->>
     [0 0
      0 1
      1 1
      1 0
      0 0]
     float-array
     (physics/chain-shape :create-chain)
     (physics/fixture-def :density 1 :shape)
     (physics/body! body :create-fixture))
    body))

(defn create-player
  [screen]
  (let [entity (assoc (create-person)
          :body (create-player-body! screen)
          :max-velocity 0.25
          :player? true
          :x 40
          :y 40)]
    entity))

(defn create-enemy
  [screen]
  (let [entity (assoc (create-person)
                       :body (create-player-body! screen)
                       :max-velocity 0.1
                       :enemy? true
                       :x 40
                       :y 45
                       :starting-x 40
                       :starting-y 45
                       :bounds {:width 3 :height 0}
                       :target-location {:x 48 :y 45})]
    entity))


(defn update-patrol-locations!
  [{:keys [starting-x bounds enemy?] :as entity}]
  (cond
    enemy? (assoc entity :target-location {:x (+ starting-x (:width bounds)) :y 45})
    :else entity))

(defn set-direction-based-on-key!
  [{:keys [direction] :as entity}]
  (cond
    (not (nil? direction)) entity
    (key-pressed? :dpad-left) (assoc entity :direction :left)
    (key-pressed? :dpad-right) (assoc entity :direction :right)
    (key-pressed? :dpad-up) (assoc entity :direction :up)
    (key-pressed? :dpad-down) (assoc entity :direction :down)
    :else entity))

(defn set-direction!
  [{:keys [player?] :as entity}]
  (cond
    player? (set-direction-based-on-key! entity)
    :else entity))


(defn remove-direction!
  [{:keys [player?] :as entity}]
  (cond
    player? (dissoc entity :direction)
    :else entity))


(defn get-player-velocity
  [{:keys [direction max-velocity]}]
  (cond
    (= direction :left) [(* -1 max-velocity) 0]
    (= direction :right) [max-velocity 0]
    (= direction :up) [0 max-velocity]
    (= direction :down) [0 (* -1 max-velocity)]
    :else [0 0]))

(defn target-location-reached?
  [{:keys [x y target-location]}]
  (let [x-diff (Math/abs (- (:x target-location) x))]
    (<= x-diff 0.1)))

(defn get-enemy-velocity
  [{:keys [max-velocity target-location x y] :as entity}]
  [(cond
     (target-location-reached? entity)
     0
     :else
     max-velocity)
   0])

(defn get-velocity
  [{:keys [x y max-velocity player? enemy?] :as entity}]
  (cond
    player? (get-player-velocity entity)
    enemy? (get-enemy-velocity entity)
    :else [0 0]))


(defn move
  [{:keys [delta-time] :as screen} 
   {:keys [x y width height max-velocity] :as entity}]
  (let [[x-velocity y-velocity] (get-velocity entity)
        new-x (+ x x-velocity)
        new-y (+ y y-velocity)
        bounds [[new-x new-y]
                [(+ width new-x) new-y]
                [(+ width new-x) (+ height new-y)]
                [new-x (+ height new-y)]]
        layers [(tiled-map-layer screen "water")
                (tiled-map-layer screen "trees")]
        cell (some
              #(not (nil? %))
              (for [[check-x check-y] bounds] 
                    (some #(tiled-map-cell % check-x check-y) layers)))]
    (when (nil? cell)
      (physics/body-x! entity new-x)
      (physics/body-y! entity new-y)))
  entity)


(defn ^:private animate-player
  [screen {:keys [direction] :as entity}]
  (if-let [anim (get entity direction)]
    (merge entity
           (animation->texture screen anim))
    entity))

(defn animate
  [screen {:keys [player?] :as entity}]
  (cond
    player? (animate-player screen entity)
    :else entity))
