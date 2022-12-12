package com.group.libraryapp.service.user

import com.group.libraryapp.domain.user.User
import com.group.libraryapp.domain.user.UserRepository
import com.group.libraryapp.domain.user.loanhistory.UserLoanHistory
import com.group.libraryapp.domain.user.loanhistory.UserLoanHistoryRepository
import com.group.libraryapp.domain.user.loanhistory.UserLoanStatus
import com.group.libraryapp.dto.user.request.UserCreateRequest
import com.group.libraryapp.dto.user.request.UserUpdateRequest
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.*

@SpringBootTest
class UserServiceTest @Autowired constructor(
    private val userRepository: UserRepository,
    private val userService: UserService,
    private val userLoanHistoryRepository: UserLoanHistoryRepository,
) {

    @AfterEach
    fun clean() {
        userRepository.deleteAll()
    }

    @Test
    @DisplayName("유저 저장이 정상 동작한다")
    fun saveUserTest() {
        // given
        val request = UserCreateRequest("유춘상", 34)

        // when
        userService.saveUser(request)

        // then
        val users = userRepository.findAll()
        assertThat(users).hasSize(1)
        assertThat(users[0].name).isEqualTo("유춘상")
        assertThat(users[0].age).isEqualTo(34)
    }

    @Test
    @DisplayName("유저 조회가 정상 동작한다")
    fun getUsersTest() {
        // given
        userRepository.saveAll(
            listOf(
                User("A", 20),
                User("B", null),
            )
        )

        // when
        val users = userService.getUsers()

        // then
        assertThat(users).hasSize(2)
        assertThat(users).extracting("name").containsExactlyInAnyOrder("A", "B") // ["A", "B"]
        assertThat(users).extracting("age").containsExactlyInAnyOrder(20, null) // [20, null]
    }

    @Test
    @DisplayName("유저명 변경이 정상 동작한다")
    fun updateUserNameTest() {
        // given
        val savedUser = userRepository.save(User("A", null, Collections.emptyList(), null))
        val request = UserUpdateRequest(savedUser.id!!, "B")

        // when
        userService.updateUserName(request)

        // then
        val user = userRepository.findAll()[0]
        assertThat(user.name).isEqualTo("B")
    }

    @Test
    @DisplayName("유저 삭제가 정상 동작한다")
    fun deleteUserTest() {
        // given
        userRepository.save(User("A", null, mutableListOf()))

        // when
        userService.deleteUser("A")

        // then
        assertThat(userRepository.findAll()).isEmpty()
    }

    @Test
    @DisplayName("대여 기록이 없는 유저도 응답에 포함된다")
    fun getUserLoanHistoriesTest1() {
        // given
        userRepository.save(User("A", null))

        // when
        val results = userService.getUserLoanHistories()

        //then
        assertThat(results).hasSize(1)
        assertThat(results[0].name).isEqualTo("A")
        assertThat(results[0].books).isEmpty()
    }

    @Test
    @DisplayName("대여 기록이 많은 유저의 응답이 정상 동작한다")
    fun getUserLoanHistoriesTest2() {
        // given
        val user = userRepository.save(User("A", null))
        userLoanHistoryRepository.saveAll(
            listOf(
                UserLoanHistory.fixture(user, "책1", UserLoanStatus.LOANED),
                UserLoanHistory.fixture(user, "책2", UserLoanStatus.LOANED),
                UserLoanHistory.fixture(user, "책3", UserLoanStatus.RETURNED),
            )
        )

        // when
        val results = userService.getUserLoanHistories()

        //then
        assertThat(results).hasSize(1)
        assertThat(results[0].name).isEqualTo("A")
        assertThat(results[0].books).hasSize(3)
        assertThat(results[0].books).extracting("name")
            .containsExactlyInAnyOrder("책1", "책2", "책3")
        assertThat(results[0].books).extracting("isReturn")
            .containsExactlyInAnyOrder(false, false, true)
    }

    @Test
    @DisplayName("대여 기록을 조회하는 통합 테스트를 성공한다")
    fun getUserLoanHistoriesTest3() {
        // given
        val users = userRepository.saveAll(
            listOf(
                User("A", null),
                User("B", null),
            )
        )

        userLoanHistoryRepository.saveAll(
            listOf(
                UserLoanHistory.fixture(users[0], "책1", UserLoanStatus.LOANED),
                UserLoanHistory.fixture(users[0], "책2", UserLoanStatus.LOANED),
                UserLoanHistory.fixture(users[0], "책3", UserLoanStatus.RETURNED),
            )
        )

        // when
        val results = userService.getUserLoanHistories()

        //then
        assertThat(results).hasSize(2)

        val userAResult = results.first { result -> result.name == "A" }
        assertThat(userAResult.name).isEqualTo("A")
        assertThat(userAResult.books).hasSize(3)
        assertThat(userAResult.books).extracting("name")
            .containsExactlyInAnyOrder("책1", "책2", "책3")
        assertThat(userAResult.books).extracting("isReturn")
            .containsExactlyInAnyOrder(false, false, true)

        val userBResult = results.first { result -> result.name == "B" }
        assertThat(userBResult.name).isEqualTo("B")
        assertThat(userBResult.books).isEmpty()
    }
}