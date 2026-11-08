package com.ciphervault.app.autofill

/** 自动填充请求的域名/包名规则。 */
object AutofillDomainResolver {
    /** 匹配域名优先，其次调用方包名。 */
    fun resolve(fields: AutofillFields): String = fields.webDomain ?: fields.packageName

    /** 网页请求生成 https URL；包名请求不生成 URL。 */
    fun urlFor(fields: AutofillFields): String =
        fields.webDomain?.let { "https://$it" }.orEmpty()

    /** 新建条目的显示名；无域名时回退固定文案。 */
    fun displayName(domain: String): String = domain.ifBlank { "Autofill" }
}
