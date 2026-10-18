package cl.miradio.app.di

import android.content.Context
import androidx.room.Room
import cl.miradio.app.data.local.RadioDatabase
import cl.miradio.app.data.local.dao.FavoriteDao
import cl.miradio.app.data.local.dao.StationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RadioDatabase =
        Room.databaseBuilder(context, RadioDatabase::class.java, RadioDatabase.NAME)
            .build()

    @Provides
    fun provideStationDao(db: RadioDatabase): StationDao = db.stationDao()

    @Provides
    fun provideFavoriteDao(db: RadioDatabase): FavoriteDao = db.favoriteDao()
}
