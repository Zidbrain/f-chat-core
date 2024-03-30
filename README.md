F-Chat backend service.

To launch the service create secrets.json in src/main/resources with the following contents:
```json
{
  "jwtSecret": "<JWT Secret>",
  "googleAppId": "<Google application id from google cloud console>"
}
```
Then run:
```shell
./gradlew jibDockerBuild # This create a docker image for the service
docker-compose up -d
```