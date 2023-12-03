# Atlas
 
> ### Atlas codebase demonstrates practical usage of real world example that abides to [RealWorld](https://github.com/gothinkster/realworld) spcifications and API

## Getting started
1. Clone the repository
2. Download and import [postman collection](https://github.com/gothinkster/realworld/blob/main/api/Conduit.postman_collection.json) for testing
3. Run the project

The server is configured to start on [7003](http://localhost:7003/api/v1/) with `api/v1/` context, but you can change it in [application.conf](https://github.com/primepixel/Atlas/blob/main/src/main/resources/application.conf) file.

## Built With :hammer:
  - [Kotlin](https://github.com/JetBrains/kotlin) as programming language
  - [Ktor](https://github.com/ktorio/ktor) as web framework
  - [Koin](https://github.com/InsertKoinIO/koin) as dependency injection framework
  - [KotlinX JSON](https://ktor.io/docs/serialization-client.html#add_json_dependency) as data bind serialization/deserialization
  - [Java-jwt](https://github.com/auth0/java-jwt) for JWT spec implementation
  - [HikariCP](https://github.com/brettwooldridge/HikariCP) as datasource to abstract driver implementation
  - [H2](https://github.com/h2database/h2database) as database
  - [Exposed](https://github.com/JetBrains/Exposed) as Sql framework to persistence layer
  - [slugify](https://github.com/slugify/slugify)
  - [Swagger](https://github.com/SMILEY4/ktor-swagger-ui) for API preview

## Structure :nut_and_bolt:
    + core/
      All application setup, exceptions, extensions, etc
      + config/
        Contains main module with neccessary server setup and plugins definitions
      + di/
      + exceptions/
      + extensions/
      + navigation/
      + security/
      
    + features/
      + articles/
        + data/
          Persistence layer and tables defintion
        + domain/
          Bridge between layers, holding DTOs and contracts
        + presentation/
          Classes and methods to map actions to routes
      + comments/...
      + profiles/...
      + tags/...
      + users/...
    - Application.kt <- Entry point

## Contribute
If you want to contribute to this project, you're always welcome!

## Contact
If you need any help, feel free to contact me: kenan.karic@outlook.com.

## License
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
