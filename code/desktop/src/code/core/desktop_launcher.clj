(ns
    code.core.desktop-launcher
  (:require [code.core :refer :all])
  (:import [com.badlogic.gdx.backends.lwjgl LwjglApplication]
           [org.lwjgl.input Keyboard])
  (:gen-class))

(defn -main
  []
  (LwjglApplication. thehunted-game "TheHunted" 1000 700)
  (Keyboard/enableRepeatEvents true))
