(ns code.core.desktop-launcher
  (:require [code.core :refer :all])
  (:import [com.badlogic.gdx.backends.lwjgl LwjglApplication]
           [org.lwjgl.input Keyboard])
  (:gen-class))

(defn -main
  []
  (LwjglApplication. code-game "code" 800 600)
  (Keyboard/enableRepeatEvents true))
