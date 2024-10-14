package io.github.zidbrain.service.base

import io.github.zidbrain.service.IdTokenParser
import io.github.zidbrain.service.SecretService
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import org.postgresql.ds.PGSimpleDataSource
import javax.sql.DataSource

@Module
@ComponentScan
class TestModule {
    @Factory
    fun dataSource(): DataSource = PGSimpleDataSource().apply {
        setURL("jdbc:postgresql://localhost:5433/postgres")
        user = "postgres"
        password = "123"
    }
}

@Single
class SecretServiceTest : SecretService {
    override fun getSecret(secret: String): String = secret
}

@Single
class TestIdTokenParser : IdTokenParser {
    override fun parseToken(idToken: String): String =
        idToken
}