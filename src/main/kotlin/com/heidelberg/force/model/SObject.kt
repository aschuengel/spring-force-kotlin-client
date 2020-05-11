package com.heidelberg.force.model

data class SObject(val custom: Boolean,
                   val label: String,
                   val labelPlural: String,
                   val name: String,
                   val queryable: Boolean)