package com.group.libraryapp.domain.book

import com.group.libraryapp.dto.book.response.BookStatResponse
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.*

interface BookRepository : JpaRepository<Book, Long> {

    fun findByName(bookName: String): Book?

    @Query("select new com.group.libraryapp.dto.book.response.BookStatResponse(b.type, count(b.id)) " +
            "from Book as b " +
            "group by b.type")
    fun findStats(): List<BookStatResponse>
}