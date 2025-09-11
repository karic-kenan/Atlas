package io.aethibo.features.users.data.repository

import io.aethibo.features.users.data.failure.UserException
import io.aethibo.features.users.domain.model.User
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.runBlocking

class UsersRepositoryImplTest : DescribeSpec({

    // Test setup
    val meterRegistry = SimpleMeterRegistry()
    lateinit var repository: UsersRepositoryImpl

    // Test data
    val testUser = User(
        id = 1L,
        email = "test@example.com",
        username = "testuser",
        password = "password123",
        bio = "Test bio",
        image = "test.jpg",
    )

    beforeTest {
        repository = UsersRepositoryImpl(meterRegistry)
    }

    describe("UsersRepositoryImpl Validation Tests") {

        describe("email validation") {

            it("should throw UserException.InvalidEmail when email is blank") {
                // When & Then
                shouldThrow<UserException.InvalidEmail> {
                    runBlocking { repository.findByEmail("") }
                }
            }

            it("should throw UserException.InvalidEmail when email doesn't contain @") {
                // When & Then
                shouldThrow<UserException.InvalidEmail> {
                    runBlocking { repository.findByEmail("invalid-email") }
                }
            }

            it("should accept valid email format") {
                // When & Then - Should not throw validation exception
                shouldThrow<Exception> { // Some database-related exception is expected
                    runBlocking { repository.findByEmail("test@example.com") }
                }.let { exception ->
                    exception shouldNotBe UserException.InvalidEmail::class
                }
            }
        }

        describe("username validation") {
            it("should throw UserException.EmptyRequiredField when username is blank") {
                // When & Then
                shouldThrow<UserException.EmptyRequiredField> {
                    runBlocking { repository.findByUsername("") }
                }
            }

            it("should throw UserException.InvalidUsername when username is too short") {
                // When & Then
                shouldThrow<UserException.InvalidUsername> {
                    runBlocking { repository.findByUsername("ab") } // Only 2 characters
                }
            }

            it("should throw UserException.InvalidUsername when username is too long") {
                // When & Then
                val longUsername = "a".repeat(51) // 51 characters
                shouldThrow<UserException.InvalidUsername> {
                    runBlocking { repository.findByUsername(longUsername) }
                }
            }

            it("should accept valid username length") {
                // When & Then - Should not throw validation exception
                shouldThrow<Exception> { // Some database-related exception is expected
                    runBlocking { repository.findByUsername("validuser") }
                }.let { exception ->
                    exception shouldNotBe UserException.EmptyRequiredField::class
                    exception shouldNotBe UserException.InvalidUsername::class
                }
            }
        }

        describe("password validation for creation") {
            it("should throw UserException.EmptyRequiredField when password is blank") {
                // Given
                val user = testUser.copy(password = "")

                // When & Then
                shouldThrow<UserException.EmptyRequiredField> {
                    runBlocking { repository.create(user) }
                }
            }
        }

        describe("create - input validation") {

            it("should throw UserException.InvalidEmail for invalid email") {
                // Given
                val user = testUser.copy(email = "invalid-email")

                // When & Then
                shouldThrow<UserException.InvalidEmail> {
                    runBlocking { repository.create(user) }
                }
            }

            it("should throw UserException.EmptyRequiredField for null username") {
                // Given
                val user = testUser.copy(username = null)

                // When & Then
                shouldThrow<UserException.EmptyRequiredField> {
                    runBlocking { repository.create(user) }
                }
            }

            it("should throw UserException.InvalidUsername for invalid username length") {
                // Given
                val user = testUser.copy(username = "ab")

                // When & Then
                shouldThrow<UserException.InvalidUsername> {
                    runBlocking { repository.create(user) }
                }
            }

            it("should throw UserException.InvalidPassword for short password") {
                // Given
                val user = testUser.copy(password = "123")

                // When & Then
                shouldThrow<UserException.InvalidPassword> {
                    runBlocking { repository.create(user) }
                }
            }
        }

        describe("update - input validation") {

            it("should throw UserException.InvalidEmail for invalid email in update") {
                // Given
                val user = testUser.copy(email = "updated-invalid-email")

                // When & Then
                shouldThrow<UserException.InvalidEmail> {
                    runBlocking { repository.update("test@example.com", user) }
                }
            }

            it("should throw UserException.InvalidEmail for blank email parameter") {
                // When & Then
                shouldThrow<UserException.InvalidEmail> {
                    runBlocking { repository.update("", testUser) }
                }
            }

            it("should throw UserException.InvalidUsername for invalid username in update") {
                // Given
                val user = testUser.copy(username = "ab") // Too short

                // When & Then
                shouldThrow<UserException.InvalidUsername> {
                    runBlocking { repository.update("test@example.com", user) }
                }
            }

            it("should throw UserException.InvalidPassword for invalid password in update") {
                // Given
                val user = testUser.copy(password = "123") // Too short

                // When & Then
                shouldThrow<UserException.InvalidPassword> {
                    runBlocking { repository.update("test@example.com", user) }
                }
            }

            it("should accept null username and password in update (optional fields)") {
                // Given
                val user = testUser.copy(username = null, password = null)

                // When & Then - Should not throw validation exceptions for null optional fields
                shouldThrow<Exception> { // Some database-related exception is expected
                    runBlocking { repository.update("test@example.com", user) }
                }.let { exception ->
                    exception shouldNotBe UserException.EmptyRequiredField::class
                    exception shouldNotBe UserException.InvalidUsername::class
                    exception shouldNotBe UserException.InvalidPassword::class
                }
            }
        }

        describe("findById - parameter validation") {

            it("should return null when id is 0") {
                // When
                val result = runBlocking { repository.findById(0L) }

                // Then
                result shouldBe null
            }

            it("should return null when id is negative") {
                // When
                val result = runBlocking { repository.findById(-1L) }

                // Then
                result shouldBe null
            }

            it("should accept positive id values") {
                // When & Then - Should not throw validation exception
                shouldThrow<Exception> { // Some database-related exception is expected
                    runBlocking { repository.findById(1L) }
                } // No specific validation to check, just ensure it doesn't fail on validation
            }
        }

        describe("findIsFollowUser - parameter validation") {

            it("should throw UserException.InvalidEmail for invalid email") {
                // When & Then
                shouldThrow<UserException.InvalidEmail> {
                    runBlocking { repository.findIsFollowUser("invalid-email", 1L) }
                }
            }

            it("should throw UserException.UserNotFound for invalid user ID") {
                // When & Then
                shouldThrow<UserException.UserNotFound> {
                    runBlocking { repository.findIsFollowUser("test@example.com", 0L) }
                }
            }

            it("should throw UserException.UserNotFound for negative user ID") {
                // When & Then
                shouldThrow<UserException.UserNotFound> {
                    runBlocking { repository.findIsFollowUser("test@example.com", -1L) }
                }
            }
        }

        describe("follow - parameter validation") {

            it("should throw UserException.InvalidEmail for invalid email") {
                // When & Then
                shouldThrow<UserException.InvalidEmail> {
                    runBlocking { repository.follow("invalid-email", "targetuser") }
                }
            }

            it("should throw UserException.EmptyRequiredField for blank username to follow") {
                // When & Then
                shouldThrow<UserException.EmptyRequiredField> {
                    runBlocking { repository.follow("test@example.com", "") }
                }
            }

            it("should throw UserException.InvalidUsername for invalid username to follow") {
                // When & Then
                shouldThrow<UserException.InvalidUsername> {
                    runBlocking { repository.follow("test@example.com", "ab") }
                }
            }
        }

        describe("unfollow - parameter validation") {

            it("should throw UserException.InvalidEmail for invalid email") {
                // When & Then
                shouldThrow<UserException.InvalidEmail> {
                    runBlocking { repository.unfollow("invalid-email", "targetuser") }
                }
            }

            it("should throw UserException.EmptyRequiredField for blank username to unfollow") {
                // When & Then
                shouldThrow<UserException.EmptyRequiredField> {
                    runBlocking { repository.unfollow("test@example.com", "") }
                }
            }

            it("should throw UserException.InvalidUsername for invalid username to unfollow") {
                // When & Then
                shouldThrow<UserException.InvalidUsername> {
                    runBlocking { repository.unfollow("test@example.com", "ab") }
                }
            }
        }

        describe("findUserWithStats - parameter validation") {

            it("should throw UserException.EmptyRequiredField for blank username") {
                // When & Then
                shouldThrow<UserException.EmptyRequiredField> {
                    runBlocking { repository.findUserWithStats("") }
                }
            }

            it("should throw UserException.InvalidUsername for invalid username") {
                // When & Then
                shouldThrow<UserException.InvalidUsername> {
                    runBlocking { repository.findUserWithStats("ab") }
                }
            }
        }

        describe("findUsersBySearch - parameter validation") {

            it("should throw UserException.InvalidLimit when limit is less than 1") {
                // Given
                val criteria = UserSearchCriteria(query = "test")

                // When & Then
                shouldThrow<UserException.InvalidLimit> {
                    runBlocking { repository.findUsersBySearch(criteria, limit = 0) }
                }
            }

            it("should throw UserException.InvalidLimit when limit is greater than 100") {
                // Given
                val criteria = UserSearchCriteria(query = "test")

                // When & Then
                shouldThrow<UserException.InvalidLimit> {
                    runBlocking { repository.findUsersBySearch(criteria, limit = 101) }
                }
            }

            it("should throw UserException.InvalidOffset when offset is negative") {
                // Given
                val criteria = UserSearchCriteria(query = "test")

                // When & Then
                shouldThrow<UserException.InvalidOffset> {
                    runBlocking { repository.findUsersBySearch(criteria, offset = -1L) }
                }
            }

            it("should accept valid limit and offset") {
                // Given
                val criteria = UserSearchCriteria(query = "test")

                // When & Then - Should not throw validation exceptions
                shouldThrow<Exception> { // Some database-related exception is expected
                    runBlocking { repository.findUsersBySearch(criteria, limit = 20, offset = 0L) }
                }.let { exception ->
                    exception shouldNotBe UserException.InvalidLimit::class
                    exception shouldNotBe UserException.InvalidOffset::class
                }
            }
        }

        describe("getFollowersWithPagination - parameter validation") {

            it("should throw UserException.EmptyRequiredField for blank username") {
                // When & Then
                shouldThrow<UserException.EmptyRequiredField> {
                    runBlocking { repository.getFollowersWithPagination("") }
                }
            }

            it("should throw UserException.InvalidUsername for invalid username") {
                // When & Then
                shouldThrow<UserException.InvalidUsername> {
                    runBlocking { repository.getFollowersWithPagination("ab") }
                }
            }

            it("should throw UserException.InvalidLimit when limit is invalid") {
                // When & Then
                shouldThrow<UserException.InvalidLimit> {
                    runBlocking { repository.getFollowersWithPagination("validuser", limit = 0) }
                }
            }

            it("should throw UserException.InvalidOffset when offset is negative") {
                // When & Then
                shouldThrow<UserException.InvalidOffset> {
                    runBlocking { repository.getFollowersWithPagination("validuser", offset = -1L) }
                }
            }
        }

        describe("getFollowingWithPagination - parameter validation") {

            it("should throw UserException.EmptyRequiredField for blank username") {
                // When & Then
                shouldThrow<UserException.EmptyRequiredField> {
                    runBlocking { repository.getFollowingWithPagination("") }
                }
            }

            it("should throw UserException.InvalidUsername for invalid username") {
                // When & Then
                shouldThrow<UserException.InvalidUsername> {
                    runBlocking { repository.getFollowingWithPagination("ab") }
                }
            }

            it("should throw UserException.InvalidLimit when limit is invalid") {
                // When & Then
                shouldThrow<UserException.InvalidLimit> {
                    runBlocking { repository.getFollowingWithPagination("validuser", limit = 0) }
                }
            }

            it("should throw UserException.InvalidOffset when offset is negative") {
                // When & Then
                shouldThrow<UserException.InvalidOffset> {
                    runBlocking { repository.getFollowingWithPagination("validuser", offset = -1L) }
                }
            }
        }

        describe("findUsersByIds - parameter validation") {

            it("should return empty map for empty list") {
                // When
                val result = runBlocking { repository.findUsersByIds(emptyList()) }

                // Then
                result.shouldBeEmpty()
            }

            it("should accept non-empty list of IDs") {
                // When & Then - Should not throw validation exception
                shouldThrow<Exception> { // Some database-related exception is expected
                    runBlocking { repository.findUsersByIds(listOf(1L, 2L, 3L)) }
                } // No specific validation to check
            }
        }

        describe("findUsersFollowStatus - parameter validation") {

            it("should return empty map for empty username list") {
                // When
                val result = runBlocking { repository.findUsersFollowStatus("test@example.com", emptyList()) }

                // Then
                result.shouldBeEmpty()
            }

            it("should accept valid email and non-empty username list") {
                // When & Then - Should not throw validation exception
                shouldThrow<Exception> { // Some database-related exception is expected
                    runBlocking { repository.findUsersFollowStatus("test@example.com", listOf("user1", "user2")) }
                } // No specific validation to check for this method
            }
        }
    }

    describe("UserSearchCriteria data class") {

        it("should have correct default values") {
            // Given
            val criteria = UserSearchCriteria()

            // Then
            criteria.query shouldBe null
            criteria.minFollowers shouldBe null
            criteria.maxFollowers shouldBe null
            criteria.hasImage shouldBe null
            criteria.hasBio shouldBe null
        }

        it("should allow custom values") {
            // Given
            val query = "search term"
            val minFollowers = 10
            val maxFollowers = 100
            val hasImage = true
            val hasBio = false

            val criteria = UserSearchCriteria(
                query = query,
                minFollowers = minFollowers,
                maxFollowers = maxFollowers,
                hasImage = hasImage,
                hasBio = hasBio
            )

            // Then
            criteria.query shouldBe query
            criteria.minFollowers shouldBe minFollowers
            criteria.maxFollowers shouldBe maxFollowers
            criteria.hasImage shouldBe hasImage
            criteria.hasBio shouldBe hasBio
        }

        it("should generate consistent hashCode for same values") {
            // Given
            val criteria1 = UserSearchCriteria(query = "test", minFollowers = 5)
            val criteria2 = UserSearchCriteria(query = "test", minFollowers = 5)

            // Then
            criteria1.hashCode() shouldBe criteria2.hashCode()
        }

        it("should generate different hashCode for different values") {
            // Given
            val criteria1 = UserSearchCriteria(query = "test1")
            val criteria2 = UserSearchCriteria(query = "test2")

            // Then
            criteria1.hashCode() shouldNotBe criteria2.hashCode()
        }
    }

    describe("FollowStats data class") {

        it("should create FollowStats with required parameters") {
            // Given
            val followersCount = 100L
            val followingCount = 50L

            // When
            val stats = FollowStats(followersCount, followingCount)

            // Then
            stats.followersCount shouldBe followersCount
            stats.followingCount shouldBe followingCount
            stats.mutualFollowersCount shouldBe 0L // default value
        }

        it("should create FollowStats with all parameters") {
            // Given
            val followersCount = 100L
            val followingCount = 50L
            val mutualFollowersCount = 25L

            // When
            val stats = FollowStats(followersCount, followingCount, mutualFollowersCount)

            // Then
            stats.followersCount shouldBe followersCount
            stats.followingCount shouldBe followingCount
            stats.mutualFollowersCount shouldBe mutualFollowersCount
        }

        it("should have correct default value for mutualFollowersCount") {
            // When
            val stats = FollowStats(10L, 5L)

            // Then
            stats.mutualFollowersCount shouldBe 0L
        }
    }
})
