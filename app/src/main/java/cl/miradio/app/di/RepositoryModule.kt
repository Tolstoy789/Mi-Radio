package cl.miradio.app.di

import cl.miradio.app.data.repository.StationRepository
import cl.miradio.app.data.repository.StationRepositoryImpl
import cl.miradio.app.domain.model.Clock
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindStationRepository(impl: StationRepositoryImpl): StationRepository

    companion object {
        @Provides
        fun provideClock(): Clock = Clock { System.currentTimeMillis() }
    }
}
