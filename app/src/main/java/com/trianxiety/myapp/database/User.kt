package com.trianxiety.myapp.database

data class User(val user: String?) {

    var firstName: String = ""
    var lastName: String = ""
    var nationality: String = ""
    var remember: Boolean = false

}