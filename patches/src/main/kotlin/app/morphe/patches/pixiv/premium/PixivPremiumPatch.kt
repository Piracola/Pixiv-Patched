package app.morphe.patches.pixiv.premium

import app.morphe.patches.pixiv.shared.PixivConstants

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch

val pixivPremiumPatch: BytecodePatch = bytecodePatch(
    name = "Pixiv Premium Features",
    description = "Emulates client-side Pixiv Premium membership status, removes the mute list limits, and restores the illustration/manga and novel browsing history lists.",
    default = true
) {
    compatibleWith(PixivConstants.PIXIV)

    extendWith("extensions/pixiv.mpe")

    execute {
        // 1. OAuthUser.isPremium() -> always return true (account state used by most UI gates)
        val oauthUserClass = mutableClassDefBy("Ljp/pxv/android/domain/auth/entity/OAuthUser;")
        val isPremiumMethod = oauthUserClass.methods.first { it.name == "m0" && it.returnType == "Z" }
        isPremiumMethod.addInstructions(
            1,
            """
            const/4 v0, 0x1
            return v0
            """.trimIndent()
        )

        // 2. ProfileApiModel.isPremium() -> always return true (remote profile DTO)
        val profileApiClass = mutableClassDefBy("Ljp/pxv/android/data/userstate/remote/dto/ProfileApiModel;")
        val profileIsPremiumMethod = profileApiClass.methods.first { it.name == "f" && it.returnType == "Z" }
        profileIsPremiumMethod.addInstructions(
            1,
            """
            const/4 v0, 0x1
            return v0
            """.trimIndent()
        )

        // 3. PixivProfile constructor -> force the isPremium parameter (p20) to true (domain entity)
        val domainProfileClass = mutableClassDefByOrNull("Ljp/pxv/android/domain/commonentity/PixivProfile;")
        domainProfileClass?.let { cls ->
            val ctor = cls.methods.firstOrNull { it.name == "<init>" && it.parameterTypes.size == 22 }
            ctor?.addInstructions(
                1,
                "const/16 p20, 0x1"
            )
        }

        // 4. MuteSettingResponse.muteLimitCount() -> return 9999 (mute setting screen)
        val muteSettingClass = mutableClassDefByOrNull("Ljp/pxv/android/data/mute/remote/dto/MuteSettingResponse;")
        muteSettingClass?.let { cls ->
            val limitMethod = cls.methods.firstOrNull { it.name == "a" && it.returnType == "I" }
            limitMethod?.addInstructions(
                1,
                """
                const/16 v0, 0x270f
                return v0
                """.trimIndent()
            )
        }

        // 5. MuteLimitForTextApiModel -> return 9999 for both free and premium text mute limits
        val muteTextLimitClass = mutableClassDefByOrNull("Ljp/pxv/android/data/mute/remote/dto/MuteLimitForTextApiModel;")
        muteTextLimitClass?.let { cls ->
            for (name in listOf("a", "b")) {
                cls.methods.firstOrNull { it.name == name && it.returnType == "I" }?.addInstructions(
                    1,
                    """
                    const/16 v0, 0x270f
                    return v0
                    """.trimIndent()
                )
            }
        }

        // 6. v07.a/b map a page of history DTOs into the domain model the history screen renders.
        //    When the server returns an empty page for a non-premium account, build the result from
        //    the locally recorded works instead. Mapping a cache of domain objects through the
        //    original code would fail its per-element check-cast, so the result is constructed here.
        val historyMapperClass = mutableClassDefByOrNull("Lv07;")
        historyMapperClass?.let { cls ->
            for ((methodName, cacheGetter) in listOf("a" to "illustCache", "b" to "novelCache")) {
                val mapper = cls.methods.firstOrNull {
                    it.name == methodName && it.returnType == "Ls07;" && it.parameterTypes.size == 2
                } ?: continue
                mapper.addInstructions(
                    0,
                    """
                    invoke-static {p2}, Lapp/morphe/extension/pixiv/premium/HistoryHelper;->isEmpty(Ljava/util/List;)Z
                    move-result v0
                    if-nez v0, :cond_orig
                    invoke-static {}, Lapp/morphe/extension/pixiv/premium/HistoryHelper;->$cacheGetter()Ljava/util/ArrayList;
                    move-result-object v1
                    new-instance v2, Ls07;
                    const/4 v0, 0x0
                    invoke-direct {v2, v1, v0}, Ls07;-><init>(Ljava/util/ArrayList;Ljp/pxv/android/domain/commonentity/PageableNextUrl;)V
                    return-object v2
                    :cond_orig
                    nop
                    """.trimIndent()
                )
            }
        }

        // NOTE: recording opened works for the history fallback lives in PixivAiFlaggerPatch, which
        // already hooks DetailBottomBarView.setWork. This method keeps `this` in v0 and has no spare
        // local register, so injecting a second hook here is not safe.
    }
}
