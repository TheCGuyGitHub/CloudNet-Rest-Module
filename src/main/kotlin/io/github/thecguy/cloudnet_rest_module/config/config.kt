package io.github.thecguy.cloudnet_rest_module.config

import org.jetbrains.annotations.NotNull


class Configuration(
    @NotNull val username: String,
    @NotNull val password: String,
    @NotNull val database: String,
    @NotNull val host: String,
    @NotNull val port: Int,
    @NotNull val restapi_port: Int
)