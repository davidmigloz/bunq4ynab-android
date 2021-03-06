package app.bunq4ynab.data.bunq.auth.di

import app.bunq4ynab.data.bunq.auth.BunqAuthManagerImpl
import app.bunq4ynab.domain.repository.bunq.BunqAuthManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
internal abstract class BunqAuthModule {

    @Singleton
    @Binds
    abstract fun bindBunqAuthManager(
        impl: BunqAuthManagerImpl
    ): BunqAuthManager
}
