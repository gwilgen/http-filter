# http-filter
conditional expression based filter

This project is an attempt to create an equivalent object to Spring Pageable, but for filtering purposes.

The core module has an Antlr4 grammar to support basic conditional expressions

The spring module has the components that allows bean injection in controller, and Querydsl translation. Examples show how to use this library:

```/list?filter=(name eq "Foo" or age gt 40) and "Springfield" in db_func(main_address_id)```

This has the power of passing the where _alike_ clause via http, but without SQL Injection

Some trade-offs have yet to be made to pass a json object (e.g. `distance(location, "{\"\lat\":42,\"long\":3}") lt 200`), but adding the proper function to db works (e.g. `custom_distance(lat, long, 42, 3)`)