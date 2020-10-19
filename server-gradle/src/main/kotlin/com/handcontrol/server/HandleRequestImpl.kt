package com.handcontrol.server

import org.lognet.springboot.grpc.GRpcService
import io.grpc.stub.StreamObserver

@GRpcService
class HandleRequestImpl : HandleRequestGrpc.HandleRequestImplBase() {

    override fun login(request: Request.LoginRequest, responseObserver: StreamObserver<Request.LoginResult>) {
        //do Authorize account here


        //sent reply
        val reply = Request.LoginResult.newBuilder().apply {
            imei = request.imei;
            message = """
                |Hi there! Your login: '${request.login}', 
|                               password: '${request.password}'
                """.trimMargin()
        }.build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }

    override fun proRequest(request: Request.ClientRequets, responseObserver: StreamObserver<Request.ClientReply>) {
        //save request into database here


        //sent reply
        val reply = Request.ClientReply.newBuilder().apply {
            imei = request.imei;
            message = """
                |Your request is protheses Id: '${request.proId}', 
|                               request: '${request.request}'
                """.trimMargin()
        }.build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }
}