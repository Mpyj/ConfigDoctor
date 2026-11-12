package com.configdoctor.parser

import com.configdoctor.model.VlessConfig

interface ConfigParser {
    fun canParse(link: String): Boolean
    fun parse(link: String): Result<VlessConfig>
    fun buildLink(config: VlessConfig): String
}