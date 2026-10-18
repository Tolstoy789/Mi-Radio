package cl.miradio.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Caché local de una estación. Se reemplaza en cada sincronización con la API,
 * por eso los favoritos viven en su propia tabla ([FavoriteEntity]) y no se pierden.
 */
@Entity(
    tableName = "stations",
    indices = [Index("name"), Index("popularity_rank")],
)
data class StationEntity(
    @PrimaryKey
    @ColumnInfo(name = "station_uuid") val stationUuid: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "stream_url") val streamUrl: String,
    @ColumnInfo(name = "homepage") val homepage: String?,
    @ColumnInfo(name = "favicon_url") val faviconUrl: String?,
    /** Tags en minúsculas separados por coma, tal como vienen de la API. */
    @ColumnInfo(name = "tags") val tags: String,
    @ColumnInfo(name = "country_code") val countryCode: String,
    @ColumnInfo(name = "state") val state: String?,
    @ColumnInfo(name = "language") val language: String?,
    @ColumnInfo(name = "codec") val codec: String?,
    @ColumnInfo(name = "bitrate") val bitrate: Int,
    @ColumnInfo(name = "is_hls") val isHls: Boolean,
    @ColumnInfo(name = "votes") val votes: Int,
    @ColumnInfo(name = "click_count") val clickCount: Int,
    /** Posición en la respuesta de la API (ordenada por votos); define el orden de la grilla. */
    @ColumnInfo(name = "popularity_rank") val popularityRank: Int,
    /** Momento de la sincronización que escribió esta fila; sirve para borrar estaciones obsoletas. */
    @ColumnInfo(name = "synced_at") val syncedAt: Long,
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    @ColumnInfo(name = "station_uuid") val stationUuid: String,
    @ColumnInfo(name = "added_at") val addedAt: Long,
)

/** Resultado de unir una estación con su estado de favorito. */
data class StationWithFavorite(
    @Embedded val station: StationEntity,
    @ColumnInfo(name = "is_favorite") val isFavorite: Boolean,
)
