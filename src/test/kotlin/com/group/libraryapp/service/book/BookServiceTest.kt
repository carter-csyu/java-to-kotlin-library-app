package com.group.libraryapp.service.book

import com.group.libraryapp.domain.book.Book
import com.group.libraryapp.domain.book.BookRepository
import com.group.libraryapp.domain.book.BookType
import com.group.libraryapp.domain.user.User
import com.group.libraryapp.domain.user.UserRepository
import com.group.libraryapp.domain.user.loanhistory.UserLoanHistory
import com.group.libraryapp.domain.user.loanhistory.UserLoanHistoryRepository
import com.group.libraryapp.domain.user.loanhistory.UserLoanStatus
import com.group.libraryapp.dto.book.request.BookLoanRequest
import com.group.libraryapp.dto.book.request.BookRequest
import com.group.libraryapp.dto.book.request.BookReturnRequest
import com.group.libraryapp.dto.book.response.BookStatResponse
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class BookServiceTest @Autowired constructor(
    private val bookRepository: BookRepository,
    private val bookService: BookService,
    private val userLoanHistoryRepository: UserLoanHistoryRepository,
    private val userRepository: UserRepository,
) {

    @AfterEach
    fun clean() {
        bookRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    @DisplayName("책 등록이 정상 동작한다")
    fun saveBookTest() {
        // given
        val request = BookRequest("클린코드", BookType.COMPUTER)

        // when
        bookService.saveBook(request)

        // then
        val books = bookRepository.findAll()
        assertThat(books).hasSize(1)
        assertThat(books[0].name).isEqualTo("클린코드")
        assertThat(books[0].type).isEqualTo(BookType.COMPUTER)
    }

    @Test
    @DisplayName("책 대여가 정상 동작한다")
    fun loanBookTest() {
        // given
        bookRepository.save(Book.fixture("클린코드"))
        val user = userRepository.save(User("유춘상", 34))
        val request = BookLoanRequest("유춘상", "클린코드")

        // when
        bookService.loanBook(request)

        // then
        val histories = userLoanHistoryRepository.findAll()
        assertThat(histories).hasSize(1)
        assertThat(histories[0].bookName).isEqualTo("클린코드")
        assertThat(histories[0].user.id).isEqualTo(user.id)
        assertThat(histories[0].status).isEqualTo(UserLoanStatus.LOANED)
    }

    @Test
    @DisplayName("책이 이미 대여되어 있다면, 대여가 실패한다")
    fun loanBookFailureTest() {
        // given
        val user = userRepository.save(User("유춘상", 34))
        bookRepository.save(Book.fixture("클린코드"))
        userLoanHistoryRepository.save(UserLoanHistory.fixture(user, "클린코드"))

        val request = BookLoanRequest("유춘상", "클린코드")

        // expect
        val message = assertThrows<IllegalArgumentException> {
            bookService.loanBook(request)
        }.message
        assertThat(message).isEqualTo("진작 대출되어 있는 책입니다")
    }

    @Test
    @DisplayName("책 반납이 정상 동작한다")
    fun returnBookTest() {
        // given
        val user = userRepository.save(User("유춘상", 34))
        bookRepository.save(Book.fixture("클린코드"))
        userLoanHistoryRepository.save(UserLoanHistory.fixture(user, "클린코드"))

        val request = BookReturnRequest("유춘상", "클린코드")

        // when
        bookService.returnBook(request)

        // then
        val histories = userLoanHistoryRepository.findAll()
        assertThat(histories).hasSize(1)
        assertThat(histories[0].status).isEqualTo(UserLoanStatus.RETURNED)

    }

    @Test
    @DisplayName("책 대여 권 수를 정상 확인한다")
    fun countLoanedBookTest() {
        // given
        val user = userRepository.save(User("유춘상", null))
        userLoanHistoryRepository.saveAll(
            listOf(
                UserLoanHistory.fixture(user, "A"),
                UserLoanHistory.fixture(user, "B", UserLoanStatus.RETURNED),
                UserLoanHistory.fixture(user, "C", UserLoanStatus.RETURNED),
            )
        )

        // when
        val result = bookService.countLoanedBook()

        // then
        assertThat(result).isEqualTo(1)
    }

    @Test
    @DisplayName("분야별 책 권수를 정상 확인한다")
    fun getBookStatisticsTest() {
        // given
        bookRepository.saveAll(
            listOf(
                Book.fixture("A", BookType.COMPUTER),
                Book.fixture("B", BookType.COMPUTER),
                Book.fixture("C", BookType.SCIENCE),
            )
        )

        // when
        val results = bookService.getBookStatistics()

        // then
        assertThat(results).hasSize(2)
        assertCount(results, BookType.COMPUTER, 2)
        assertCount(results, BookType.SCIENCE, 1)
    }

    private fun assertCount(results: List<BookStatResponse>, type: BookType, count: Int) {
        assertThat(results.first { result -> result.type == type }.count).isEqualTo(count)
    }
}