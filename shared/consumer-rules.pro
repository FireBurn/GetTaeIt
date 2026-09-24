# Applied to every app that depends on :shared.

# Gson reads and writes these by field name: Room type converters, the Firestore
# backup, Wear Data Layer snapshots, routine templates and data export all need
# the names (and enum constants) left exactly as they are in source.
-keep class uk.co.fireburn.gettaeit.shared.data.** { <fields>; <init>(...); }
-keep class uk.co.fireburn.gettaeit.shared.domain.RoutineTemplate** { <fields>; <init>(...); }
-keepclassmembers enum uk.co.fireburn.gettaeit.shared.** { <fields>; }
