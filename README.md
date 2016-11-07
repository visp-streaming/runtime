# What is VISP?
VISP represents a set of different prototypes, which are designed to create an ecosystem for elastic stream processing applications, which consist of individual building blocks to ease the creation of new applications. Therefore, these prototypes cover all relevant lifecycle phases, ranging from the inventarization of the building blocks and the design of the application, over the deployment up to the usage monitoring and provisioning of computational resources to ensure a high level of quality of service.

# Publications
```
VISP: An Ecosystem for Elastic Data Stream Processing for the Internet of Things
Christoph Hochreiner, Michael Vögler, Philipp Waibel and Schahram Dustdar
20th Int. Enterprise Distributed Object Computing Conf. (EDOC), 2016
```
[Paper](http://www.infosys.tuwien.ac.at/staff/hochreiner/publications/EDOC2016.pdf) |
[Presentation](https://speakerdeck.com/chochreiner/visp-an-ecosystem-for-elastic-data-stream-processing-for-the-internet-of-things) | [Bibtex](http://www.infosys.tuwien.ac.at/staff/hochreiner/publications/bibtex/hochreiner2016edoc.bib) 

```
Elastic Stream Processing for the Internet of Things
Christoph Hochreiner, Michael Vögler, Stefan Schulte and Schahram Dustdar
9th Int. Conf. on Cloud Computing (CLOUD), 2016
```
[Paper](http://www.infosys.tuwien.ac.at/staff/hochreiner/publications/IEEECloud2016.pdf) | 
[Presentation](https://speakerdeck.com/chochreiner/elastic-stream-processing-for-the-internet-of-things) | [Bibtex](http://www.infosys.tuwien.ac.at/staff/hochreiner/publications/bibtex/hochreiner2016cloud.bib) 

```
Elastic Stream Processing for Distributed Environments
Christoph Hochreiner, Stefan Schulte, Schahram Dustdar and Freddy Lecue
IEEE Internet Computing, Volume 19, Number 6, pages 54-59, 2015
```
[Paper](http://ieeexplore.ieee.org/xpl/articleDetails.jsp?arnumber=7307896) | [Bibtex](http://www.infosys.tuwien.ac.at/staff/hochreiner/publications/bibtex/hochreiner2015ic.bib) 


# Setup Requirements

## Message Infrastructure
Setup a RabbitMQ instance and set the URL from the RabbitMQ instance to *visp.infrastructurehost*  *spring.rabbitmq.host* in application.properties.
Further create a user with the name *visp* and the password *visp*.

## Shared Storage
Setup a Redis instance on the same host as the Message Infrastructure.

## Data Backend
Setup a Mysql database with username: *root*, password: *password* and a database: *visp* on the same machine as the VISP runtime

## Configure the Dockerhost image
Set the id of an CoreOS image on an Openstack instance for the parameter *visp.dockerhost.image* in application.properties

## Access Configuration
Create a copy of the *credential.sample.properties* and store it as *credential.properties* and add the access credentials to this file.

# Start VISP runtime

The application can be started with:

```
mvn spring-boot:run

```

At startup, the VISP runtime creates the topology based on the configured topology (*visp.topology* in *application.propertie*s), starts an initial Dockerhost for the services and created the initial processing configuration (one instance for each service).

# Configuring the Runtime

## Topology selection
The topology can be choosen based on the value of *visp.topology*  in *application.properties*.

## BTU configuration of Hosts
The billing time unit of the Hosts can be configured based on the value of *visp.btu* in *application.properties*.

## Graceperiod for shutdown
The graceperiod for shutting down processing services can be set based on the value of *visp.shutdown.graceperiod* in *application.properties*.

