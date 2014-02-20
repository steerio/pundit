# Pundit

A Clojure client library to access the [Parse](http://parse.com) PaaS backend
through its [REST API](https://www.parse.com/docs/rest).

This README is an introduction to the usage of the library.

## Authentication

You can choose between "permanent" authentication and an auth binding:

```clojure
(require '[pundit.api :as pa])

(pa/connect! [app-id app-key])

; OR

(pa/with-auth [app-id app-key]
  (do-things-here))
```

## Basic CRUD operations

```clojure
user=> (pa/create "Klass" {:foo 123 :bar "Hello"})
{:bar "Hello", :foo 123, :created-at "2014-02-19T17:42:12.947Z", :object-id
"ZwyZBrgKgU", :updated-at "2014-02-19T17:42:12.947Z", :class-name "Klass"}

user=> (pa/update obj {:bar "Namaste"})
{:updated-at "2014-02-19T17:43:07.221Z"}

user=> (pa/update "Klass" "ZwyZBrgKgU" {:bar "Namaste"})
{:updated-at "2014-02-19T17:44:22.453Z"}

user=> (pa/delete obj)
{}

user=> (pa/delete "Klass" "ZwyZBrgKgU")
{}
```

## Querying

### Overview

The `pundit.api/query` function returns a seqable query object, which represents a
query that has not been executed yet. It is immutable, but it is possible to
generate modified query objects out of it.

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
({:object-id "bgQwXqBtEu", :updated-at "2014-02-18T20:49:42.559Z", ..... })

user=> (pa/find-one "Klass" :where {:foo 123} :include :baz)
{:object-id "bgQwXqBtEu", :updated-at "2014-02-18T20:49:42.559Z", ..... }
```

### Notes on composing where queries

When adding to a query, a `:where` clause with an exact match will always
overwrite comparisons, because it makes them obsolete. Otherwise they will be
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
PARSE API to mix exact queries with comparisons. There is no `$eq`.

### Automatic paging

Queries returning many objects will be requested in paged windows
automatically. Users will see one continuous lazy sequence, which they can use
as any other sequence in Clojure.

Always use the `:limit` clause if you don't want this behaviour, do not rely on
limits imposed by the Parse API!

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
"2014-02-19T17:05:24.640Z", :created-at "2014-02-19T16:33:23.457Z"}
```

To generate pointers out of objects, IDS, sequences of objects and IDs for
querying or updating, you can use the following functions:

```clojure
user=> (pa/ptr obj)
{:__type "Pointer", :class-name "Klass", :object-id "bgQwXqBtEu"}

user=> (pa/ptr "OtherKlass" "Tp0ULkm8j1")
{:__type "Pointer", :class-name "OtherKlass", :object-id "Tp0ULkm8j1"}

user=> (pa/ptrs (list obj other-obj))
({:__type "Pointer", :class-name "Klass", :object-id "bgQwXqBtEu"}
{:__type "Pointer", :class-name "OtherKlass", :object-id "Tp0ULkm8j1"})

user=> (pa/ptrs "Klass" (list id other-id))
({:__type "Pointer", :class-name "Klass", :object-id "xAvwwBjCf0"}
{:__type "Pointer", :class-name "Klass", :object-id "iNXDFjYdre"})
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

## TODO

* Batched requests
* More documentation
* Improved test coverage

## License

Copyright © 2013-2014 Roland Venesz

Distributed under the Eclipse Public License, the same as Clojure.
