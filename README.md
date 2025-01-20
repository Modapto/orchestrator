# MODAPTO Orchestrator

This project contain the source code of the MODAPTO Orchestrator and the deployment Docker of both Orchestrator and Service Catalog.

Instructions to build and start the docker container:

1) Build the image
<pre>
sudo docker build --no-cache -t olive-msc .
</pre>

2) Run the container
<pre>
mkdir ./msc-data
sudo docker run -d -p 8080:8080 -v ${PWD}/msc-data/:/opt/msc-data/ --name olive-msc --restart always olive-msc
</pre>

3) Access the Orchestrator at http://127.0.0.1:8080/micro-service-controller-rest/

4) Access the Service Catalog at http://127.0.0.1:8080/catalog/

### Useful commands
- Stop the container
<pre>
sudo docker stop olive-msc
</pre>

- Remove the container
<pre>
sudo docker rm olive-msc
sudo docker ps -a
</pre>

- Remove the image
<pre>
sudo docker rmi olive-msc
sudo docker images -a
</pre>

- Remove any stopped container and unreferenced image
<pre>
sudo docker system prune -a
sudo docker image prune -a
</pre>

- Run the container shell for problem analysis
<pre>
sudo docker run -it -p 8080:8080 -v ${PWD}/msc-data/:/opt/msc-data/ --name olive-msc --rm olive-msc bash
/opt/initialize.sh
catalina.sh run&

nano /opt/msc-data/msc.log
exit
</pre>

### Notes
- If docker is running with parameter -v ${PWD}/msc-data/:/opt/msc-data/ the folder ${PWD}/msc-data/ must have write permission in the host (eg. if docker is running on Window OS and the folder is under C:\Program Files\ the docker run command will fail).
- If docker is running with parameter -p 8080:8080 the port 8080 of the host must be available