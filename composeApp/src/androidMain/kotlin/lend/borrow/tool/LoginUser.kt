package lend.borrow.tool

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.auth

suspend fun createUserWithEmailAndPassword(email: String, password: String) {
    val auth: FirebaseAuth = Firebase.auth
    val  result = auth.createUserWithEmailAndPassword(email, password)
    val user = result.user
}

suspend fun authenticateUserWithEmailAndPassword(email: String, password: String) {
    val auth: FirebaseAuth = Firebase.auth
    val  result = auth.createUserWithEmailAndPassword(email, password)
    val user = result.user
}