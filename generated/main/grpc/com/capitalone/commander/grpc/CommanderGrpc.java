/*
 * Copyright 2016 Capital One Services, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 *
 * SPDX-Copyright: Copyright (c) Capital One Services, LLC
 * SPDX-License-Identifier: Apache-2.0
 */

package com.capitalone.commander.grpc;

import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.0.0)",
    comments = "Source: commander.proto")
public class CommanderGrpc {

  private CommanderGrpc() {}

  public static final String SERVICE_NAME = "commander.Commander";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<com.capitalone.commander.grpc.CommanderProtos.CommandParams,
      com.capitalone.commander.grpc.CommanderProtos.Command> METHOD_CREATE_COMMAND =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "commander.Commander", "CreateCommand"),
          io.grpc.protobuf.ProtoUtils.marshaller(com.capitalone.commander.grpc.CommanderProtos.CommandParams.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(com.capitalone.commander.grpc.CommanderProtos.Command.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<com.capitalone.commander.grpc.CommanderProtos.PagingInfo,
      com.capitalone.commander.grpc.CommanderProtos.PagedCommands> METHOD_LIST_COMMANDS =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "commander.Commander", "ListCommands"),
          io.grpc.protobuf.ProtoUtils.marshaller(com.capitalone.commander.grpc.CommanderProtos.PagingInfo.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(com.capitalone.commander.grpc.CommanderProtos.PagedCommands.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<com.capitalone.commander.grpc.CommanderProtos.UUID,
      com.capitalone.commander.grpc.CommanderProtos.Command> METHOD_COMMAND_BY_ID =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "commander.Commander", "CommandById"),
          io.grpc.protobuf.ProtoUtils.marshaller(com.capitalone.commander.grpc.CommanderProtos.UUID.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(com.capitalone.commander.grpc.CommanderProtos.Command.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<com.capitalone.commander.grpc.CommanderProtos.StreamRequest,
      com.capitalone.commander.grpc.CommanderProtos.Command> METHOD_COMMAND_STREAM =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING,
          generateFullMethodName(
              "commander.Commander", "CommandStream"),
          io.grpc.protobuf.ProtoUtils.marshaller(com.capitalone.commander.grpc.CommanderProtos.StreamRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(com.capitalone.commander.grpc.CommanderProtos.Command.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<com.capitalone.commander.grpc.CommanderProtos.PagingInfo,
      com.capitalone.commander.grpc.CommanderProtos.PagedEvents> METHOD_LIST_EVENTS =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "commander.Commander", "ListEvents"),
          io.grpc.protobuf.ProtoUtils.marshaller(com.capitalone.commander.grpc.CommanderProtos.PagingInfo.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(com.capitalone.commander.grpc.CommanderProtos.PagedEvents.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<com.capitalone.commander.grpc.CommanderProtos.UUID,
      com.capitalone.commander.grpc.CommanderProtos.Event> METHOD_EVENT_BY_ID =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "commander.Commander", "EventById"),
          io.grpc.protobuf.ProtoUtils.marshaller(com.capitalone.commander.grpc.CommanderProtos.UUID.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(com.capitalone.commander.grpc.CommanderProtos.Event.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<com.capitalone.commander.grpc.CommanderProtos.StreamRequest,
      com.capitalone.commander.grpc.CommanderProtos.Event> METHOD_EVENT_STREAM =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING,
          generateFullMethodName(
              "commander.Commander", "EventStream"),
          io.grpc.protobuf.ProtoUtils.marshaller(com.capitalone.commander.grpc.CommanderProtos.StreamRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(com.capitalone.commander.grpc.CommanderProtos.Event.getDefaultInstance()));

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static CommanderStub newStub(io.grpc.Channel channel) {
    return new CommanderStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static CommanderBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new CommanderBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary and streaming output calls on the service
   */
  public static CommanderFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new CommanderFutureStub(channel);
  }

  /**
   */
  public static abstract class CommanderImplBase implements io.grpc.BindableService {

    /**
     */
    public void createCommand(com.capitalone.commander.grpc.CommanderProtos.CommandParams request,
        io.grpc.stub.StreamObserver<com.capitalone.commander.grpc.CommanderProtos.Command> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_CREATE_COMMAND, responseObserver);
    }

    /**
     */
    public void listCommands(com.capitalone.commander.grpc.CommanderProtos.PagingInfo request,
        io.grpc.stub.StreamObserver<com.capitalone.commander.grpc.CommanderProtos.PagedCommands> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_LIST_COMMANDS, responseObserver);
    }

    /**
     */
    public void commandById(com.capitalone.commander.grpc.CommanderProtos.UUID request,
        io.grpc.stub.StreamObserver<com.capitalone.commander.grpc.CommanderProtos.Command> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_COMMAND_BY_ID, responseObserver);
    }

    /**
     */
    public void commandStream(com.capitalone.commander.grpc.CommanderProtos.StreamRequest request,
        io.grpc.stub.StreamObserver<com.capitalone.commander.grpc.CommanderProtos.Command> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_COMMAND_STREAM, responseObserver);
    }

    /**
     */
    public void listEvents(com.capitalone.commander.grpc.CommanderProtos.PagingInfo request,
        io.grpc.stub.StreamObserver<com.capitalone.commander.grpc.CommanderProtos.PagedEvents> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_LIST_EVENTS, responseObserver);
    }

    /**
     */
    public void eventById(com.capitalone.commander.grpc.CommanderProtos.UUID request,
        io.grpc.stub.StreamObserver<com.capitalone.commander.grpc.CommanderProtos.Event> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_EVENT_BY_ID, responseObserver);
    }

    /**
     */
    public void eventStream(com.capitalone.commander.grpc.CommanderProtos.StreamRequest request,
        io.grpc.stub.StreamObserver<com.capitalone.commander.grpc.CommanderProtos.Event> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_EVENT_STREAM, responseObserver);
    }

    @java.lang.Override public io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            METHOD_CREATE_COMMAND,
            asyncUnaryCall(
              new MethodHandlers<
                com.capitalone.commander.grpc.CommanderProtos.CommandParams,
                com.capitalone.commander.grpc.CommanderProtos.Command>(
                  this, METHODID_CREATE_COMMAND)))
          .addMethod(
            METHOD_LIST_COMMANDS,
            asyncUnaryCall(
              new MethodHandlers<
                com.capitalone.commander.grpc.CommanderProtos.PagingInfo,
                com.capitalone.commander.grpc.CommanderProtos.PagedCommands>(
                  this, METHODID_LIST_COMMANDS)))
          .addMethod(
            METHOD_COMMAND_BY_ID,
            asyncUnaryCall(
              new MethodHandlers<
                com.capitalone.commander.grpc.CommanderProtos.UUID,
                com.capitalone.commander.grpc.CommanderProtos.Command>(
                  this, METHODID_COMMAND_BY_ID)))
          .addMethod(
            METHOD_COMMAND_STREAM,
            asyncServerStreamingCall(
              new MethodHandlers<
                com.capitalone.commander.grpc.CommanderProtos.StreamRequest,
                com.capitalone.commander.grpc.CommanderProtos.Command>(
                  this, METHODID_COMMAND_STREAM)))
          .addMethod(
            METHOD_LIST_EVENTS,
            asyncUnaryCall(
              new MethodHandlers<
                com.capitalone.commander.grpc.CommanderProtos.PagingInfo,
                com.capitalone.commander.grpc.CommanderProtos.PagedEvents>(
                  this, METHODID_LIST_EVENTS)))
          .addMethod(
            METHOD_EVENT_BY_ID,
            asyncUnaryCall(
              new MethodHandlers<
                com.capitalone.commander.grpc.CommanderProtos.UUID,
                com.capitalone.commander.grpc.CommanderProtos.Event>(
                  this, METHODID_EVENT_BY_ID)))
          .addMethod(
            METHOD_EVENT_STREAM,
            asyncServerStreamingCall(
              new MethodHandlers<
                com.capitalone.commander.grpc.CommanderProtos.StreamRequest,
                com.capitalone.commander.grpc.CommanderProtos.Event>(
                  this, METHODID_EVENT_STREAM)))
          .build();
    }
  }

  /**
   */
  public static final class CommanderStub extends io.grpc.stub.AbstractStub<CommanderStub> {
    private CommanderStub(io.grpc.Channel channel) {
      super(channel);
    }

    private CommanderStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected CommanderStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new CommanderStub(channel, callOptions);
    }

    /**
     */
    public void createCommand(com.capitalone.commander.grpc.CommanderProtos.CommandParams request,
        io.grpc.stub.StreamObserver<com.capitalone.commander.grpc.CommanderProtos.Command> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_CREATE_COMMAND, getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void listCommands(com.capitalone.commander.grpc.CommanderProtos.PagingInfo request,
        io.grpc.stub.StreamObserver<com.capitalone.commander.grpc.CommanderProtos.PagedCommands> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_LIST_COMMANDS, getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void commandById(com.capitalone.commander.grpc.CommanderProtos.UUID request,
        io.grpc.stub.StreamObserver<com.capitalone.commander.grpc.CommanderProtos.Command> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_COMMAND_BY_ID, getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void commandStream(com.capitalone.commander.grpc.CommanderProtos.StreamRequest request,
        io.grpc.stub.StreamObserver<com.capitalone.commander.grpc.CommanderProtos.Command> responseObserver) {
      asyncServerStreamingCall(
          getChannel().newCall(METHOD_COMMAND_STREAM, getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void listEvents(com.capitalone.commander.grpc.CommanderProtos.PagingInfo request,
        io.grpc.stub.StreamObserver<com.capitalone.commander.grpc.CommanderProtos.PagedEvents> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_LIST_EVENTS, getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void eventById(com.capitalone.commander.grpc.CommanderProtos.UUID request,
        io.grpc.stub.StreamObserver<com.capitalone.commander.grpc.CommanderProtos.Event> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_EVENT_BY_ID, getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void eventStream(com.capitalone.commander.grpc.CommanderProtos.StreamRequest request,
        io.grpc.stub.StreamObserver<com.capitalone.commander.grpc.CommanderProtos.Event> responseObserver) {
      asyncServerStreamingCall(
          getChannel().newCall(METHOD_EVENT_STREAM, getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class CommanderBlockingStub extends io.grpc.stub.AbstractStub<CommanderBlockingStub> {
    private CommanderBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private CommanderBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected CommanderBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new CommanderBlockingStub(channel, callOptions);
    }

    /**
     */
    public com.capitalone.commander.grpc.CommanderProtos.Command createCommand(com.capitalone.commander.grpc.CommanderProtos.CommandParams request) {
      return blockingUnaryCall(
          getChannel(), METHOD_CREATE_COMMAND, getCallOptions(), request);
    }

    /**
     */
    public com.capitalone.commander.grpc.CommanderProtos.PagedCommands listCommands(com.capitalone.commander.grpc.CommanderProtos.PagingInfo request) {
      return blockingUnaryCall(
          getChannel(), METHOD_LIST_COMMANDS, getCallOptions(), request);
    }

    /**
     */
    public com.capitalone.commander.grpc.CommanderProtos.Command commandById(com.capitalone.commander.grpc.CommanderProtos.UUID request) {
      return blockingUnaryCall(
          getChannel(), METHOD_COMMAND_BY_ID, getCallOptions(), request);
    }

    /**
     */
    public java.util.Iterator<com.capitalone.commander.grpc.CommanderProtos.Command> commandStream(
        com.capitalone.commander.grpc.CommanderProtos.StreamRequest request) {
      return blockingServerStreamingCall(
          getChannel(), METHOD_COMMAND_STREAM, getCallOptions(), request);
    }

    /**
     */
    public com.capitalone.commander.grpc.CommanderProtos.PagedEvents listEvents(com.capitalone.commander.grpc.CommanderProtos.PagingInfo request) {
      return blockingUnaryCall(
          getChannel(), METHOD_LIST_EVENTS, getCallOptions(), request);
    }

    /**
     */
    public com.capitalone.commander.grpc.CommanderProtos.Event eventById(com.capitalone.commander.grpc.CommanderProtos.UUID request) {
      return blockingUnaryCall(
          getChannel(), METHOD_EVENT_BY_ID, getCallOptions(), request);
    }

    /**
     */
    public java.util.Iterator<com.capitalone.commander.grpc.CommanderProtos.Event> eventStream(
        com.capitalone.commander.grpc.CommanderProtos.StreamRequest request) {
      return blockingServerStreamingCall(
          getChannel(), METHOD_EVENT_STREAM, getCallOptions(), request);
    }
  }

  /**
   */
  public static final class CommanderFutureStub extends io.grpc.stub.AbstractStub<CommanderFutureStub> {
    private CommanderFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private CommanderFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected CommanderFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new CommanderFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.capitalone.commander.grpc.CommanderProtos.Command> createCommand(
        com.capitalone.commander.grpc.CommanderProtos.CommandParams request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_CREATE_COMMAND, getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.capitalone.commander.grpc.CommanderProtos.PagedCommands> listCommands(
        com.capitalone.commander.grpc.CommanderProtos.PagingInfo request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_LIST_COMMANDS, getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.capitalone.commander.grpc.CommanderProtos.Command> commandById(
        com.capitalone.commander.grpc.CommanderProtos.UUID request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_COMMAND_BY_ID, getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.capitalone.commander.grpc.CommanderProtos.PagedEvents> listEvents(
        com.capitalone.commander.grpc.CommanderProtos.PagingInfo request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_LIST_EVENTS, getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.capitalone.commander.grpc.CommanderProtos.Event> eventById(
        com.capitalone.commander.grpc.CommanderProtos.UUID request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_EVENT_BY_ID, getCallOptions()), request);
    }
  }

  private static final int METHODID_CREATE_COMMAND = 0;
  private static final int METHODID_LIST_COMMANDS = 1;
  private static final int METHODID_COMMAND_BY_ID = 2;
  private static final int METHODID_COMMAND_STREAM = 3;
  private static final int METHODID_LIST_EVENTS = 4;
  private static final int METHODID_EVENT_BY_ID = 5;
  private static final int METHODID_EVENT_STREAM = 6;

  private static class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final CommanderImplBase serviceImpl;
    private final int methodId;

    public MethodHandlers(CommanderImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_CREATE_COMMAND:
          serviceImpl.createCommand((com.capitalone.commander.grpc.CommanderProtos.CommandParams) request,
              (io.grpc.stub.StreamObserver<com.capitalone.commander.grpc.CommanderProtos.Command>) responseObserver);
          break;
        case METHODID_LIST_COMMANDS:
          serviceImpl.listCommands((com.capitalone.commander.grpc.CommanderProtos.PagingInfo) request,
              (io.grpc.stub.StreamObserver<com.capitalone.commander.grpc.CommanderProtos.PagedCommands>) responseObserver);
          break;
        case METHODID_COMMAND_BY_ID:
          serviceImpl.commandById((com.capitalone.commander.grpc.CommanderProtos.UUID) request,
              (io.grpc.stub.StreamObserver<com.capitalone.commander.grpc.CommanderProtos.Command>) responseObserver);
          break;
        case METHODID_COMMAND_STREAM:
          serviceImpl.commandStream((com.capitalone.commander.grpc.CommanderProtos.StreamRequest) request,
              (io.grpc.stub.StreamObserver<com.capitalone.commander.grpc.CommanderProtos.Command>) responseObserver);
          break;
        case METHODID_LIST_EVENTS:
          serviceImpl.listEvents((com.capitalone.commander.grpc.CommanderProtos.PagingInfo) request,
              (io.grpc.stub.StreamObserver<com.capitalone.commander.grpc.CommanderProtos.PagedEvents>) responseObserver);
          break;
        case METHODID_EVENT_BY_ID:
          serviceImpl.eventById((com.capitalone.commander.grpc.CommanderProtos.UUID) request,
              (io.grpc.stub.StreamObserver<com.capitalone.commander.grpc.CommanderProtos.Event>) responseObserver);
          break;
        case METHODID_EVENT_STREAM:
          serviceImpl.eventStream((com.capitalone.commander.grpc.CommanderProtos.StreamRequest) request,
              (io.grpc.stub.StreamObserver<com.capitalone.commander.grpc.CommanderProtos.Event>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    return new io.grpc.ServiceDescriptor(SERVICE_NAME,
        METHOD_CREATE_COMMAND,
        METHOD_LIST_COMMANDS,
        METHOD_COMMAND_BY_ID,
        METHOD_COMMAND_STREAM,
        METHOD_LIST_EVENTS,
        METHOD_EVENT_BY_ID,
        METHOD_EVENT_STREAM);
  }

}
