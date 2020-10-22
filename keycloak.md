# Keycloak

## What is Keycloak

Keycloak is an open-source Identity-and-Access-Management (IAM) tool for modern applications and services. Keycloak adds authentication to applications and secure services with minimum fuss. No need to deal with storing users or authenticating users. It's all available out of the box.

Keycloak has advanced features such as User Federation, Identity Brokering and Social Login.

## Keycloak on Docker

- Make sure Docker is installed

- Run

  ``` Powershell
  docker run -p 8080:8080 -e KEYCLOAK_USER=admin -e KEYCLOAK_PASSWORD=admin -d -t quay.io/keycloak/keycloak
  ```

- Login at http://localhost:8080/auth/admin using `admin` as the username and password

## Keycloak on Kubernetes

## Keycloak Operator for Kubernetes
