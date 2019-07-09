# Rflux API

This project is a POC on implementing oauth2-secured API with microservice architecture.
It uses Keycloak as authorization server.

Some other concepts explored in the project:

* Spring Functional Routing with Kotlin DSL
* Spring Cloud Gateway with Kotlin
* gradle builds with Kotlin build files for multi-module Spring Boot project with shared project libraries

## gateway-service

For information about Spring Cloud Gateway, refer to:

https://cloud.spring.io/spring-cloud-gateway/single/spring-cloud-gateway.html

## rocket-service

REST API, contains CRUD for "rockets" and protected API for launching them.

## rflux-keycloak

Library containing reusable utils for better KeyCloak OpenID integration with Spring security

## Security

To use keycloak locally, you have to:

1. `docker-compose up -d keycloak` 
1. login into keycloak: [http://localhost:8000](http://localhost:8000)
1. create new realm: `rflux`
1. create new client `rflux-client` and set the following properties:
   * Access Type to `public`
   * Direct Access Grants Enabled to `ON`
   * Valid Redirect URLs 
      * `http://localhost:8080/*`
1. Create new user, set password
1. To enable write access to `/rocket-service/launch` API, add role rockets_launch

Oauth2 Tokens will be forwarded to the serviced behind the gateway so that they can act as OAuth 2 Resource Servers with fine-grained access control

The gateway itself acts a Resource Server - it will accept requests with header `Authorization: Bearer ${TOKEN}`

To obtain Auth token and Refresh token, you can do (replace params to match your config):

```bash
curl -d client_id=rflux-client \
 -d username=${USERNAME} \
 -d password=${PASSWORD} \
 -d grant_type=password \
 -X POST http://localhost:8000/auth/realms/rflux/protocol/openid-connect/token
```

To refresh Auth token:

```bash
curl -d client_id=rflux-client \
 -d refresh_token=${REFRESH_TOKEN} \
 -d grant_type=refresh_token \
 -X POST http://localhost:8000/auth/realms/rflux/protocol/openid-connect/token
```

To logout:

```bash
curl -d client_id=rflux-client \
 -d refresh_token=${REFRESH_TOKEN} \
 -H "Authorization: Bearer ${AUTH_TOKEN}"\
 -X POST http://localhost:8000/auth/realms/rflux/protocol/openid-connect/logout
```
