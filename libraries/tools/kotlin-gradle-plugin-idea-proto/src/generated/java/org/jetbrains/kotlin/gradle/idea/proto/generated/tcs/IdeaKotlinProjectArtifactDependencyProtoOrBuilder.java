// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: proto_tcs.proto

package org.jetbrains.kotlin.gradle.idea.proto.generated.tcs;

public interface IdeaKotlinProjectArtifactDependencyProtoOrBuilder extends
    // @@protoc_insertion_point(interface_extends:org.jetbrains.kotlin.gradle.idea.proto.generated.tcs.IdeaKotlinProjectArtifactDependencyProto)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>optional .org.jetbrains.kotlin.gradle.idea.proto.generated.IdeaExtrasProto extras = 1;</code>
   * @return Whether the extras field is set.
   */
  boolean hasExtras();
  /**
   * <code>optional .org.jetbrains.kotlin.gradle.idea.proto.generated.IdeaExtrasProto extras = 1;</code>
   * @return The extras.
   */
  org.jetbrains.kotlin.gradle.idea.proto.generated.IdeaExtrasProto getExtras();
  /**
   * <code>optional .org.jetbrains.kotlin.gradle.idea.proto.generated.IdeaExtrasProto extras = 1;</code>
   */
  org.jetbrains.kotlin.gradle.idea.proto.generated.IdeaExtrasProtoOrBuilder getExtrasOrBuilder();

  /**
   * <code>optional .org.jetbrains.kotlin.gradle.idea.proto.generated.tcs.IdeaKotlinSourceDependencyProto.Type type = 2;</code>
   * @return Whether the type field is set.
   */
  boolean hasType();
  /**
   * <code>optional .org.jetbrains.kotlin.gradle.idea.proto.generated.tcs.IdeaKotlinSourceDependencyProto.Type type = 2;</code>
   * @return The enum numeric value on the wire for type.
   */
  int getTypeValue();
  /**
   * <code>optional .org.jetbrains.kotlin.gradle.idea.proto.generated.tcs.IdeaKotlinSourceDependencyProto.Type type = 2;</code>
   * @return The type.
   */
  org.jetbrains.kotlin.gradle.idea.proto.generated.tcs.IdeaKotlinSourceDependencyProto.Type getType();

  /**
   * <code>optional .org.jetbrains.kotlin.gradle.idea.proto.generated.tcs.IdeaKotlinProjectArtifactCoordinatesProto coordinates = 3;</code>
   * @return Whether the coordinates field is set.
   */
  boolean hasCoordinates();
  /**
   * <code>optional .org.jetbrains.kotlin.gradle.idea.proto.generated.tcs.IdeaKotlinProjectArtifactCoordinatesProto coordinates = 3;</code>
   * @return The coordinates.
   */
  org.jetbrains.kotlin.gradle.idea.proto.generated.tcs.IdeaKotlinProjectArtifactCoordinatesProto getCoordinates();
  /**
   * <code>optional .org.jetbrains.kotlin.gradle.idea.proto.generated.tcs.IdeaKotlinProjectArtifactCoordinatesProto coordinates = 3;</code>
   */
  org.jetbrains.kotlin.gradle.idea.proto.generated.tcs.IdeaKotlinProjectArtifactCoordinatesProtoOrBuilder getCoordinatesOrBuilder();
}
