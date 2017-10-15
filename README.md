# search-service-consumer
This application consumes search service exposed by 'search-service-broker' on cloud foundry.

Please refer to:
https://github.com/cbagade/search-service-broker  for broker application.

Once the service is exposed on cloud foundry platform, other applications can bind it. 
Please refer cloud foundry documentation for details regarding exposing service and binding.

com.cloud.search.serviceconsumer.resource.Consumer got the functionality to parse and use the service. 

Creation of service and other related functionality is available inside 'search-service-broker' code base, link mentioned above.
