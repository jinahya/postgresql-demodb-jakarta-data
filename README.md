# postgresql-demodb-jakarta-data

[Jakarta Data](https://jakarta.ee/specifications/data/1.0/) repositories for the
[PostgreSQL Demonstration Database](https://postgrespro.com/community/demodb) (Airlines; database `demo`, schema
`bookings`), over the mapped superclasses of
[postgresql-demodb-persistence](https://github.com/jinahya/postgresql-demodb-persistence).

The sibling [postgresql-demodb-spring-data](https://github.com/jinahya/postgresql-demodb-spring-data) is the same
thing in Spring Data JPA.

## The entities

Upstream publishes mapped superclasses (`MappedFlight`, ...), their static metamodel and two converters, and keeps its
entities in its test sources: which associations to map, and how, is an application's decision. This project is that
application. Its entities, in `com.github.jinahya.postgresql.demodb.persistence`, one per table and view, are copies
of upstream's test entities, kept in step with them; the repositories, in `com.github.jinahya.postgresql.demodb.persistence.data`,
are one per entity.

## The provider

One: [Hibernate Data Repositories](https://docs.hibernate.org/orm/7.4/repositories/html_single/Hibernate_Data_Repositories.html).
`hibernate-processor` implements every `@Repository` interface at compile time as a `<Name>_` class, which runs on
a `StatelessSession` of Hibernate ORM. EclipseLink 5.0 implements Jakarta Persistence 3.2 only, no Jakarta Data,
so unlike upstream this build has no provider profiles.

## The database

Upstream owns the compose file:

```shell
$ cd ../postgresql-demodb-persistence && ./_docker_compose_up.sh
```

brings up `onacit/postgresql-demodb:18-alpine-3m` on `localhost:5432` as `postgres` / `password`, with the
demonstration database loaded (three months of flights); Ctrl-C takes it down again. The `demonstration-it` unit of
[`persistence.xml`](src/test/resources/META-INF/persistence.xml) points there, and sets each session's time zone
to UTC, as the container's own `psql` sessions have it, so that a month or a day a query takes out of a
`timestamptz` is the one upstream's exercises show.

## Building

```shell
$ ./_mvn_test.sh              # unit tests, H2 in PostgreSQL mode
$ ./_mvn_verify.sh            # + integration tests, against the database above
$ ./_mvn_verify_notests.sh    # no database needed
```

The upstream jar, and its `test-companion` jar of randomizers and persisters, are local `SNAPSHOT`s:
`./mvnw -DskipTests install` in `../postgresql-demodb-persistence` first.

## The exercises

Upstream's [EXERCISES.asciidoc](https://github.com/jinahya/postgresql-demodb-persistence/blob/develop/EXERCISES.asciidoc)
states the exercises on the installed data, each with an id (`DEMODB_FLIGHTS_B_01`, ...). Each repository here
answers the Basic and the Intermediate pick of its section, one method per exercise, the id named in the method's
javadoc.
