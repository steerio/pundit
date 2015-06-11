(ns pundit.test.user-auth
  (:require [clojure.test :refer :all]
            [pundit.test.helper :refer :all]
            [pundit.api :as pa]))

(use-fixtures :once authenticate)

(deftest logging-in
  (let [email (props "user.email")
        token (pa/login email (props "user.password"))]
    (is (string? token))
    (is (= (:email (pa/with-token token (pa/whoami)))
           email))))
