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

        // 6. IllustBrowsingHistoryResponse.illusts() -> fall back to the locally cached list when the
        //    server returns an empty page for non-premium accounts.
        val historyResponseClass = mutableClassDefByOrNull("Ljp/pxv/android/data/browsinghistory/remote/dto/IllustBrowsingHistoryResponse;")
        historyResponseClass?.let { cls ->
            val historyMethod = cls.methods.firstOrNull { it.name == "a" && it.returnType == "Ljava/util/List;" }
            historyMethod?.addInstructions(
                1,
                """
                invoke-static {p0}, Lapp/morphe/extension/pixiv/premium/HistoryHelper;->getHistoryList(Ljava/lang/Object;)Ljava/util/List;
                move-result-object v0
                return-object v0
                """.trimIndent()
            )
        }

        // 7. NovelBrowsingHistoryResponse.novels() -> same fallback for the novel history tab
        val novelHistoryResponseClass = mutableClassDefByOrNull("Ljp/pxv/android/data/browsinghistory/remote/dto/NovelBrowsingHistoryResponse;")
        novelHistoryResponseClass?.let { cls ->
            val novelHistoryMethod = cls.methods.firstOrNull { it.name == "b" && it.returnType == "Ljava/util/List;" }
            novelHistoryMethod?.addInstructions(
                1,
                """
                invoke-static {p0}, Lapp/morphe/extension/pixiv/premium/HistoryHelper;->getNovelHistoryList(Ljava/lang/Object;)Ljava/util/List;
                move-result-object v0
                return-object v0
                """.trimIndent()
            )
        }

        // 8. DetailBottomBarView.setWork() -> record the opened work (illustration or novel) so the
        //    history fallback above has something to show. The illust class name is pushed in from
        //    here because both artwork types reach this method.
        val bottomBarClass = mutableClassDefByOrNull("Ljp/pxv/android/feature/component/androidview/DetailBottomBarView;")
        bottomBarClass?.let { cls ->
            val setWorkMethod = cls.methods.firstOrNull { it.name == "setWork" && it.parameterTypes.size == 1 }
            setWorkMethod?.addInstructions(
                1,
                """
                const-string v0, "Le97;"
                invoke-static {v0}, Lapp/morphe/extension/pixiv/premium/HistoryHelper;->setIllustClassName(Ljava/lang/String;)V
                invoke-static {p1}, Lapp/morphe/extension/pixiv/premium/HistoryHelper;->recordView(Ljava/lang/Object;)V
                """.trimIndent()
            )
        }
    }
}
