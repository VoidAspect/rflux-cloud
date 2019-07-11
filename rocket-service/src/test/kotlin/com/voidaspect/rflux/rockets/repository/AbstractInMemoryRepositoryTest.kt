package com.voidaspect.rflux.rockets.repository

import com.voidaspect.rflux.rockets.model.Stored
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import reactor.test.test
import java.util.UUID

internal class AbstractInMemoryRepositoryTest {

    @Test
    fun `should not allow duplicate keys`() {
        val key = "d-key"
        val repo = object : AbstractInMemoryRepository<String, String>() {
            override fun initialize(entity: String): Stored<String, String> {
                return Stored(key, entity)
            }
        }
        repo.add("one").test()
                .expectNext(Stored(key, "one"))
                .verifyComplete()

        repo.add("two").test().verifyErrorSatisfies { e ->
            assertAll({
                assertTrue(e is IllegalStateException)
            }, {
                assertEquals("Can't store new entity: duplicate key $key", e.message)
            })
        }
    }

    @Test
    fun `should fail to update absent entities`() {
        val repo = object : AbstractInMemoryUUIDRepository<String>() {}
        val id = UUID.randomUUID()
        repo.update(id, "update").test().verifyErrorSatisfies { e ->
            assertAll({
                assertTrue(e is NoSuchElementException)
            },{
                assertEquals("Can't update stored entity, id $id not found", e.message)
            })
        }
    }
}