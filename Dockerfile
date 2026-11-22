FROM tomcat:9.0-jdk21-temurin

RUN rm -rf /usr/local/tomcat/webapps/*

COPY WebContent /tmp/webcontent
COPY src /tmp/src

RUN javac -encoding UTF-8 \
    -cp "/usr/local/tomcat/lib/servlet-api.jar:/tmp/webcontent/WEB-INF/lib/gson.jar:/tmp/webcontent/WEB-INF/lib/mysql-connector-j.jar" \
    -d /tmp/webcontent/WEB-INF/classes \
    $(find /tmp/src -name "*.java" | sort)

RUN mv /tmp/webcontent /usr/local/tomcat/webapps/ROOT

EXPOSE 8080
