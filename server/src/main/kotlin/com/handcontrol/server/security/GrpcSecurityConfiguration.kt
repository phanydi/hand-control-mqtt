package com.handcontrol.server.security

import org.lognet.springboot.grpc.security.EnableGrpcSecurity
import org.lognet.springboot.grpc.security.GrpcSecurityConfigurerAdapter

@EnableGrpcSecurity
class GrpcSecurityConfiguration : GrpcSecurityConfigurerAdapter() {
}