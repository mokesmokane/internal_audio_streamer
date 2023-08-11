import 'dart:typed_data';

import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'internal_audio_streamer_method_channel.dart';

abstract class InternalAudioStreamerPlatform extends PlatformInterface {
  /// Constructs a InternalAudioStreamerPlatform.
  InternalAudioStreamerPlatform() : super(token: _token);

  static final Object _token = Object();

  static InternalAudioStreamerPlatform _instance = MethodChannelInternalAudioStreamer();

  /// The default instance of [InternalAudioStreamerPlatform] to use.
  ///
  /// Defaults to [MethodChannelInternalAudioStreamer].
  static InternalAudioStreamerPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [InternalAudioStreamerPlatform] when
  /// they register themselves.
  static set instance(InternalAudioStreamerPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }

  /// Returns the current battery level in percent.
  Future<void> stopRecording() {
    throw UnimplementedError('stopRecording() has not been implemented.');
  }

  /// Returns true if the device is on battery save mode
  Future<void> startRecording() {
    throw UnimplementedError('startRecording() has not been implemented.');
  }

  // /// Returns the current battery state in percent.
  // Future<BatteryState> get batteryState {
  //   throw UnimplementedError('batteryState() has not been implemented.');
  // }

  /// Returns a Stream of BatteryState changes.
  Stream<Uint8List> get onAudio {
    throw UnimplementedError(
        'get onAudio has not been implemented.');
  }
}
