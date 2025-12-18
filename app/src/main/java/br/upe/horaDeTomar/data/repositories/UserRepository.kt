package br.upe.horaDeTomar.data.repositories

import br.upe.horaDeTomar.data.daos.UserDao
import br.upe.horaDeTomar.data.entities.User
import kotlinx.coroutines.flow.Flow

class UserRepository(
    private val dao: UserDao
) {

    suspend fun insert(user: User): Long {
        return dao.insert(user)
    }

    suspend fun update(user: User) {
        dao.update(user)
    }

    suspend fun delete(user: User) {
        dao.delete(user)
    }

    suspend fun getUsersByAccount(accountId: Int): List<User>? {
        return dao.getUsersByAccount(accountId)
    }

    suspend fun getById(id: Int): User? {
        return dao.getUserById(id)
    }

    val users: Flow<List<User>> = dao.getUsers();
}
