package com.configdoctor.parser

object ConfigParserFactory {
    private val parsers = listOf(VlessParser, VmessParser, TrojanParser)

    fun getParser(link: String): ConfigParser? {
        return parsers.firstOrNull { it.canParse(link) }
    }

    fun parse(link: String): Result<com.configdoctor.model.VlessConfig> {
        val parser = getParser(link) ?: return Result.failure(IllegalArgumentException("فرمت لینک پشتیبانی نمی‌شود"))
        return parser.parse(link)
    }

    fun buildLink(config: com.configdoctor.model.VlessConfig): String {
        val parser = parsers.firstOrNull { it.canParse("${config.protocol}://") } ?: VlessParser
        return parser.buildLink(config)
    }
}