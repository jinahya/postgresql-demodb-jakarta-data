#!/bin/sh
# Runs `verify`: the whole lifecycle -- compile, package, enforcer -- with both the unit tests and the
# integration tests executed.
#
# The ITs talk to a real PostgreSQL at localhost:5432/demo (the `demonstration-it` unit), which upstream owns; bring
# it up with
#
#     cd ../postgresql-demodb-persistence && ./_docker_compose_up.sh
#
# first; otherwise they fail on the connection rather than on a query.
#
# `failsafe` is the profile that binds maven-failsafe-plugin.
#
# As of 3.6.0 failsafe no longer binds its own skipTests parameter to the skipTests user property, so the two
# halves are selected separately -- and that is a property of ${version.maven-failsafe}; under 3.5.x and earlier
# -DskipTests reached both:
#
#     ./_mvn_verify.sh -DskipTests   # the ITs alone
#     ./_mvn_verify.sh -DskipITs     # the unit tests alone
#     ./_mvn_verify_notests.sh       # neither
#
# `clean` is deliberate; see _mvn_test.sh.
cd "$(dirname "$0")" || exit 1

exec ./mvnw -Pfailsafe clean verify "$@"
