# vndb-android

An unofficial Android client for [vndb.org](https://vndb.org), styled like the site (all 23 skins).

- Native screens read from the [Kana API](https://api.vndb.org/kana): search, VN pages (info, tags with
  spoiler levels, releases, characters, staff), and your list, which you can edit (labels, vote,
  dates, notes, release status) with an API token from vndb.org/u/tokens.
- Everything else (forums, profile, reviews, DB edits) opens vndb.org in an in-app web view,
  with your normal site login.
- Images flagged as suggestive/explicit or violent stay hidden until tapped, per your settings.

Data from vndb.org under the VNDB Data License. Not affiliated with VNDB.

## Building

JDK 17+ and the Android SDK (platform 37): `./gradlew assembleDebug`. Unit tests, plus Robolectric
screenshots written to `app/build/shots/`: `./gradlew testDebugUnitTest`.
