package com.heidelberg.force.model

import com.fasterxml.jackson.annotation.JsonProperty

data class ServiceLimit(@JsonProperty("Max") val max: Int,
                        @JsonProperty("Remaining") val remaining: Int)
