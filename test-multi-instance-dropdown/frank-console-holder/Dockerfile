ARG FF_VERSION=latest
FROM frankframework/frankframework:${FF_VERSION}
ARG PG_VERSION
ADD --chown=tomcat https://jdbc.postgresql.org/download/postgresql-${PG_VERSION}.jar /opt/frank/drivers/postgresql-${PG_VERSION}.jar
