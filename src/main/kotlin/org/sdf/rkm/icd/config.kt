package org.sdf.rkm.icd

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter

@Configuration
class Config: WebMvcConfigurerAdapter() {
    // to allow for dots in our resource IDs
    override fun configurePathMatch(configurer: PathMatchConfigurer) {
        configurer.isUseSuffixPatternMatch = false
    }
}