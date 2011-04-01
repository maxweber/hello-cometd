(defproject hello-cometd "1.0.0-SNAPSHOT"
  :description "A minimal hello world example how to use CometD in Clojure."
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [org.cometd.java/bayeux-api "2.1.0"]
                 [org.cometd.java/cometd-java-server "2.1.0"]
                 [ring/ring-jetty-adapter "0.3.7" :exclusions
                      [org.mortbay.jetty/jetty
                       org.mortbay.jetty/jetty-util]]
                 [ring/ring-servlet "0.3.7"]]
  :dev-dependencies [[org.eclipse.jetty/jetty-servlets "7.3.1.v20110307"]
                     [org.mortbay.jetty/jetty-maven-plugin "7.3.1.v20110307"]]
  :aot [hello-cometd.HelloService])
