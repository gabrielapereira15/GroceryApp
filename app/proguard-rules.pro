# Release builds are shrunk with R8. The libraries used here (Room, Navigation's
# type-safe routes, kotlinx.serialization, Play services) ship their own keep
# rules, so nothing app-specific is needed yet.

# Keep line numbers so crash reports stay readable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
