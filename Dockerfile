FROM tomcat:8.5-jdk11-temurin

SHELL ["/bin/bash", "-c"]

ENV JAVA_OPTS="-Djdk.xml.xpathExprGrpLimit=0 -Djdk.xml.xpathExprOpLimit=0"
ENV TZ=Europe/Vienna
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

ENV msc_data=/opt/msc-data
ENV MSC_CONFIG=${msc_data}/msc_config.json

RUN \
    apt-get -q update && \
    apt-get -qy install maven && \
    apt-get -qy install nano && \
    apt-get -qy install wget && \
    apt-get -qy install unzip && \
    wget https://github.com/Modapto/orchestrator/archive/refs/heads/main.zip -O /opt/main.zip && \
    unzip /opt/main.zip -d /opt  && \
    rm /opt/main.zip && \
    mvn -B -f /opt/orchestrator-main/microservice-controller/pom.xml clean install && \
    mvn -B -f /opt/orchestrator-main/microservice-controller-rest/pom.xml clean package && \
    unzip /opt/orchestrator-main/microservice-controller-rest/target/micro-service-controller-rest.war -d /usr/local/tomcat/webapps/micro-service-controller-rest/  && \
    rm -r /opt/orchestrator-main && \
    mkdir ${msc_data} && \
    echo 'if [ -f "${MSC_CONFIG}" ]; then exit 0; else cp /usr/local/tomcat/webapps/micro-service-controller-rest/WEB-INF/classes/org/adoxx/microservice/api/rest/config.json ${MSC_CONFIG}; fi' > /opt/initialize.sh && \
    chmod +x /opt/initialize.sh && \

    wget https://github.com/Modapto/service-catalog/archive/refs/heads/main.zip -O /opt/main.zip && \
    unzip /opt/main.zip -d /opt  && \
    rm /opt/main.zip && \
    mkdir /usr/local/tomcat/webapps/catalog && \
    mkdir ${msc_data}/microservices-collection && \
    cp /opt/service-catalog-main/PUBLIC/modapto_service_catalogue.html /usr/local/tomcat/webapps/catalog/index.html && \
    cp -a /opt/service-catalog-main/MICROSERVICES/. ${msc_data}/microservices-collection/ && \

    apt-get -qy purge maven && \
    apt-get -qy autoremove && \
    rm -r /root/.m2/

EXPOSE 8080
CMD ["bash", "-c", "/opt/initialize.sh && catalina.sh run"]