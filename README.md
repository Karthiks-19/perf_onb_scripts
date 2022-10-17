Refer for gradle plugin doc:

https://gatling.io/docs/current/extensions/gradle_plugin/ 

- To run the simulation use command
``` ./gradlew clean gatlingRun -DHOST=<<HOSTURL>>```

- To run the simulation with number of users and duration use command line

``` ./gradlew clean gatlingRun -DHOST=<<HOSTURL>> -DUsers=1 -DDuration=10 ```