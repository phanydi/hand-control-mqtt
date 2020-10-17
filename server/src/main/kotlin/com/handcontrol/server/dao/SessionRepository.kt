package com.handcontrol.server.dao

import com.handcontrol.server.model.Session
import org.springframework.data.repository.CrudRepository

interface SessionRepository: CrudRepository<Session, String>