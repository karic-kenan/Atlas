# Atlas

> Atlas is a Kotlin + Ktor backend demonstrating a RealWorld-compliant API with practical usage examples.

## Getting Started

1. Clone the repository
2. Download and import the [Postman collection](https://github.com/gothinkster/realworld/blob/main/api/Conduit.postman_collection.json) for testing
3. Run the project

The server starts on [http://localhost:7003/api/](http://localhost:7003/api/) by default. You can change it in [application.yaml](https://github.com/primepixel/Atlas/blob/main/src/main/resources/application.yaml).

## Built With

* [Kotlin](https://github.com/JetBrains/kotlin) – Programming language
* [Ktor](https://github.com/ktorio/ktor) – Web framework
* [Koin](https://github.com/InsertKoinIO/koin) – Dependency injection
* [KotlinX JSON](https://ktor.io/docs/serialization-client.html#add_json_dependency) – Serialization/Deserialization
* [Java-jwt](https://github.com/auth0/java-jwt) – JWT authentication
* [HikariCP](https://github.com/brettwooldridge/HikariCP) – Database connection pool
* [H2 Database](https://github.com/h2database/h2database) – Embedded database
* [Exposed](https://github.com/JetBrains/Exposed) – SQL framework for Kotlin
* [Slugify](https://github.com/slugify/slugify) – Slug generation
* [Swagger UI](https://github.com/SMILEY4/ktor-openapi-tools) – API documentation UI

## API Routes Overview

### Users

* **POST `/api/users`** – Register a new user
* **POST `/api/users/login`** – Authenticate and get access tokens
* **GET `/api/user`** – Get the current authenticated user
* **PUT `/api/user`** – Update current user profile

### Profiles

* **GET `/api/profiles/celeb_{USERNAME}`** – Get a user's profile (public info + follow status)
* **POST `/api/profiles/celeb_{USERNAME}/follow`** – Follow a user
* **DELETE `/api/profiles/celeb_{USERNAME}/follow`** – Unfollow a user

### Articles

* **POST `/api/articles`** – Create an article
* **GET `/api/articles/{slug}`** – Get a single article by slug
* **PUT `/api/articles/{slug}`** – Update an article
* **DELETE `/api/articles/{slug}`** – Delete an article
* **GET `/api/articles`** – List articles with optional filters (author, tag, favorited)

### Comments

* **POST `/api/articles/{slug}/comments`** – Add a comment to an article
* **DELETE `/api/articles/{slug}/comments/{commentId}`** – Delete a comment
* **GET `/api/articles/{slug}/comments`** – List comments for an article

### Tags

* **GET `/api/tags`** – Get all available tags

## Project Structure

```
+ core/
  + config/       # Main module, server setup, plugins
  + di/           # Dependency injection
  + exceptions/   # Custom exceptions
  + extensions/   # Extensions and helpers
  + navigation/   # Route navigation
  + security/     # JWT & auth setup

+ features/
  + articles/     # Articles feature
    + data/       # Persistence & tables
    + domain/     # DTOs, contracts
    + presentation/ # Routes
  + comments/...
  + profiles/...
  + tags/...
  + users/...
- Application.kt  # Entry point
```

## Contributing

Contributions are welcome! Feel free to submit pull requests or open issues.

## Contact

Kenan Karic – [kenan.karic@outlook.com](mailto:kenan.karic@outlook.com)

## License

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
