FROM tomcat:9.0-jdk11-temurin

RUN rm -rf /usr/local/tomcat/webapps/*

COPY target/ta-recruitment.war /usr/local/tomcat/webapps/ta-recruitment.war

ENV CATALINA_OPTS="-Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

CMD ["catalina.sh", "run"]
