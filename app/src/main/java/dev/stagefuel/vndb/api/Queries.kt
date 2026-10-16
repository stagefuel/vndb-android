package dev.stagefuel.vndb.api

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/** Field lists and filters for the screens. Kept in one place so request sizes stay visible. */
object Fields {
  const val IMAGE = "image.url,image.dims,image.sexual,image.violence,image.thumbnail,image.thumbnail_dims"
  const val VN_BRIEF = "id,title,alttitle,released,rating,votecount,developers.name,$IMAGE"
  const val VN_FULL = "id,title,alttitle,titles.lang,titles.title,titles.latin,titles.official,titles.main," +
    "aliases,olang,devstatus,released,languages,platforms,$IMAGE,length,length_minutes,length_votes," +
    "description,rating,average,votecount,developers.id,developers.name,developers.original," +
    "tags.id,tags.name,tags.rating,tags.spoiler,tags.lie,tags.category," +
    "relations.id,relations.relation,relations.relation_official,relations.title,relations.alttitle,relations.released," +
    "screenshots.url,screenshots.dims,screenshots.sexual,screenshots.violence,screenshots.thumbnail,screenshots.thumbnail_dims," +
    "screenshots.release.id,screenshots.release.languages.lang," +
    "staff.id,staff.name,staff.original,staff.role,staff.note,staff.eid,extlinks.url,extlinks.label," +
    "editions.eid,editions.lang,editions.name,editions.official," +
    "va.note,va.character.id,va.character.name,va.staff.id,va.staff.name,va.staff.original"
  const val RELEASE = "id,title,alttitle,languages.lang,languages.mtl,languages.main,platforms,media.medium,media.qty," +
    "producers.id,producers.name,producers.developer,producers.publisher,released,minage,patch,freeware," +
    "uncensored,official,has_ero,voiced,engine,notes,vns.id,vns.rtype"
  const val CHARACTER_BRIEF = "id,name,original,image.url,image.dims,image.sexual,image.violence,sex,vns.id,vns.role,vns.spoiler"
  const val CHARACTER_FULL = "id,name,original,aliases,description,$IMAGE,blood_type,height,weight,bust,waist,hips,cup,age," +
    "birthday,sex,vns.id,vns.role,vns.spoiler,vns.title,vns.release.id,traits.id,traits.name,traits.group_name,traits.spoiler,traits.lie"
  const val STAFF = "id,name,original,lang,gender,description,extlinks.url,extlinks.label,aliases.aid,aliases.name,aliases.latin,aliases.ismain"
  const val PRODUCER = "id,name,original,aliases,lang,type,description,extlinks.url,extlinks.label"
  const val TAG = "id,name,aliases,description,category,searchable,applicable,vn_count"
  const val TRAIT = "id,name,aliases,description,searchable,applicable,group_id,group_name,char_count"
  const val RELEASE_FULL = "id,title,alttitle,languages.lang,languages.title,languages.mtl,languages.main,platforms,media.medium,media.qty," +
    "vns.id,vns.title,vns.rtype,producers.id,producers.name,producers.developer,producers.publisher,released,minage,patch," +
    "freeware,uncensored,official,has_ero,resolution,engine,voiced,notes,gtin,catalog,extlinks.url,extlinks.label"
  const val ULIST = "id,added,voted,lastmod,vote,started,finished,notes,labels.id,labels.label," +
    "vn.title,vn.alttitle,vn.released,vn.rating,vn.developers.name,vn.image.url,vn.image.sexual,vn.image.violence," +
    "releases.id,releases.list_status,releases.title"
  const val RELEASE_IMAGES = "id,title,released,images.id,images.type,images.url,images.dims,images.sexual," +
    "images.violence,images.thumbnail,images.thumbnail_dims,images.photo,images.languages"
  const val FEED = "id,title,released,platforms,languages.lang,vns.id,vns.title,vns.image.url,vns.image.sexual," +
    "vns.image.violence,vns.image.thumbnail"
  const val QUOTE = "id,quote,score,vn.id,vn.title,character.id,character.name"
}

object Filters {
  fun f(vararg parts: Any): JsonElement = JsonArray(parts.map {
    when (it) {
      is JsonElement -> it
      is String -> JsonPrimitive(it)
      is Number -> JsonPrimitive(it)
      is Boolean -> JsonPrimitive(it)
      else -> error("bad filter part $it")
    }
  })

  fun id(id: String) = f("id", "=", id)
  fun search(q: String) = f("search", "=", q)
  fun vn(id: String) = f("vn", "=", id(id))
  fun label(label: Int) = f("label", "=", label)
  fun staff(id: String) = f("staff", "=", id(id))
  fun seiyuu(id: String) = f("seiyuu", "=", id(id))
  fun developer(id: String) = f("developer", "=", id(id))
  fun tag(id: String) = f("tag", "=", id)
  fun trait(id: String) = f("trait", "=", id)
  val randomQuote = f("random", "=", 1)
  fun quotesOf(vnId: String) = f("vn", "=", id(vnId))

  /** `v17`, `https://vndb.org/v17` or `vndb.org/v17/chars` → `v17`; anything else → null. */
  fun vnId(q: String): String? =
    Regex("""^(?:https?://)?(?:www\.)?(?:vndb\.org/)?(v\d+)\b""", RegexOption.IGNORE_CASE)
      .find(q.trim())?.groupValues?.get(1)?.lowercase()
}
