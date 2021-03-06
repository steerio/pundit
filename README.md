# Pundit

A Clojure client library to access the [Parse](http://parse.com) PaaS backend
through its [REST API](https://www.parse.com/docs/rest).

This README is an introduction to the usage of the library.

## Authentication

You can choose between "permanent" authentication and an auth binding:

```clojure
(require '[pundit.api :as pa])

(pa/connect! {:app "foo"
              :api-key "bar"
              :master-key "baz"})

; OR

(pa/with-auth same-map-as-above
  (do-things-here))
```

You can switch to the master key like this:

```clojure
(pa/with-master
  (pa/find-all "Foo"))
```

You can authenticate as a Parse user (and you can obviously save the token for
later, too):

```clojure
(pa/with-token (pa/login email password)
  (pa/find-all "Foo"))
```

This form will log your user in, and log them out in a `finally` clause once
the body is completed:

```clojure
(pa/with-login ["foo@bar.com" "abc123"]
  (pa/find-all "Foo"))
```

## Basic CRUD operations

```clojure
user=> (pa/create! "Klass" {:foo 123 :bar "Hello"})
{:bar "Hello", :foo 123, :created-at #<DateTime 2014-02-19T17:42:12.947Z>,
:object-id "ZwyZBrgKgU", :updated-at #<DateTime 2014-02-19T17:42:12.947Z>,
:class-name "Klass"}

user=> (pa/update! obj {:bar "Namaste"})
{:updated-at #<DateTime 2014-02-19T17:43:07.221Z>}

user=> (pa/update! "Klass" "ZwyZBrgKgU" {:bar "Namaste"})
{:updated-at #<DateTime 2014-02-19T17:44:22.453Z>}

user=> (pa/delete! obj)
{}

user=> (pa/delete! "Klass" "ZwyZBrgKgU")
{}
```

## Batch operations

If you need the results, use `pundit.api/execute-map!`:

```clojure
(def objs
  (pa/execute-map!
    (map #(pa/create "Foo" {:num %}) (range 10)))
```

If you can throw away results, you can use `execute!` and never retain the
head:

```clojure
(pa/execute!
  (map #(pa/update % {:$inc {:num 1}}) objs))

(pa/execute!
  (map pa/delete (pa/query "Foo" :where { :owner someone })))
```

## Querying

### Overview

The `pundit.api/query` function returns a *seqable*, immutable query object that
represents a pending query.

```clojure
user=> (require '[pundit.query :refer :all])
nil

user=> (pa/query "Klass" :where {:foo 123})
#<Query Klass {:where {:foo 123}}>

user=> (where *1 {:bar "Hello"})
#<Query Klass {:where {:bar "Hello", :foo 123}}>

user=> (include *1 :baz)
#<Query Klass {:include :baz, :where {:bar "Hello", :foo 123}}>

user=> (include *1 :another)
#<Query Klass {:include [:baz :another], :where {:bar "Hello", :foo 123}}>

user=> (seq *1)
({:object-id "bgQwXqBtEu", :updated-at #<DateTime 2014-02-18T20:49:42.559Z>, ..... })

user=> (pa/find-one "Klass" :where {:foo 123} :include :baz)
{:object-id "bgQwXqBtEu", :updated-at #<DateTime 2014-02-18T20:49:42.559Z>, ..... }
```

### Notes on composing where queries

When adding to a query, a `:where` clause with an exact match will always
overwrite comparisons because it makes them obsolete. Otherwise they will be
composed as *AND*.

```clojure
user=> (where (pa/query "Klass" :where {:foo 123}) {:foo {:$lt 200}})
#<Query Klass {:where {:foo 123}}>

user=> (where (pa/query "Klass" :where {:foo {:$gt 100}}) {:foo {:$lt 200}})
#<Query Klass {:where {:foo {:$lt 200, :$gt 100}}}>

user=> (where (pa/query "Klass" :where {:foo {:$gt 100}}) {:foo 333})
#<Query Klass {:where {:foo 333}}>
```

This can lead to some unexpected results, but currently there is no way in the
PARSE API to mix exact queries with comparisons on the same key. There is no
`$eq`.

### Automatic paging

Queries returning many objects will be requested in paged windows
automatically. You get continuous lazy sequences that you can use
like any other sequence in Clojure.

Always use the `:limit` clause if you don't want all the results, do not rely
on limits imposed by the Parse API!

### Counting

The `count` function does not work on queries. It is a deliberate decision to
avoid ambiguities between local and remote counting. This might change in the
future, though.

You can use `(pa/remote-count q)` or `(count (seq q))` for server-side and
local counting respectively.

## Pointers

Pointers are returned as [delayed maps](https://github.com/steerio/delayed-map).
This means that they transparently act like fully loaded objects, which can be
useful for single requests occasionally, but for queries it's better to specify
a proper `:include` clause to avoid the *N+1 requests problem*

```clojure
user=> (def obj (pa/find-one "Klass"))
#'user/obj

user=> (:category obj)
{:object-id "zAi04Yu41O", :class-name "Category", ...}

user=> (:name (:category obj)) ; Second HTTP request happens here
"Things"

user=> (:category obj)
{:object-id "zAi04Yu41O", :class-name "Category", :name "Things", :updated-at
#<DateTime 2014-02-19T17:05:24.640Z>, :created-at #<DateTime
2014-02-19T16:33:23.457Z>}
```

Writing objects with pointer fields does not require explicitly creating
pointers. You can simply use a Parse object as the value.

```clojure
(pa/update! obj {:foo (pa/retrieve "Klass" "bgQwXqBtEu")})
```

## Calling cloud functions

```clojure
user=> (pa/call "FunctionWithoutParams")
{:some "result"}

user=> (pa/call "DoubleAllValues" {:hello 123})
{:answer {:hello 246}}
```

## Running tests

Before you run tests, please copy `resources/test.properties.tmpl` to
`resources/test.properties` and specify your authentication details.

The latter is included in `.gitignore` for safety.

## License

The library is distributed under the Eclipse Public License, the same as
Clojure.
