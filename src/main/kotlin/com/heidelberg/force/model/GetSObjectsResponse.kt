package com.heidelberg.force.model

data class GetSObjectsResponse(val encoding: String,
                               val maxBatchSize: Int,
                               val sobjects: List<SObject>)
