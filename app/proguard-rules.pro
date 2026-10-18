# kotlinx.serialization: conserva los serializadores generados de los DTO.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers @kotlinx.serialization.Serializable class cl.miradio.app.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class cl.miradio.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit: las interfaces de servicio se usan vía reflexión.
-keep,allowobfuscation interface cl.miradio.app.data.remote.RadioBrowserApi
-keepattributes Signature, Exceptions
