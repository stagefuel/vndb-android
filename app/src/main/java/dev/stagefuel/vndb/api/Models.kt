package dev.stagefuel.vndb.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** Body of every `POST /<type>` query. [filters] is Kana's nested-array filter syntax. */
@Serializable
data class Query(
  val filters: JsonElement? = null,
  val fields: String,
  val sort: String? = null,
  val reverse: Boolean? = null,
  val results: Int? = null,
  val page: Int? = null,
  val user: String? = null,
  val count: Boolean? = null,
)

@Serializable
data class QueryResult<T>(val results: List<T> = emptyList(), val more: Boolean = false, val count: Int? = null)

@Serializable
data class AuthInfo(val id: String, val username: String, val permissions: List<String> = emptyList())

@Serializable
data class UlistLabels(val labels: List<UlistLabel> = emptyList())

@Serializable
data class UlistLabel(val id: Int, val label: String, val private: Boolean = false, val count: Int? = null)

@Serializable
data class Image(
  val id: String? = null,
  val url: String? = null,
  val dims: List<Int>? = null,
  val sexual: Double = 0.0,
  val violence: Double = 0.0,
  val thumbnail: String? = null,
  @SerialName("thumbnail_dims") val thumbnailDims: List<Int>? = null,
)

@Serializable
data class Title(val lang: String, val title: String, val latin: String? = null, val official: Boolean = true, val main: Boolean = false)

@Serializable
data class ProducerRef(
  val id: String = "",
  val name: String,
  val original: String? = null,
  val developer: Boolean = false,
  val publisher: Boolean = false,
)

@Serializable
data class VnTag(
  val id: String,
  val name: String,
  val rating: Double,
  val spoiler: Int = 0,
  val lie: Boolean = false,
  val category: String = "cont",
)

@Serializable
data class VnRelation(
  val id: String,
  val relation: String,
  @SerialName("relation_official") val official: Boolean = true,
  val title: String,
  val alttitle: String? = null,
  val released: String? = null,
)

@Serializable
data class Screenshot(
  val url: String,
  val dims: List<Int>? = null,
  val sexual: Double = 0.0,
  val violence: Double = 0.0,
  val thumbnail: String? = null,
  @SerialName("thumbnail_dims") val thumbnailDims: List<Int>? = null,
  val release: ReleaseRef? = null,
)

@Serializable
data class ReleaseRef(val id: String? = null, val languages: List<ReleaseLang> = emptyList())

@Serializable
data class VnStaff(
  val id: String,
  val name: String,
  val original: String? = null,
  val role: String,
  val note: String? = null,
  /** Edition this credit belongs to (vn.editions); null = the original release. */
  val eid: Int? = null,
)

@Serializable
data class Edition(val eid: Int, val lang: String? = null, val name: String = "", val official: Boolean = true)

@Serializable
data class VaRef(val id: String, val name: String = "", val original: String? = null)

/** vn.va: who voices which character, with an optional note (e.g. which edition). */
@Serializable
data class VnVa(val note: String? = null, val character: VaRef? = null, val staff: VaRef? = null)

@Serializable
data class ExtLink(val url: String, val label: String, val name: String? = null)

@Serializable
data class Vn(
  val id: String,
  val title: String,
  val alttitle: String? = null,
  val titles: List<Title> = emptyList(),
  val aliases: List<String> = emptyList(),
  val olang: String? = null,
  val devstatus: Int = 0,
  val released: String? = null,
  val languages: List<String> = emptyList(),
  val platforms: List<String> = emptyList(),
  val image: Image? = null,
  val length: Int? = null,
  @SerialName("length_minutes") val lengthMinutes: Int? = null,
  @SerialName("length_votes") val lengthVotes: Int? = null,
  val description: String? = null,
  val rating: Double? = null,
  val average: Double? = null,
  val votecount: Int = 0,
  val developers: List<ProducerRef> = emptyList(),
  val tags: List<VnTag> = emptyList(),
  val relations: List<VnRelation> = emptyList(),
  val screenshots: List<Screenshot> = emptyList(),
  val staff: List<VnStaff> = emptyList(),
  val extlinks: List<ExtLink> = emptyList(),
  val editions: List<Edition> = emptyList(),
  val va: List<VnVa> = emptyList(),
)

@Serializable
data class ReleaseLang(val lang: String, val title: String? = null, val latin: String? = null, val mtl: Boolean = false, val main: Boolean = false)

@Serializable
data class Media(val medium: String, val qty: Int = 0)

@Serializable
data class ReleaseVn(val id: String, val rtype: String = "complete", val title: String? = null)

@Serializable
data class Release(
  val id: String,
  val title: String,
  val alttitle: String? = null,
  val languages: List<ReleaseLang> = emptyList(),
  val platforms: List<String> = emptyList(),
  val media: List<Media> = emptyList(),
  val vns: List<ReleaseVn> = emptyList(),
  val producers: List<ProducerRef> = emptyList(),
  val released: String? = null,
  val minage: Int? = null,
  val patch: Boolean = false,
  val freeware: Boolean = false,
  val uncensored: Boolean? = null,
  val official: Boolean = true,
  @SerialName("has_ero") val hasEro: Boolean = false,
  val voiced: Int? = null,
  val engine: String? = null,
  val notes: String? = null,
  val extlinks: List<ExtLink> = emptyList(),
)

@Serializable
data class CharVn(val id: String, val role: String, val spoiler: Int = 0, val title: String? = null)

@Serializable
data class CharTrait(val id: String, val name: String, @SerialName("group_name") val groupName: String? = null, val spoiler: Int = 0, val lie: Boolean = false)

@Serializable
data class Character(
  val id: String,
  val name: String,
  val original: String? = null,
  val aliases: List<String> = emptyList(),
  val description: String? = null,
  val image: Image? = null,
  @SerialName("blood_type") val bloodType: String? = null,
  val height: Int? = null,
  val weight: Int? = null,
  val bust: Int? = null,
  val waist: Int? = null,
  val hips: Int? = null,
  val cup: String? = null,
  val age: Int? = null,
  val birthday: List<Int>? = null,
  val sex: List<String?>? = null,
  val vns: List<CharVn> = emptyList(),
  val traits: List<CharTrait> = emptyList(),
)

@Serializable
data class UlistRelease(val id: String, @SerialName("list_status") val listStatus: Int = 0, val title: String? = null)

@Serializable
data class UlistVn(
  val title: String,
  val alttitle: String? = null,
  val released: String? = null,
  val image: Image? = null,
  val rating: Double? = null,
  val developers: List<ProducerRef> = emptyList(),
)

@Serializable
data class UlistEntry(
  val id: String,
  val added: Long? = null,
  val voted: Long? = null,
  val lastmod: Long? = null,
  val vote: Int? = null,
  val started: String? = null,
  val finished: String? = null,
  val notes: String? = null,
  val labels: List<UlistLabel> = emptyList(),
  val vn: UlistVn? = null,
  val releases: List<UlistRelease> = emptyList(),
)

@Serializable
data class CharVnFull(
  val id: String,
  val role: String,
  val spoiler: Int = 0,
  val title: String? = null,
  val release: ReleaseRef? = null,
)

@Serializable
data class CharacterFull(
  val id: String,
  val name: String,
  val original: String? = null,
  val aliases: List<String> = emptyList(),
  val description: String? = null,
  val image: Image? = null,
  @SerialName("blood_type") val bloodType: String? = null,
  val height: Int? = null,
  val weight: Int? = null,
  val bust: Int? = null,
  val waist: Int? = null,
  val hips: Int? = null,
  val cup: String? = null,
  val age: Int? = null,
  val birthday: List<Int>? = null,
  val sex: List<String?>? = null,
  val vns: List<CharVnFull> = emptyList(),
  val traits: List<CharTrait> = emptyList(),
)

@Serializable
data class StaffAlias(val aid: Int, val name: String, val latin: String? = null, val ismain: Boolean = false)

@Serializable
data class Staff(
  val id: String,
  val name: String,
  val original: String? = null,
  val lang: String? = null,
  val gender: String? = null,
  val description: String? = null,
  val extlinks: List<ExtLink> = emptyList(),
  val aliases: List<StaffAlias> = emptyList(),
)

@Serializable
data class Producer(
  val id: String,
  val name: String,
  val original: String? = null,
  val aliases: List<String> = emptyList(),
  val lang: String? = null,
  val type: String? = null,
  val description: String? = null,
  val extlinks: List<ExtLink> = emptyList(),
)

@Serializable
data class Tag(
  val id: String,
  val name: String,
  val aliases: List<String> = emptyList(),
  val description: String? = null,
  val category: String = "cont",
  val searchable: Boolean = true,
  val applicable: Boolean = true,
  @SerialName("vn_count") val vnCount: Int = 0,
)

@Serializable
data class Trait(
  val id: String,
  val name: String,
  val aliases: List<String> = emptyList(),
  val description: String? = null,
  val searchable: Boolean = true,
  val applicable: Boolean = true,
  @SerialName("group_id") val groupId: String? = null,
  @SerialName("group_name") val groupName: String? = null,
  @SerialName("char_count") val charCount: Int = 0,
)

@Serializable
data class Stats(
  val chars: Int = 0,
  val producers: Int = 0,
  val releases: Int = 0,
  val staff: Int = 0,
  val tags: Int = 0,
  val traits: Int = 0,
  val vn: Int = 0,
)

@Serializable
data class ReleaseFull(
  val id: String,
  val title: String,
  val alttitle: String? = null,
  val languages: List<ReleaseLang> = emptyList(),
  val platforms: List<String> = emptyList(),
  val media: List<Media> = emptyList(),
  val vns: List<ReleaseVn> = emptyList(),
  val producers: List<ProducerRef> = emptyList(),
  val released: String? = null,
  val minage: Int? = null,
  val patch: Boolean = false,
  val freeware: Boolean = false,
  val uncensored: Boolean? = null,
  val official: Boolean = true,
  @SerialName("has_ero") val hasEro: Boolean = false,
  val resolution: kotlinx.serialization.json.JsonElement? = null,
  val engine: String? = null,
  val voiced: Int? = null,
  val notes: String? = null,
  val gtin: String? = null,
  val catalog: String? = null,
  val extlinks: List<ExtLink> = emptyList(),
)

/** VN with only the staff credits a staff page needs. */
@Serializable
data class VnCredit(val id: String, val title: String, val released: String? = null, val staff: List<VnStaffRef> = emptyList())

@Serializable
data class VnStaffRef(val id: String, val role: String, val note: String? = null)

@Serializable
data class UserInfo(
  val id: String,
  val username: String,
  val lengthvotes: Int = 0,
  @SerialName("lengthvotes_sum") val lengthvotesSum: Int = 0,
)

@Serializable
data class QuoteRef(val id: String = "", val title: String? = null, val name: String? = null)

@Serializable
data class Quote(val id: String, val quote: String, val score: Int = 0, val vn: QuoteRef? = null, val character: QuoteRef? = null)

/** A release's package/digital image (`release.images`): type is pkgfront, pkgback, pkgside, pkgmed, pkgcontent or dig. */
@Serializable
data class ReleaseImage(
  val id: String,
  val type: String = "",
  val url: String? = null,
  val dims: List<Int>? = null,
  val sexual: Double = 0.0,
  val violence: Double = 0.0,
  val thumbnail: String? = null,
  @SerialName("thumbnail_dims") val thumbnailDims: List<Int>? = null,
  val photo: Boolean = false,
  /** Languages the image is for (null = all of the release's). */
  val languages: List<String>? = null,
) {
  fun asImage() = Image(id = id, url = url, dims = dims, sexual = sexual, violence = violence, thumbnail = thumbnail, thumbnailDims = thumbnailDims)
}

@Serializable
data class ReleaseImages(val id: String, val title: String = "", val released: String? = null, val images: List<ReleaseImage> = emptyList())

@Serializable
data class IdOnly(val id: String)

@Serializable
data class FeedVn(val id: String, val title: String = "", val image: Image? = null)

/** A release in the upcoming / just-released feeds, with its VN's title and cover. */
@Serializable
data class FeedRelease(
  val id: String,
  val title: String = "",
  val released: String? = null,
  val platforms: List<String> = emptyList(),
  val languages: List<ReleaseLang> = emptyList(),
  val vns: List<FeedVn> = emptyList(),
)
