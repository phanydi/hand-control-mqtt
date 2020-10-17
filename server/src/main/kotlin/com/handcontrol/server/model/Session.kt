package com.handcontrol.server.model

import org.springframework.data.redis.core.RedisHash
import org.springframework.data.redis.core.index.Indexed

@RedisHash("Session")
class Session (
    @Indexed val id: String
){
}