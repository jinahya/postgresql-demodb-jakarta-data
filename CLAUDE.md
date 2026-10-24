# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this project is

Jakarta Data 1.0 repositories over the Jakarta Persistence mappings of
[`postgresql-demodb-persistence`](https://github.com/jinahya/postgresql-demodb-persistence), which maps the
[PostgreSQL Demonstration Database](https://postgrespro.com/community/demodb) (Airlines; database `demo`, schema
`bookings`). A single, parent-less jar. The sibling `../postgresql-demodb-spring-data` does the same in Spring Data
JPA, but this module does **not** follow its shape or rules; design for Jakarta Data on its own terms. The author's
background is Java EE 5/6, so plain DAO-style explanations and container-free code are preferred.

- Main package: `com.github.jinahya.postgresql.demodb.persistence.data` (`@NullMarked`, JSpecify; a lookup that may
  find nothing returns `Optional`).
- Entities: `com.github.jinahya.postgresql.demodb.persistence`, one package up, **in this project's `src/main`**,
  one per table and view (`Airplane`, `AirplaneData`, `Airport`, `AirportData`, `BoardingPass`, `Booking`, `Flight`,
  `Route`, `Seat`, `Segment`, `Ticket`, `Timetable`, with the id classes `BoardingPassId`, `RouteId`, `SeatId`,
  `SegmentId`). Each extends a mapped superclass of the upstream artifact
  `io.github.jinahya:postgres-demonstration-persistence` (`MappedFlight`, ...), and their static metamodel
  (`Flight_`, extending upstream's `MappedFlight_`) is generated here. The three views are entities too, so they
  have repositories like the tables.
- One repository per entity, `<Entity>Repository`, annotated `@Repository`. Each extends **both**
  `_Repository<Entity, Id>` (the unannotated shared base; Jakarta Data needs no `@NoRepositoryBean`) **and**
  `CrudRepository<Entity, Id>` directly. Drop the direct `CrudRepository` and `hibernate-processor` fails with
  "repository does not have a well-defined primary entity type for inherited member 'deleteById'". A query method in
  `_Repository` is implemented per repository with the concrete types, and makes the processor emit a harmless,
  empty `_Repository_` metamodel class.
- **One provider, no provider profiles.** Hibernate Data Repositories is the only Jakarta Data provider for Java SE:
  `hibernate-processor` implements every `@Repository` interface at compile time as a `<Name>_` class under
  `target/generated-sources/annotations`, which runs on an `org.hibernate.StatelessSession`. EclipseLink 5.0 has no
  Jakarta Data, so unlike upstream there is no EclipseLink axis here.
- **The processor's Jakarta Data metamodel is suppressed** (`-AsuppressJakartaDataMetamodel=true` in the pom): with
  `jakarta.data` on the classpath the processor would also write `_Flight extends _MappedFlight`, and upstream,
  compiled without `jakarta.data`, never generated `_MappedFlight`. The repositories are generated against the
  Jakarta Persistence metamodel, which is enough.
- No repository names a `dataStore`; the unit is the caller's choice. The generated `<Name>_` has a `public`
  constructor taking a `StatelessSession` (container-free use) and an `@Inject` no-arg constructor that opens its
  session from a `@PersistenceUnit` (CDI use).
- The published jar carries **the entities and the repositories**. `jakarta.data-api`, `jakarta.persistence-api`,
  `jakarta.validation-api`, `jakarta.inject-api`, `cdi-api`, `hibernate-core`, `jspecify` and Lombok are all
  `provided`; the consumer brings its own Jakarta Data, provider and (to enforce constraints on repository
  parameters) a validation provider. With `jakarta.inject` / CDI on the processor's classpath, the generated
  `<Name>_` is a `@Dependent` CDI bean with an `@Inject` constructor.
- `-parameters` is on (`maven.compiler.parameters`), as upstream: Jakarta Data binds query parameters by name.

## Upstream, and the entities

The sibling checkout `../postgresql-demodb-persistence` publishes, at `0.0.1-SNAPSHOT`, a jar of **mapped
superclasses, their generated static metamodel (`MappedFlight_`, ...) and two converters**
(`IntegerListArrayConverter` for `integer[]`, `IntervalConverter` for `interval`), and a second jar of the same
artifact classified `test-companion` (`*_Randomizer` / `*_Persister` classes, and the `__Random_TestUtils` helper
they draw their values from). Both exist **only in the local `~/.m2`**: after any change upstream, run
`./mvnw -DskipTests install` there before building here. Its `persistence.xml` is test-only, so this project supplies
its own.

- **Upstream keeps its entities in `src/test`**, on purpose: which associations an entity maps, lazy or not, with
  what cascade, is an application's decision, and this project is that application. The entities here are copies of
  upstream's test entities, verbatim (`src/test/java/com/github/jinahya/postgresql/demodb/persistence/<Entity>.java`
  there), in the same package, so that the `test-companion` randomizers and persisters, compiled against that
  package, find them. **Keep them in step with upstream**: diff against upstream's before touching a mapping, and
  change the mapping there first.
- Do not copy upstream's randomizers or persisters into `src/test`; the `test-companion` jar is on the test
  classpath. Podam and `jinahya-persistence-test-utils` are declared here because that jar brings no dependencies.
- The version properties in `pom.xml` (`version.jinahya-persistence`, `version.uk.co.jemos.podam`, Hibernate ORM
  `7.4.12.Final`, H2, `postgresql`, Hibernate Validator ...) deliberately mirror upstream's, so what compiles here
  against the test-companion jar is what that jar was compiled against, and the generated code runs on the ORM that
  generated upstream's metamodel. Change them in step with upstream, not independently.
- **The exotic columns.** `routes.days_of_week` (`integer[]`) is a `List<Integer>` and `routes.duration`
  (`interval`) a `Duration`, through upstream's converters; `tstzrange`, `point` and `jsonb` columns are `String`s
  holding the server's text. A query filters or orders by them as text; what needs the server's operators (`@>`,
  `->>`, `lower(validity)`) is native SQL, and such an exercise is native-only. Writing any of the five needs
  `stringtype=unspecified` on the JDBC URL, which the `demonstration-it` unit has.
- **Time zones.** `timestamptz` columns are `OffsetDateTime`s, normalized to UTC on the way in. The JDBC session's
  time zone is UTC, as the container's own `psql` sessions have it, through `hibernate.connection.init_sql`
  (`SET TIME ZONE 'UTC'` in the `demonstration-it` unit), so `extract(month from f.scheduledDeparture)` gives the
  month upstream's exercises show. The driver sends the JVM's zone as the session's `TimeZone` startup parameter,
  which the server applies after any `options=-c TimeZone=...` of the URL, so the URL cannot do it.
- Upstream's `../postgresql-demodb-persistence/EXERCISES.asciidoc` / `EXERCISES.yaml` is the catalogue of query
  exercises the sibling projects answer: for each repository, the Basic and the Intermediate pick of the exercise's
  section become one method each, the exercise id named in the method's javadoc, with the exercise's order written
  in the query and its `LIMIT n` left to the caller's `PageRequest.ofSize(n)`. A result that is not an entity is a
  nested record of the repository. The `SCHEMA` section is not answered.
- Identifiers are assigned (`airplaneCode`, `airportCode`, `bookRef`, `ticketNo`, the composite ones), except
  `flights.flight_id`, which is `IDENTITY`. `Route` has no key of its own in the schema; its `@EmbeddedId` is
  `RouteId` (`routeNo`, `validity`), and the inherited `routeNo`/`validity` attributes are read-only there.
- Read `../postgresql-demodb-persistence/README.md` before touching anything about the mappings.

## Build

Maven wrapper (`./mvnw`); the build JDK must be **25** (enforced). Main and test sources both compile at release 25.
Every script below runs `clean` on purpose: a renamed or removed `@Repository` interface leaves a stale generated
`<Name>_` class that the next incremental compile fails on with an unexplained "cannot find symbol".

```sh
./_mvn_test.sh                         # unit tests on H2 in PostgreSQL mode (unit `demonstration-test`); no database needed
./_mvn_verify.sh                       # + integration tests (*IT) against the PostgreSQL below (unit `demonstration-it`)
./_mvn_verify_notests.sh               # full lifecycle, tests compiled but not run; no database needed
./_mvn_test.sh -Dtest=FlightRepository_Test             # one test class
./_mvn_test.sh -Dtest=FlightRepository_Test#method      # one test method
./_mvn_verify.sh -DskipTests           # the ITs alone
./_mvn_verify.sh -DskipITs             # the unit tests alone
./_mvn_verify.sh -Dit.test=FlightRepository_IT          # one IT class
```

Failsafe is bound only by the `failsafe` profile (`-Pfailsafe`), picking up `**/*IT.java`; a plain `./mvnw verify`
never runs an IT. As of surefire/failsafe 3.6.0, `-DskipTests` reaches surefire only, so `-DskipITs` is the flag
that stops the ITs. Both plugins stop after the first failing class (`skipAfterFailureCount=1`). The enforcer fails
the build on dependency non-convergence, a JDK below 25, and a `-P` naming a profile that does not exist.

**The integration tests need the demonstration database container upstream owns**: run
`../postgresql-demodb-persistence/_docker_compose_up.sh` (Ctrl-C takes it down with its data). It runs
`onacit/postgresql-demodb:18-alpine-3m` on `localhost:5432`, database `demo`, `postgres` / `password`, with three
months of flights loaded on first start, which takes about a minute. The `demonstration-it` unit connects there with
`hbm2ddl.auto=none`: the sample data is the fixture the ITs read, and nothing may touch the schema or leave rows
behind.

## Tests

A Weld SE container per test method (`@EnableAutoWeld` on `__Repository_TestBase<R, T, K>`). The container creates the
generated `<R>_` through its `@Inject` constructor; the concrete test adds the class and names its types:

```java
@AddBeanClasses(FlightRepository_.class)
class FlightRepository_Test
        extends _Repository_Test<FlightRepository, Flight, Integer> {

    FlightRepository_Test() {
        super(FlightRepository.class, Flight.class, Integer.class);
    }
}
```

`getRepositoryInstance()` returns the repository, one `@Dependent` instance (and `StatelessSession`) per test.
`_Repository_Test` (unit `demonstration-test`, empty H2 per test, insert what you query) and `_Repository_IT` (unit
`demonstration-it`, read the sample data, roll back any write) each add, with an inherited `@AddBeanClasses`, a
nested `EntityManagerFactoryProducer` for their unit. A write needs the repository's own session and its transaction
(`session(getRepositoryInstance()).inTransaction(...)`), or the injected `EntityManagerFactory`'s
`callInTransaction(em -> ...)` with upstream's persisters (`EntityPersisterUtils.newPersistedInstanceOf(em, X.class)`).
Three things are load-bearing:

- `<R>_` takes the factory from a `@PersistenceUnit` field, resource injection that no producer reaches and Weld SE
  does not fill. `__WeldJpaInjectionServices`, a test-only Weld `JpaInjectionServices` registered in
  `src/test/resources/META-INF/services/org.jboss.weld.bootstrap.api.Service`, hands it the produced factory. Without
  it the field stays `null` (NPE in the generated `@PostConstruct`).
- The producer is `@Singleton`, not `@ApplicationScoped`: `<R>_`'s `unwrap(SessionFactory.class)` on a client proxy
  throws `ClassCastException`.
- The base does not `@Inject R`: an injection point typed by a type variable is a CDI definition error
  (WELD-001407), even when the subclass binds it. It injects `Instance<Object>` and selects `repositoryClass`.

- `META-INF/persistence.xml`: units `demonstration-test` (H2 `MODE=PostgreSQL`, `create-drop`; the three views are
  tables there, generated from their entities) and `demonstration-it` (PostgreSQL, `none`), mirroring upstream's; keep
  the two files in step. Both list every entity and the two converters explicitly, since `exclude-unlisted-classes`
  is on.
- `logback-test.xml`: root DEBUG, Hibernate at WARN except `org.hibernate.SQL` (DEBUG) and `org.hibernate.orm.jdbc.bind`
  (TRACE) for statements and bind values.

The test classpath also has Hibernate Validator + Expressly (to enforce the repositories' parameter constraints),
JUnit 5, AssertJ, Mockito, and the upstream `test-companion` randomizers/persisters.
