;!zprint {:style :require-justify :style-map {:rj-var {:pair {:justify {:max-variance 15}}}}}
(ns zprint.config-test
  (:require
    [expectations.clojure.test #?(:clj :refer
                                  :cljs :refer-macros)
                                 [defexpect expect]]
    [zprint.zutil]
    #?(:clj [clojure.edn :as edn])
    #?(:clj [clojure.java.io :as io])
    #?(:clj [clojure.string :as str])
    [zprint.core        :refer [set-options! zprint-str load-options!]]
    [zprint.config      :refer [get-options get-explained-all-options only-set]]
    [rewrite-clj.parser :as    p
                        :refer [parse-string parse-string-all]]
    [rewrite-clj.node   :as n]
    [rewrite-clj.zip    :as    z
                        :refer [edn*]])
  #?(:clj (:import (com.sun.net.httpserver HttpHandler HttpServer)
                   (java.net InetSocketAddress)
                   (java.io File ByteArrayOutputStream PrintStream)
                   (java.util Date))))

;; Keep some of the tests from wrapping so they still work
;; and format more-of more readably.

;!zprint {:comment {:wrap? false} :fn-map {"more-of" [:arg1-pair {:fn-force-nl #{:arg1-pair} :list {:constant-pair-min 2}}]}}         


;
; Keep tests from configuring from any $HOME/.zprintrc or local .zprintrc
;

(set-options! {:configured? true})

(defexpect config-tests

  ;;
  ;; # :fn-*-force-nl tests
  ;;

  (expect "(if :a\n  :b\n  :c)"
          (zprint-str "(if :a :b :c)"
                      {:parse-string? true, :fn-force-nl #{:arg1-body}}))

  (expect "(if :a\n  :b\n  :c)"
          (zprint-str "(if :a :b :c)"
                      {:parse-string? true, :fn-gt2-force-nl #{:arg1-body}}))

  (expect "(if :a :b)"
          (zprint-str "(if :a :b)"
                      {:parse-string? true, :fn-gt2-force-nl #{:arg1-body}}))

  (expect "(assoc {} :a :b)"
          (zprint-str "(assoc {} :a :b)"
                      {:parse-string? true, :fn-gt3-force-nl #{:arg1-pair}}))

  (expect "(assoc {}\n  :a :b\n  :c :d)"
          (zprint-str "(assoc {} :a :b :c :d)"
                      {:parse-string? true, :fn-gt3-force-nl #{:arg1-pair}}))

  (expect
    "(:require [boot-fmt.impl :as impl]\n          [boot.core :as bc]\n          [boot.util :as bu])"
    (zprint-str
      "(:require [boot-fmt.impl :as impl] [boot.core :as bc] [boot.util :as bu])"
      {:parse-string? true, :fn-map {":require" :force-nl-body}}))

  (expect
    "(:require\n  [boot-fmt.impl :as impl]\n  [boot.core :as bc]\n  [boot.util :as bu])"
    (zprint-str
      "(:require [boot-fmt.impl :as impl] [boot.core :as bc] [boot.util :as bu])"
      {:parse-string? true, :fn-map {":require" :flow}}))

  (expect
    "(:require\n  [boot-fmt.impl :as impl]\n  [boot.core :as bc]\n  [boot.util :as bu])"
    (zprint-str
      "(:require [boot-fmt.impl :as impl] [boot.core :as bc] [boot.util :as bu])"
      {:parse-string? true, :fn-map {":require" :flow-body}}))

  ;;
  ;; # Style tests
  ;;

  ;;
  ;; First, let's ensure that we know how to do these tests!
  ;;

  (expect false (:justify? (:binding (get-options))))

  ;;
  ;; This is how you do config tests, without altering the configuration
  ;; for all of the rest of the tests.
  ;;


  (expect true
          (with-redefs [zprint.config/configured-options
                          (atom zprint.config/default-zprint-options)
                        zprint.config/explained-options
                          (atom zprint.config/default-zprint-options)
                        zprint.config/explained-sequence (atom 1)]
            (set-options! {:style :justified})
            (:justify? (:binding (get-options)))))


  ;;
  ;; And this shows that it leaves things alone!
  ;;

  (expect false (:justify? (:binding (get-options))))


  ;;
  ;; Now, to the actual tests
  ;;

  (expect (more-of options
            true (:justify? (:binding options))
            true (:justify? (:map options))
            true (:justify? (:pair options)))
          (with-redefs [zprint.config/configured-options
                          (atom zprint.config/default-zprint-options)
                        zprint.config/explained-options
                          (atom zprint.config/default-zprint-options)
                        zprint.config/explained-sequence (atom 1)]
            (set-options! {:style :justified})
            (get-options)))



  (expect (more-of options
            0 (:indent (:binding options))
            1 (:indent-arg (:list options))
            0 (:indent (:map options))
            0 (:indent (:pair options))
            :none ((:fn-map options) "apply")
            :none ((:fn-map options) "assoc")
            :none ((:fn-map options) "filter")
            :none ((:fn-map options) "filterv")
            :none ((:fn-map options) "map")
            :none ((:fn-map options) "mapv")
            :none ((:fn-map options) "reduce")
            :none ((:fn-map options) "remove")
            :none-body ((:fn-map options) "with-meta"))
          (with-redefs [zprint.config/configured-options
                          (atom zprint.config/default-zprint-options)
                        zprint.config/explained-options
                          (atom zprint.config/default-zprint-options)
                        zprint.config/explained-sequence (atom 1)]
            (set-options! {:style :justified})
            (set-options! {:style :community})
            (get-options)))


  (expect (more-of options
            true (:justify? (:binding options))
            true (:justify? (:map options))
            true (:justify? (:pair options))
            0 (:indent (:binding options))
            1 (:indent-arg (:list options))
            0 (:indent (:map options))
            0 (:indent (:pair options))
            :none ((:fn-map options) "apply")
            :none ((:fn-map options) "assoc")
            :none ((:fn-map options) "filter")
            :none ((:fn-map options) "filterv")
            :none ((:fn-map options) "map")
            :none ((:fn-map options) "mapv")
            :none ((:fn-map options) "reduce")
            :none ((:fn-map options) "remove")
            :none-body ((:fn-map options) "with-meta"))
          (with-redefs [zprint.config/configured-options
                          (atom zprint.config/default-zprint-options)
                        zprint.config/explained-options
                          (atom zprint.config/default-zprint-options)
                        zprint.config/explained-sequence (atom 1)]
            (set-options! {:style [:community :justified]})
            (get-options)))

  (expect (more-of options
            true (:nl-separator? (:extend options))
            true (:flow? (:extend options))
            0 (:indent (:extend options)))
          (with-redefs [zprint.config/configured-options
                          (atom zprint.config/default-zprint-options)
                        zprint.config/explained-options
                          (atom zprint.config/default-zprint-options)
                        zprint.config/explained-sequence (atom 1)]
            (set-options! {:style :extend-nl})
            (get-options)))

  (expect (more-of options
            true (:nl-separator? (:map options))
            0 (:indent (:map options)))
          (with-redefs [zprint.config/configured-options
                          (atom zprint.config/default-zprint-options)
                        zprint.config/explained-options
                          (atom zprint.config/default-zprint-options)
                        zprint.config/explained-sequence (atom 1)]
            (set-options! {:style :map-nl})
            (get-options)))

  (expect (more-of options
            true (:nl-separator? (:pair options))
            0 (:indent (:pair options)))
          (with-redefs [zprint.config/configured-options
                          (atom zprint.config/default-zprint-options)
                        zprint.config/explained-options
                          (atom zprint.config/default-zprint-options)
                        zprint.config/explained-sequence (atom 1)]
            (set-options! {:style :pair-nl})
            (get-options)))

  (expect (more-of options
            true (:nl-separator? (:binding options))
            0 (:indent (:binding options)))
          (with-redefs [zprint.config/configured-options
                          (atom zprint.config/default-zprint-options)
                        zprint.config/explained-options
                          (atom zprint.config/default-zprint-options)
                        zprint.config/explained-sequence (atom 1)]
            (set-options! {:style :binding-nl})
            (get-options)))

  ;;
  ;; # Test set element addition and removal
  ;;

  ; Define a new style

  (expect
    (more-of options
      {:extend {:modifiers #{"stuff"}}} (:tst-style-1 (:style-map options)))
    (with-redefs [zprint.config/configured-options
                    (atom zprint.config/default-zprint-options)
                  zprint.config/explained-options
                    (atom zprint.config/default-zprint-options)
                  zprint.config/explained-sequence (atom 1)]
      (set-options! {:style-map {:tst-style-1 {:extend {:modifiers
                                                          #{"stuff"}}}}})
      (get-options)))

  ; Apply a new style (which adds a set element)

  (expect (more-of options
            #{"static" "stuff"} (:modifiers (:extend options)))
          (with-redefs [zprint.config/configured-options
                          (atom zprint.config/default-zprint-options)
                        zprint.config/explained-options
                          (atom zprint.config/default-zprint-options)
                        zprint.config/explained-sequence (atom 1)]
            (set-options! {:style-map {:tst-style-1 {:extend {:modifiers
                                                                #{"stuff"}}}}})
            (set-options! {:style :tst-style-1})
            (get-options)))

  ; Define a new style and apply it in the same set-options! call

  (expect (more-of options
            #{"static" "stuff"} (:modifiers (:extend options)))
          (with-redefs [zprint.config/configured-options
                          (atom zprint.config/default-zprint-options)
                        zprint.config/explained-options
                          (atom zprint.config/default-zprint-options)
                        zprint.config/explained-sequence (atom 1)]
            (set-options! {:style :tst-style-1,
                           :style-map {:tst-style-1 {:extend {:modifiers
                                                                #{"stuff"}}}}})
            (get-options)))

  ; Define a new style and use it to define another style and then use
  ; that second style

  (expect
    (more-of options
      #{"static" "stuff"} (:modifiers (:extend options)))
    (with-redefs [zprint.config/configured-options
                    (atom zprint.config/default-zprint-options)
                  zprint.config/explained-options
                    (atom zprint.config/default-zprint-options)
                  zprint.config/explained-sequence (atom 1)]
      (set-options! {:style :tst-style-2,
                     :style-map {:tst-style-1 {:extend {:modifiers #{"stuff"}}},
                                 :tst-style-2 {:style :tst-style-1}}})
      (get-options)))

  ; Define two styles that reference each other, and see if we get an
  ; exception

  (expect
    #?(:clj Exception
       :cljs js/Error)
    (with-redefs [zprint.config/configured-options
                    (atom zprint.config/default-zprint-options)
                  zprint.config/explained-options
                    (atom zprint.config/default-zprint-options)
                  zprint.config/explained-sequence (atom 1)]
      (set-options! {:style-map {:x {:style :y}, :y {:style :x}}, :style :x})
      (get-options)))

  ; Define three styles that reference each other in a circle, and see if
  ; we get an exception

  (expect #?(:clj Exception
             :cljs js/Error)
          (with-redefs [zprint.config/configured-options
                          (atom zprint.config/default-zprint-options)
                        zprint.config/explained-options
                          (atom zprint.config/default-zprint-options)
                        zprint.config/explained-sequence (atom 1)]
            (set-options! {:style-map
                             {:x {:style :y}, :y {:style :z}, :z {:style :x}},
                           :style :x})
            (get-options)))


  ; Remove a set element

  (expect (more-of options
            #{"stuff"} (:modifiers (:extend options)))
          (with-redefs [zprint.config/configured-options
                          (atom zprint.config/default-zprint-options)
                        zprint.config/explained-options
                          (atom zprint.config/default-zprint-options)
                        zprint.config/explained-sequence (atom 1)]
            (set-options! {:style-map {:tst-style-1 {:extend {:modifiers
                                                                #{"stuff"}}}}})
            (set-options! {:style :tst-style-1})
            (set-options! {:remove {:extend {:modifiers #{"static"}}}})
            (get-options)))

  ; Do the explained-options work?

  ; Add and remove something

  (expect (more-of options
            #{"stuff"} (:value (:modifiers (:extend options))))
          (with-redefs [zprint.config/configured-options
                          (atom zprint.config/default-zprint-options)
                        zprint.config/explained-options
                          (atom zprint.config/default-zprint-options)
                        zprint.config/explained-sequence (atom 1)]
            (set-options! {:style-map {:tst-style-1 {:extend {:modifiers
                                                                #{"stuff"}}}}})
            (set-options! {:style :tst-style-1})
            (set-options! {:remove {:extend {:modifiers #{"static"}}}})
            (get-explained-all-options)))

  ; Add without style

  (expect (more-of options
            #{:force-nl :flow :noarg1 :noarg1-body :force-nl-body :binding
              :arg1-force-nl :arg1-force-nl-body :flow-body}
              (:value (:fn-force-nl options)))
          (with-redefs [zprint.config/configured-options
                          (atom zprint.config/default-zprint-options)
                        zprint.config/explained-options
                          (atom zprint.config/default-zprint-options)
                        zprint.config/explained-sequence (atom 1)]
            (set-options! {:fn-force-nl #{:binding}})
            (get-explained-all-options)))
  ;
  ; Tests for argument types that include options maps
  ; in :fn-map --> [<arg-type> <options-map>]
  ;

  ; Does defproject have an options map, and is it correct?

  (expect (more-of options
            true (vector? ((:fn-map options) "defproject"))
            {:vector {:wrap? false}} (second ((:fn-map options) "defproject")))
          (with-redefs [zprint.config/configured-options
                          (atom zprint.config/default-zprint-options)
                        zprint.config/explained-options
                          (atom zprint.config/default-zprint-options)
                        zprint.config/explained-sequence (atom 1)]
            (get-options)))

  ; Can we set an options map on let?

  (expect (more-of options
            true (vector? ((:fn-map options) "let"))
            {:width 99} (second ((:fn-map options) "let")))
          (with-redefs [zprint.config/configured-options
                          (atom zprint.config/default-zprint-options)
                        zprint.config/explained-options
                          (atom zprint.config/default-zprint-options)
                        zprint.config/explained-sequence (atom 1)]
            (set-options! {:fn-map {"let" [:binding {:width 99}]}})
            (get-options)))

  ; Will we get an exception when setting an invalid options map?


  (expect #?(:clj Exception
             :cljs js/Error)
          (with-redefs [zprint.config/configured-options
                          (atom zprint.config/default-zprint-options)
                        zprint.config/explained-options
                          (atom zprint.config/default-zprint-options)
                        zprint.config/explained-sequence (atom 1)]
            (set-options! {:fn-map {"let" [:binding {:width "a"}]}})
            (get-options)))

  ; Will we get an exception when setting an invalid options map inside of
  ; an otherwise valid options map?

  (expect
    #?(:clj Exception
       :cljs js/Error)
    (with-redefs [zprint.config/configured-options
                    (atom zprint.config/default-zprint-options)
                  zprint.config/explained-options
                    (atom zprint.config/default-zprint-options)
                  zprint.config/explained-sequence (atom 1)]
      (set-options!
        {:fn-map {"xx" [:arg1-body
                        {:fn-map {":export" [:flow {:list {:hang true}}]}}]}})
      (get-options)))

  ;; Test config loading via URL
  ;;
  ;; NOTE WELL: This code *MUST* return the same values as the equivalent code
  ;; in load-options! regarding the location and name of the cache file.  This
  ;; is because the tests manipulate the cache file in various ways as part of
  ;; the testing.

  #?(:clj (def url-cache-path
            (let [options {}
                  cache-loc (or (:location (:cache options)) "")
                  ; If we have no cache-loc, make it the current directory
                  cache-loc (if (empty? cache-loc)
                              zprint.core/*default-cache-loc*
                              ; If the cache-loc includes a ".", then treat it
                              ; as a Java system property, else an environment
                              ; variable.
                              (if (clojure.string/includes? cache-loc ".")
                                (System/getProperty cache-loc)
                                (System/getenv cache-loc)))
                  ; Default cache-dir to .zprint
                  cache-dir (or (:directory (:cache options))
                                zprint.core/*default-cache-dir*)
                  cache-path (str cache-loc File/separator cache-dir)
                  ; Default urldir to "urlcache"
                  urldir (or (:cache-dir (:url options))
                             zprint.core/*default-url-cache*)]
              (str cache-path File/separator urldir))))

  ;;
  ;; Test option setting via URL (--url to the uberjar)
  ;;


  #?(:clj (expect
            (more-of options
              1 (get options :max-depth))
            (let [options-file (File/createTempFile "load-options" "1")
                  cache-file (io/file url-cache-path
                                      (str "nohost_"
                                           (hash (str (.toURL options-file)))))]
              (.delete cache-file)
              (spit options-file (print-str {:max-depth 1}))
              (with-redefs [zprint.config/configured-options
                              (atom zprint.config/default-zprint-options)
                            zprint.config/explained-options
                              (atom zprint.config/default-zprint-options)
                            zprint.config/explained-sequence (atom 1)]
                (load-options! nil (.toURL options-file))
                (.delete cache-file)
                (get-options)))))

  ; Extend with set-options
  #?(:clj (expect
            (more-of options
              2 (get options :max-depth)
              22 (get options :max-length))
            (let [options-file (File/createTempFile "load-options" "2")
                  cache-file (io/file url-cache-path
                                      (str "nohost_"
                                           (hash (str (.toURL options-file)))))]
              (.delete cache-file)
              (spit options-file (print-str {:max-depth 2}))
              (with-redefs [zprint.config/configured-options
                              (atom zprint.config/default-zprint-options)
                            zprint.config/explained-options
                              (atom zprint.config/default-zprint-options)
                            zprint.config/explained-sequence (atom 1)]
                (set-options! {:max-length 22})
                (load-options! nil (.toURL options-file))
                ;(.delete cache-file)
                (get-options)))))

  ; Cached
  #?(:clj (expect
            (more-of options
              3 (get options :max-depth))
            (let [options-file (File/createTempFile "load-options" "3")
                  cache-file (io/file url-cache-path
                                      (str "nohost_"
                                           (hash (str (.toURL options-file)))))]
              (.delete cache-file)
              (spit options-file (print-str {:max-depth 3}))
              (with-redefs [zprint.config/configured-options
                              (atom zprint.config/default-zprint-options)
                            zprint.config/explained-options
                              (atom zprint.config/default-zprint-options)
                            zprint.config/explained-sequence (atom 1)]
                (load-options! nil (.toURL options-file))
                (while (not (.exists cache-file)) ;default 5 min
                                                  ;cache
                                                  ;created async in
                                                  ;ms
                       (Thread/sleep 10))
                (spit options-file (print-str {:max-depth 33})) ;unused
                                                                ;remote
                (load-options! nil (.toURL options-file))
                (.delete cache-file)
                (get-options)))))

  ; Expired cache, get rempte
  #?(:clj (expect
            (more-of options
              44 (get options :max-depth))
            (let [options-file (File/createTempFile "load-options" "4")
                  cache-file (io/file url-cache-path
                                      (str "nohost_"
                                           (hash (str (.toURL options-file)))))]
              (.delete cache-file)
              (spit options-file (print-str {:max-depth 4}))
              (with-redefs [zprint.config/configured-options
                              (atom zprint.config/default-zprint-options)
                            zprint.config/explained-options
                              (atom zprint.config/default-zprint-options)
                            zprint.config/explained-sequence (atom 1)]
                (load-options! nil (.toURL options-file))
                (while (not (.exists cache-file)) (Thread/sleep 10))
                ; expired cache
                (spit cache-file
                      (print-str {:expires 0, :options {:max-depth 4}}))
                ;used remote
                (spit options-file (print-str {:max-depth 44}))
                (load-options! nil (.toURL options-file))
                (.delete cache-file)
                (get-options)))))

  ; Good url, corrupt cache
  #?(:clj (expect
            (more-of options
              5 (get options :max-depth))
            (let [options-file (File/createTempFile "load-options" "5")
                  cache-file (io/file url-cache-path
                                      (str "nohost_"
                                           (hash (str (.toURL options-file)))))]
              (.delete cache-file)
              (spit options-file (print-str {:max-depth 5}))
              (with-redefs [zprint.config/configured-options
                              (atom zprint.config/default-zprint-options)
                            zprint.config/explained-options
                              (atom zprint.config/default-zprint-options)
                            zprint.config/explained-sequence (atom 1)]
                (spit cache-file "{bad-cache")       ;corrupt edn
                (load-options! nil (.toURL options-file))
                (.delete cache-file)
                (get-options)))))

  #?(:clj (expect Exception
                  (with-redefs [zprint.config/configured-options
                                  (atom zprint.config/default-zprint-options)
                                zprint.config/explained-options
                                  (atom zprint.config/default-zprint-options)
                                zprint.config/explained-sequence (atom 1)]
                    (load-options! nil "http://b.a.d.u.r.l")
                    (get-options))))
  ; Bad url, no cache

  ; Write url, bad content, no cache
  #?(:clj (expect
            Exception
            (let [options-file (File/createTempFile "url-bad-content" "1")]
              (spit options-file "{bad-content")
              (with-redefs [zprint.config/configured-options
                              (atom zprint.config/default-zprint-options)
                            zprint.config/explained-options
                              (atom zprint.config/default-zprint-options)
                            zprint.config/explained-sequence (atom 1)]
                (load-options! nil (.toURL options-file))))))

  ; Bad url, but cache
  #?(:clj (expect
            (more-of options
              6 (get options :max-depth))
            (let [options-file (File/createTempFile "load-options" "6")
                  cache-file (io/file url-cache-path
                                      (str "nohost_"
                                           (hash (str (.toURL options-file)))))]
              (.delete cache-file)
              (spit options-file (print-str {:max-depth 6}))
              (with-redefs [zprint.config/configured-options
                              (atom zprint.config/default-zprint-options)
                            zprint.config/explained-options
                              (atom zprint.config/default-zprint-options)
                            zprint.config/explained-sequence (atom 1)]
                (load-options! nil (.toURL options-file))
                (while (not (.exists cache-file)) (Thread/sleep 10))
                (.delete options-file)               ;break url
                (load-options! nil (.toURL options-file))
                (.delete cache-file)
                (get-options)))))

  ; Bad url, expired cache
  #?(:clj (expect
            (more-of [options std-err]
              7 (get options :max-depth)
              true (some? (re-matches #"WARN: using expired cache config for.*"
                                      (str/trim std-err))))
            (let [options-file (File/createTempFile "load-options" "7")
                  cache-file (io/file url-cache-path
                                      (str "nohost_"
                                           (hash (str (.toURL options-file)))))
                  baos (ByteArrayOutputStream.)]
              (System/setErr (PrintStream. baos))
              (.delete cache-file)
              (with-redefs [zprint.config/configured-options
                              (atom zprint.config/default-zprint-options)
                            zprint.config/explained-options
                              (atom zprint.config/default-zprint-options)
                            zprint.config/explained-sequence (atom 1)]
                ; expire cache
                (spit cache-file
                      (print-str {:expires 0, :options {:max-depth 7}}))
                ; break url
                (.delete options-file)               ;break url
                (try (load-options! nil (.toURL options-file))
                     (finally (.delete cache-file)))
                [(get-options) (str baos)]))))

  ; max-age for cache expiry and overrides Expires,
  ; else Expires by itself sets cache

  #?(:clj (expect
            (more-of [options cache1 cache2]
              true (<= (System/currentTimeMillis)
                       (:expires cache1)
                       (+ 1e7 (System/currentTimeMillis)))
              true (<= (System/currentTimeMillis)
                       (:expires cache1)
                       (.getTime (Date. (- 2999 1900) 9 19))
                       (:expires cache2)
                       (.getTime (Date. (- 2999 1900) 9 23))))
            (with-redefs [zprint.config/configured-options
                            (atom zprint.config/default-zprint-options)
                          zprint.config/explained-options
                            (atom zprint.config/default-zprint-options)
                          zprint.config/explained-sequence (atom 1)]
              (let [body "{:max-depth 8}"
                    first-request (atom true)
                    http-server
                      ; any port will do
                      (doto (HttpServer/create (InetSocketAddress. "0.0.0.0" 0)
                                               0)
                        (.createContext
                          "/cfg"
                          (reify
                            HttpHandler
                              (handle [this ex]
                                (if @first-request
                                  (.add (.getResponseHeaders ex)
                                        "Cache-Control"
                                        (format "max-age=%s" (int 1e4))))
                                (.add (.getResponseHeaders ex)
                                      "Expires"
                                      "Wed, 21 Oct 2999 00:00:00 GMT")
                                (.sendResponseHeaders ex 200 (count body))
                                (reset! first-request false)
                                (doto (io/writer (.getResponseBody ex))
                                  (.write body)
                                  (.close)))))
                        (.setExecutor nil)
                        (.start))
                    server (.getAddress http-server)
                    url (format "http://0.0.0.0:%s/cfg" (.getPort server))]
                (let [cache-file (io/file url-cache-path
                                          (str "0.0.0.0_" (hash url)))]
                  (.delete cache-file)
                  (load-options! nil url)
                  (let [cache1 (-> cache-file
                                   slurp
                                   edn/read-string)]
                    (.delete cache-file)
                    (load-options! nil url)
                    (let [cache2 (-> cache-file
                                     slurp
                                     edn/read-string)]
                      (.stop http-server 0)
                      (.delete cache-file)
                      [(get-options) cache1 cache2])))))))

  ; Cached via url-cache-path and url-cache-secs

  #?(:clj (expect (more-of [options cache]
                    9 (:max-depth options)
                    9 (get-in cache [:options :max-depth])
                    0 (:cache-secs (:url options))
                    true (< (:expires cache) (System/currentTimeMillis)))
                  (let [options-file (File/createTempFile "load-options" "9")
                        cache-file (File/createTempFile "cache-file" "9")]
                    (spit options-file (print-str {:max-depth 9}))
                    (with-redefs [zprint.config/configured-options
                                    (atom zprint.config/default-zprint-options)
                                  zprint.config/explained-options
                                    (atom zprint.config/default-zprint-options)
                                  zprint.config/explained-sequence (atom 1)]
                      (set-options! {:configured? true,
                                     :url {:cache-secs 0,
                                           ; cache-path only exists for this
                                           ; kind of test, not available for
                                           ; general use
                                           :cache-path (.getPath cache-file)}})
                      (load-options! (get-options) (.toURL options-file))
                      (Thread/sleep 1)      ;make sure expires
                      (.delete options-file)
                      (load-options! (get-options) (.toURL options-file))
                      (while (not (.exists cache-file)) (Thread/sleep 10))
                      [(get-options)
                       (-> cache-file
                           slurp
                           edn/read-string)]))))

  ;;
  ;; Check coercion of non-boolean value for boolean keys
  ;;
  ;; Issue #111
  ;;

  (expect
    (more-of options
      false (boolean (:to-string? (:record options))))
    (with-redefs [zprint.config/configured-options
                    (atom zprint.config/default-zprint-options)
                  zprint.config/explained-options
                    (atom zprint.config/default-zprint-options)
                  zprint.config/explained-sequence (atom 1)]
      (set-options! {:coerce-to-false 'stuff, :record {:to-string? 'stuff}})
      (get-options)))


  ;;
  ;; Check to see that second options map (if any) is validated.
  ;;
  ;; Issue #121 enhancements for more option maps in :fn-map vectors.
  ;;

  (expect #?(:clj Exception
             :cljs js/Error)
          (with-redefs [zprint.config/configured-options
                          (atom zprint.config/default-zprint-options)
                        zprint.config/explained-options
                          (atom zprint.config/default-zprint-options)
                        zprint.config/explained-sequence (atom 1)]
            (set-options! {:fn-map {"quote" [:replace-w-string
                                             {:list {:replacement-string "'"}}
                                             ; This is incorrect, and should
                                             ; force an Exception -- bad second
                                             ; map.
                                             {:stuff :bother}]}})
            (get-options)))

  (expect #?(:clj Exception
             :cljs js/Error)
          (with-redefs [zprint.config/configured-options
                          (atom zprint.config/default-zprint-options)
                        zprint.config/explained-options
                          (atom zprint.config/default-zprint-options)
                        zprint.config/explained-sequence (atom 1)]
            (set-options!
              {:fn-map {"quote" [:replace-w-string
                                 {:list {:replacement-string "'"}} {:width 20}
                                 ; This is incorrect, and should
                                 ; force an Exception -- can't have
                                 ; more than two maps.
                                 {}]}})
            (get-options)))


  ;;
  ;; Can we input fn definitions on a single call?
  ;;

  (expect "{:a :b}"
          (zprint-str {:a :b} {:vector {:option-fn-first (fn [x y] {})}}))

  ;;
  ;; ## Test only-set (used in :explain-set)
  ;;

  (expect
    {:list {:hang? {:set-by "repl or api call 3", :value false}},
     :parallel? {:set-by "repl or api call 4", :value false},
     :style-map {:test {:list {:hang? {:set-by "repl or api call 2",
                                       :value true}}}}}
    (only-set
      {:input {:range {:end nil, :start nil}},
       :list {:constant-pair-min 4,
              :constant-pair? true,
              :hang-avoid 0.5,
              :hang-diff 1,
              :hang-expand 2.0,
              :hang-size 100,
              :hang? {:set-by "repl or api call 3", :value false},
              :indent 2,
              :indent-arg nil},
       :pair-fn {:hang-diff 1, :hang-expand 2.0, :hang-size 10, :hang? true},
       :parallel? {:set-by "repl or api call 4", :value false},
       :parse {:interpose nil, :left-space :drop},
       :style nil,
       :style-map {:all-hang {:extend {:hang? true},
                              :list {:hang? true},
                              :map {:hang? true},
                              :pair {:hang? true},
                              :pair-fn {:hang? true},
                              :reader-cond {:hang? true},
                              :record {:hang? true}},
                   :test {:list {:hang? {:set-by "repl or api call 2",
                                         :value true}}}},
       :tab {:expand? true, :size 8},
       :width 80}))

  ;;
  ;; # See if we can get calculated-options to work with config-and-validate
  ;;

  (expect
    "(defn cvtest\n  \"This is to see whether we need determine options.\"\n  []\n  (let [x {:c 3, :a 1, :b 2}\n        y #{:z :y :z}]\n    (if (= (:b x) 2) (println \"hi\") (println \"there\"))))"
    (zprint-str
      "(defn cvtest\n  \"This is to see whether we need determine options.\"\n  []\n  (let [x {:b 2 :c 3 :a 1}\n        y #{:z :y :z}]\n   (if (= (:b x) 2)\n     (println \"hi\")\n     (println \"there\"))))\n"
      {:parse-string? true,
       :fn-map {"defn" [:arg1-body
                        {:map {:key-order [:c], :sort-in-code? true}}]}}))

  ;
  ; Define one new complex style and then apply two complex styles
  ;
  ; Issue #217

  (expect
    (more-of options
      [:flow {:style [:jimportguide :ij-var]}] (get (:fn-map options) ":import")
      [:flow {:style [:jrequireguide :rj-var]}] (get (:fn-map options)
                                                     ":require")
      [:flow {:style [:jrequiremacrosguide :rjm-var]}] (get (:fn-map options)
                                                            ":require-macros")
      0 (:indent (:map options))
      true (:nl-separator? (:map options))
      {:doc "style :a", :style [:map-nl :keyword-respect-nl]} (:a (:style-map
                                                                    options)))
    (with-redefs [zprint.config/configured-options
                    (atom zprint.config/default-zprint-options)
                  zprint.config/explained-options
                    (atom zprint.config/default-zprint-options)
                  zprint.config/explained-sequence (atom 1)]
      (set-options! {:style [:a :ns-justify],
                     :style-map {:a {:doc "style :a",
                                     :style [:map-nl :keyword-respect-nl]}}})
      (get-options)))


  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;;
  ;; End of defexpect
  ;;
  ;; All tests MUST come before this!!!
  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
)
