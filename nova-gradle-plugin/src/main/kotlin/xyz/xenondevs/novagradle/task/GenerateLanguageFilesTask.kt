package xyz.xenondevs.novagradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

private val LANGUAGE_CODES = arrayOf(
    "af_za", "ar_sa", "ast_es", "az_az", "bar", "ba_ru", "be_by", "bg_bg", "brb", "br_fr", "bs_ba", "ca_es", "cs_cz",
    "cy_gb", "da_dk", "de_at", "de_ch", "de_de", "el_gr", "en_au", "en_ca", "en_gb", "en_nz", "en_us", "eo_uy", "esan",
    "es_ar", "es_cl", "es_ec", "es_es", "es_mx", "es_uy", "es_ve", "et_ee", "eu_es", "fa_ir", "fil_ph", "fi_fi", "fo_fo",
    "fra_de", "fr_ca", "fr_fr", "fur_it", "fy_nl", "ga_ie", "gd_gb", "gl_es", "haw_us", "he_il", "hi_in", "hr_hr",
    "hu_hu", "hy_am", "id_id", "ig_ng", "io_en", "isv", "is_is", "it_it", "ja_jp", "jbo_en", "ka_ge", "kk_kz", "kn_in",
    "ko_kr", "ksh", "kw_gb", "la_la", "lb_lu", "li_li", "lmo", "lt_lt", "lv_lv", "lzh", "mk_mk", "mn_mn", "ms_my",
    "mt_mt", "nah", "nds_de", "nl_be", "nl_nl", "nn_no", "no_no", "oc_fr", "ovd", "pl_pl", "pt_br", "pt_pt", "ro_ro",
    "rpr", "ru_ru", "ry_ua", "se_no", "sk_sk", "sl_si", "so_so", "sq_al", "sr_sp", "sv_se", "sxu", "szl", "ta_in",
    "th_th", "tl_ph", "tok", "tr_tr", "tt_ru", "uk_ua", "val_es", "vec_it", "vi_vn", "yi_de", "yo_ng", "zh_cn",
    "zh_hk", "zh_tw", "zlm_arab"
)

internal abstract class GenerateLanguageFilesTask : DefaultTask() {
    
    @TaskAction
    fun run() {
        val langDir = project.projectDir.resolve("src/main/resources/assets/lang/")
        langDir.mkdirs()
        LANGUAGE_CODES.forEach { 
            val langFile = langDir.resolve("$it.json")
            if (!langFile.exists())
                langFile.writeText("{}")
        }
    }
    
}