package com.group.libraryapp.domain.user

interface UserRepositoryCustom {

    fun findAllWithUserLoanHistories(): List<User>
}